package Impl


import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import Objects.SemesterPhaseService.Phase
import Objects.SemesterPhaseService.SemesterPhase
import Objects.CourseManagementService.CourseInfo
import Utils.CourseSelectionProcess._
import APIs.SemesterPhaseService.QuerySemesterPhaseStatusMessage
import cats.effect.IO
import org.slf4j.LoggerFactory
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Objects.CourseManagementService.CourseTime
import Objects.SystemLogService.SystemLogEntry
import Objects.CourseManagementService.DayOfWeek
import Objects.CourseManagementService.TimePeriod
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
import Utils.CourseSelectionProcess.validateStudentToken
import Utils.CourseSelectionProcess.fetchCourseInfoByID
import Utils.CourseSelectionProcess.recordCourseSelectionOperationLog
import Objects.SemesterPhaseService.Permissions
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}
import Objects.SemesterPhaseService.Permissions

case class QueryStudentSelectedCoursesMessagePlanner(
  studentToken: String,
  override val planContext: PlanContext
) extends Planner[List[CourseInfo]] {
  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[List[CourseInfo]] = {
    for {
      // Step 1: 验证学生Token并获取studentID
      _ <- IO(logger.info(s"开始验证学生Token: ${studentToken}"))
      studentIDOpt <- validateStudentToken(studentToken)
      studentID <- IO {
        studentIDOpt.getOrElse(throw new IllegalArgumentException("Invalid studentToken"))
      }
      _ <- IO(logger.info(s"学生ID验证成功，studentID: ${studentID}"))

      // Step 2: 调用QuerySemesterPhaseStatusMessage接口，检查学期阶段是否为Phase2
      _ <- IO(logger.info("检查当前学期阶段"))
      semesterPhase <- QuerySemesterPhaseStatusMessage(studentToken).send
      _ <- IO {
        if (semesterPhase.currentPhase != Phase.Phase2)
          throw new IllegalStateException("当前阶段尚未抽签完成")
      }
      _ <- IO(logger.info("学期阶段验证通过，当前阶段为Phase2"))

      // Step 3: 查询学生选中的课程信息
      _ <- IO(logger.info("开始查询学生选中的课程信息"))
      selectedCoursesQuery =
        s"""
         SELECT course_id
         FROM ${schemaName}.course_selection_table
         WHERE student_id = ?
        """
      selectedCourses <- readDBRows(selectedCoursesQuery, List(SqlParameter("Int", studentID.toString)))
        .map(_.map(json => decodeField[Int](json, "course_id")))
      _ <- IO(logger.info(s"成功查询到选中课程ID列表: ${selectedCourses}"))

      // Step 4: 构造CourseInfo对象列表
      courseInfos <- selectedCourses.traverse { courseID =>
        fetchCourseInfoByID(courseID).map {
          case Some(courseInfo) =>
            courseInfo
          case None =>
            throw new IllegalStateException(s"无法找到课程ID为${courseID}的详细信息")
        }
      }
      _ <- IO(logger.info(s"成功构造选中课程的CourseInfo列表: ${courseInfos}"))

    } yield courseInfos
  }
}