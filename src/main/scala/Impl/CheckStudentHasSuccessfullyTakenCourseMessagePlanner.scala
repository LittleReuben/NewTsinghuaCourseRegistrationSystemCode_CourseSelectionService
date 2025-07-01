package Impl


import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import Utils.CourseSelectionProcess.validateStudentToken
import Utils.CourseSelectionProcess.recordCourseSelectionOperationLog
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
import cats.effect.IO
import org.slf4j.LoggerFactory
import Objects.CourseManagementService.{CourseTime, DayOfWeek, TimePeriod, CourseInfo}
import Objects.SystemLogService.SystemLogEntry
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
import cats.implicits.*
import Common.DBAPI._
import Common.API.{PlanContext, Planner}
import cats.effect.IO
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}
import Common.ServiceUtils.schemaName
import Objects.CourseManagementService.CourseTime
import Utils.CourseSelectionProcess.fetchCourseInfoByID
import Objects.CourseManagementService.DayOfWeek
import Objects.CourseManagementService.TimePeriod
import Objects.CourseManagementService.CourseInfo
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}
import Objects.CourseManagementService.CourseInfo

case class CheckStudentHasSuccessfullyTakenCourseMessagePlanner(
    studentToken: String,
    courseID: Int,
    override val planContext: PlanContext
) extends Planner[Boolean] {
  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[Boolean] = {
    for {
      // Step 1: Validate and decode student token
      _ <- IO(logger.info(s"验证学生Token: ${studentToken}并解码成学生ID"))
      studentIDOpt <- validateStudentToken(studentToken)
      studentID <- IO {
        studentIDOpt.getOrElse(
          throw new IllegalArgumentException("Token验证失败.")
        )
      }
      _ <- IO(logger.info(s"Token验证成功，学生ID为: ${studentID}"))

      // Step 2: Check course participation history
      hasTakenCourse <- checkCourseParticipationHistory(studentID, courseID)
      _ <- IO(logger.info(s"学生是否曾选上课程ID (${courseID}): ${hasTakenCourse}"))
    } yield hasTakenCourse
  }

  private def checkCourseParticipationHistory(studentID: Int, courseID: Int)(using PlanContext): IO[Boolean] = {
    logger.info(s"开始查询学生(${studentID})是否选上课程(${courseID})的历史记录")

    val sqlQuery =
      s"""
      SELECT EXISTS (
        SELECT 1
        FROM ${schemaName}.course_participation_history_table
        WHERE student_id = ? AND course_id = ?
      );
      """
    val parameters = List(
      SqlParameter("Int", studentID.toString),
      SqlParameter("Int", courseID.toString)
    )
    for {
      hasTakenCourse <- readDBBoolean(sqlQuery, parameters)
      _ <- recordCourseSelectionOperationLog(
        studentID = studentID,
        action = "CHECK_COURSE_PARTICIPATION",
        courseID = Some(courseID),
        details = s"查询学生选课历史: 存在记录=${hasTakenCourse}"
      ).flatMap(logRecorded =>
        IO(logger.info(s"选课记录查询日志记录${if (logRecorded) "成功" else "失败"}"))
      )
    } yield hasTakenCourse
  }
}