package Impl


import Utils.CourseSelectionProcess.validateTeacherToken
import Utils.CourseSelectionProcess.checkCurrentPhase
import Utils.CourseSelectionProcess.fetchCourseInfoByID
import Objects.SemesterPhaseService.Phase
import Objects.UserAccountService.SafeUserInfo
import Objects.CourseManagementService.CourseInfo
import Objects.UserAccountService.UserRole
import Common.API.{Planner, PlanContext}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.joda.time.DateTime
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import cats.implicits.*
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
import Objects.SystemLogService.SystemLogEntry
import Utils.CourseSelectionProcess.recordCourseSelectionOperationLog
import Objects.CourseManagementService.DayOfWeek
import Objects.CourseManagementService.TimePeriod
import Objects.CourseManagementService.CourseInfo
import Common.API.{PlanContext, Planner}
import Utils.CourseSelectionProcess.{checkCurrentPhase, fetchCourseInfoByID, validateTeacherToken}
import org.slf4j.LoggerFactory
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class QueryCoursePreselectionDataMessagePlanner(
  teacherToken: String,
  courseID: Int,
  override val planContext: PlanContext
) extends Planner[List[SafeUserInfo]] {

  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[List[SafeUserInfo]] = {
    for {
      // Step 1: Validate teacher token and authenticate teacher
      teacherID <- validateAndAuthenticateTeacher(teacherToken)

      // Step 2: Validate course existence
      courseInfo <- validateCourseExistence(courseID)

      // Step 3: Check current semester phase
      _ <- validateCurrentPhaseIsPhase1()

      // Step 4: Query preselection data for the course
      preselectionData <- fetchCoursePreselectionData(courseID)
    } yield preselectionData
  }

  private def validateAndAuthenticateTeacher(teacherToken: String)(using PlanContext): IO[Int] = {
    for {
      _ <- IO(logger.info("开始鉴权教师Token"))
      teacherIDOpt <- validateTeacherToken(teacherToken)
      teacherID <- teacherIDOpt match {
        case Some(id) =>
          IO(logger.info(s"Token鉴权成功，教师ID: ${id}")) >> IO.pure(id)
        case None =>
          IO.raiseError(new IllegalArgumentException("教师Token无效或鉴权失败"))
      }
    } yield teacherID
  }

  private def validateCourseExistence(courseID: Int)(using PlanContext): IO[CourseInfo] = {
    for {
      _ <- IO(logger.info(s"开始验证课程ID:${courseID}是否存在"))
      courseInfoOpt <- fetchCourseInfoByID(courseID)
      courseInfo <- courseInfoOpt match {
        case Some(info) =>
          IO(logger.info(s"课程验证成功，课程信息: ${info}")) >> IO.pure(info)
        case None =>
          IO.raiseError(new IllegalArgumentException("课程不存在！"))
      }
    } yield courseInfo
  }

  private def validateCurrentPhaseIsPhase1()(using PlanContext): IO[Unit] = {
    for {
      _ <- IO(logger.info("开始检查当前学期阶段是否为Phase1"))
      currentPhase <- checkCurrentPhase()
      _ <- if (currentPhase == Phase.Phase1) {
        IO(logger.info("当前阶段为Phase1，允许查询预选数据"))
      } else {
        IO.raiseError(new IllegalStateException("当前阶段无预选数据"))
      }
    } yield ()
  }

  private def fetchCoursePreselectionData(courseID: Int)(using PlanContext): IO[List[SafeUserInfo]] = {
    val sqlQuery =
      s"""
      SELECT student_id, user_name, account_name
      FROM ${schemaName}.course_preselection_table AS cpt
      JOIN ${schemaName}.user_account AS ua
      ON cpt.student_id = ua.user_id
      WHERE cpt.course_id = ?;
      """
    val sqlParams = List(SqlParameter("Int", courseID.toString))

    for {
      _ <- IO(logger.info(s"执行查询预选数据的SQL: ${sqlQuery}"))
      rows <- readDBRows(sqlQuery, sqlParams)
      safeUserInfoList <- IO {
        rows.map { json =>
          val userID = decodeField[Int](json, "student_id")
          val userName = decodeField[String](json, "user_name")
          val accountName = decodeField[String](json, "account_name")
          val role = UserRole.Student // 默认学生角色
          SafeUserInfo(userID, userName, accountName, role)
        }
      }
      _ <- IO(logger.info(s"成功查询课程ID:${courseID}的预选数据，共${safeUserInfoList.size}条记录"))
    } yield safeUserInfoList
  }
}