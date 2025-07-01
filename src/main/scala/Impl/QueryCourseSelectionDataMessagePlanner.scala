package Impl


import Common.DBAPI._
import Common.API.{PlanContext, Planner}
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import Utils.CourseSelectionProcess._
import Objects.UserAccountService.SafeUserInfo
import cats.effect.IO
import cats.implicits._
import io.circe.Json
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
import Objects.CourseManagementService.CourseInfo
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
import Utils.CourseSelectionProcess.validateTeacherToken
import Objects.CourseManagementService.CourseTime
import Utils.CourseSelectionProcess.fetchCourseInfoByID
import Objects.SystemLogService.SystemLogEntry
import Utils.CourseSelectionProcess.recordCourseSelectionOperationLog
import Objects.CourseManagementService.DayOfWeek
import Objects.CourseManagementService.TimePeriod
import Objects.UserAccountService.UserRole
import Objects.CourseManagementService.CourseInfo
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class QueryCourseSelectionDataMessagePlanner(
                                                   teacherToken: String,
                                                   courseID: Int,
                                                   override val planContext: PlanContext
                                                 ) extends Planner[List[SafeUserInfo]] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[List[SafeUserInfo]] = {
    for {
      _ <- IO(logger.info(s"开始执行QueryCourseSelectionDataMessagePlanner，教师Token: ${teacherToken}, 课程ID: ${courseID}"))

      // Step 1: Validate teacher token
      teacherID <- validateTeacherTokenWithLogging(teacherToken)

      // Step 2: Check if the course exists
      courseInfo <- fetchCourseInfoWithLogging(courseID)

      // Step 3: Check if the current phase is Phase2
      _ <- validatePhase2()

      // Step 4: Query course selection list
      selectedStudents <- queryCourseSelectionWithLogging(courseID)

      _ <- IO(logger.info(s"QueryCourseSelectionDataMessagePlanner 执行成功，返回选上学生：${selectedStudents.size} 条记录"))
    } yield selectedStudents
  }

  private def validateTeacherTokenWithLogging(token: String)(using PlanContext): IO[Int] = {
    for {
      _ <- IO(logger.info(s"验证教师Token: ${token}"))
      teacherIDOpt <- validateTeacherToken(token)
      teacherID <- teacherIDOpt match {
        case Some(id) =>
          IO {
            logger.info(s"教师验证成功，TeacherID: ${id}")
            id
          }
        case None =>
          IO.raiseError(new IllegalArgumentException("教师Token无效！"))
      }
    } yield teacherID
  }

  private def fetchCourseInfoWithLogging(courseID: Int)(using PlanContext): IO[CourseInfo] = {
    for {
      _ <- IO(logger.info(s"验证课程ID: ${courseID}"))
      courseInfoOpt <- fetchCourseInfoByID(courseID)
      courseInfo <- courseInfoOpt match {
        case Some(info) =>
          IO {
            logger.info(s"课程验证成功，课程信息: ${info}")
            info
          }
        case None =>
          IO.raiseError(new IllegalArgumentException("课程不存在！"))
      }
    } yield courseInfo
  }

  private def validatePhase2()(using PlanContext): IO[Unit] = {
    val sql = s"SELECT current_phase FROM ${schemaName}.system_phase_table LIMIT 1"
    for {
      _ <- IO(logger.info("检查当前学期阶段"))
      currentPhase <- readDBString(sql, List.empty)
      _ <- IO {
        if (currentPhase != "Phase2") {
          logger.error("当前阶段不是Phase2，无权查看选上名单")
          throw new IllegalStateException("当前阶段尚未抽签。")
        } else {
          logger.info("当前阶段为Phase2，验证通过")
        }
      }
    } yield ()
  }

  private def queryCourseSelectionWithLogging(courseID: Int)(using PlanContext): IO[List[SafeUserInfo]] = {
    val sql = s"SELECT student_id FROM ${schemaName}.course_selection_table WHERE course_id = ?"
    for {
      _ <- IO(logger.info(s"查询课程ID: ${courseID} 的选上名单"))
      resultJson <- readDBRows(sql, List(SqlParameter("Int", courseID.toString)))
      studentIds = resultJson.map(json => decodeField[Int](json, "student_id"))
      _ <- IO(logger.info(s"从数据库成功查询到学生ID列表：${studentIds.mkString(", ")}"))

      // Fetch safe user info for each student ID
      safeUserInfos <- studentIds.map(fetchStudentSafeInfo).sequence
      _ <- IO(logger.info(s"成功查询到学生隐私安全信息，数量: ${safeUserInfos.size} 条"))
    } yield safeUserInfos
  }

  private def fetchStudentSafeInfo(studentID: Int)(using PlanContext): IO[SafeUserInfo] = {
    val sql = s"""
      SELECT user_id, user_name, account_name, role
      FROM ${schemaName}.user_table
      WHERE user_id = ?
    """.stripMargin
    for {
      _ <- IO(logger.info(s"查询学生ID: ${studentID} 的隐私安全信息"))
      studentJson <- readDBJson(sql, List(SqlParameter("Int", studentID.toString)))
      userInfo = decodeType[SafeUserInfo](studentJson)
      _ <- IO(logger.info(s"成功查询到学生ID: ${studentID} 的安全信息: ${userInfo}"))
    } yield userInfo
  }
}