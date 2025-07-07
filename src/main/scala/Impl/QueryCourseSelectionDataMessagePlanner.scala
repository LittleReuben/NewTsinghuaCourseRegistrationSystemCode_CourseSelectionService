package Impl

import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
import cats.implicits.*
import Common.DBAPI._
import Common.API.{PlanContext, Planner}
import cats.effect.IO
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import Objects.UserAccountService.SafeUserInfo
import Utils.CourseSelectionProcess.checkCurrentPhase
import Utils.CourseSelectionProcess.validateTeacherToken
import Utils.CourseSelectionProcess.fetchCourseInfoByID
import Objects.CourseManagementService.CourseInfo
import Objects.CourseManagementService.CourseTime
import Objects.SystemLogService.SystemLogEntry
import Utils.CourseSelectionProcess.recordCourseSelectionOperationLog
import Objects.CourseManagementService.DayOfWeek
import Objects.CourseManagementService.TimePeriod
import Objects.UserAccountService.UserRole
import APIs.UserAccountService.QuerySafeUserInfoByUserIDMessage
import org.slf4j.LoggerFactory
import cats.effect.unsafe.implicits.global
import Objects.SemesterPhaseService.Phase

case class QueryCourseSelectionDataMessagePlanner(
    teacherToken: String,
    courseID: Int,
    override val planContext: PlanContext
) extends Planner[List[SafeUserInfo]] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[List[SafeUserInfo]] = {
    for {
      // Step 1: Validate teacher token
      _ <- IO(logger.info(s"验证教师Token: ${teacherToken}"))
      teacherIDOpt <- validateTeacherToken(teacherToken)
      teacherID <- teacherIDOpt match {
        case Some(id) =>
          IO(logger.info(s"教师Token验证成功，教师ID为${id}")) *> IO(id)
        case None =>
          IO.raiseError(new Exception("教师Token验证失败"))
      }

      // Step 2: Check if the course exists
      _ <- IO(logger.info(s"检查课程ID: ${courseID}是否存在"))
      courseInfoOpt <- fetchCourseInfoByID(courseID)
      courseInfo <- courseInfoOpt match {
        case Some(info) =>
          IO(logger.info(s"课程ID: ${courseID}存在，课程信息: ${info}")) *> IO(info)
        case None =>
          IO(logger.error(s"课程ID: ${courseID}不存在")) *> IO.raiseError(new Exception("课程不存在！"))
      }

      // Step 3: Check if the current phase is Phase2
      currentPhase <- checkCurrentPhase()
      _ <- if (currentPhase != Phase.Phase2)
        IO.raiseError(new IllegalArgumentException("当前阶段尚未抽签！"))
      else IO.unit

      // Step 4: Fetch course selection data
      selectionData <- getCourseSelectionData(courseID)
    } yield selectionData
  }

  private def getCourseSelectionData(courseID: Int)(using PlanContext): IO[List[SafeUserInfo]] = {
    val sqlQuery = s"""
      SELECT student_id
      FROM ${schemaName}.course_selection_table
      WHERE course_id = ?;
    """
    val parameters = List(SqlParameter("Int", courseID.toString))

    for {
      _ <- IO(logger.info(s"执行数据库查询获取学生选上名单，课程ID:${courseID}"))
      studentRows <- readDBRows(sqlQuery, parameters)
      selectionData <- studentRows.flatTraverse { row =>
        val studentID = decodeField[Int](row, "student_id")
        QuerySafeUserInfoByUserIDMessage(studentID).send.map {
          case Some(userInfo) =>
            logger.info(s"成功获取到学生信息: ${userInfo}")
            List(userInfo)
          case None =>
            logger.error(s"未找到学生ID: ${studentID}的相关信息")
            List.empty
        }
      }
      _ <- IO(logger.info(s"成功获取到${selectionData.size}位学生的选上名单"))
    } yield selectionData
  }
}