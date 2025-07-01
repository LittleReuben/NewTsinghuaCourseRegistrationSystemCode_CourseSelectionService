package Impl


import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import Utils.CourseSelectionProcess.validateStudentToken
import org.slf4j.LoggerFactory
import cats.effect.IO
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
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
import Objects.SystemLogService.SystemLogEntry
import Utils.CourseSelectionProcess.recordCourseSelectionOperationLog
import Objects.CourseManagementService.DayOfWeek
import Objects.CourseManagementService.TimePeriod
import Objects.CourseManagementService.CourseInfo
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}
import Objects.CourseManagementService.CourseInfo

case class CheckStudentHasSuccessfullyTakenCourseMessagePlanner(
    studentToken: String,
    courseID: Int,
    override val planContext: PlanContext
) extends Planner[Boolean] {

  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[Boolean] = {
    for {
      // Step 1: 验证学生Token，如果无效直接抛出异常
      studentID <- validateAndFetchStudentID(studentToken)

      // Step 2: 检查选课历史记录是否存在
      hasTakenCourse <- checkCourseHistory(studentID, courseID)

      _ <- IO(logger.info(s"查询结果：学生${studentID}是否选上课程${courseID}: ${hasTakenCourse}"))
    } yield hasTakenCourse
  }

  /** 验证学生Token并解码为studentID */
  private def validateAndFetchStudentID(studentToken: String)(using PlanContext): IO[Int] = {
    for {
      _ <- IO(logger.info(s"开始验证学生Token: ${studentToken}"))
      studentIDOption <- validateStudentToken(studentToken)
      studentID <- studentIDOption match {
        case Some(id) =>
          IO(logger.info(s"学生Token验证成功，解码出学生ID: ${id}")).as(id)
        case None =>
          val errorMsg = s"Token验证失败，Token无效或不存在"
          IO(logger.error(errorMsg)) >>
          IO.raiseError(new IllegalArgumentException(errorMsg))
      }
    } yield studentID
  }

  /** 根据 studentID 和 courseID 查询选课历史记录 */
  private def checkCourseHistory(studentID: Int, courseID: Int)(using PlanContext): IO[Boolean] = {
    val query =
      s"""
       SELECT EXISTS (
         SELECT 1
         FROM ${schemaName}.course_participation_history_table
         WHERE student_id = ? AND course_id = ?
       )
     """
    val parameters = List(
      SqlParameter("Int", studentID.toString),
      SqlParameter("Int", courseID.toString)
    )

    for {
      exists <- readDBBoolean(query, parameters)
      _ <- IO(logger.info(s"数据库查询结果：学生${studentID}是否选上课程${courseID}: ${exists}"))
    } yield exists
  }
}