package Impl


import Objects.SemesterPhaseService.Phase
import Objects.UserAccountService.SafeUserInfo
import Utils.CourseSelectionProcess.{checkCurrentPhase, validateTeacherToken, fetchCourseInfoByID}
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import org.slf4j.LoggerFactory
import cats.effect.IO
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Utils.CourseSelectionProcess.checkCurrentPhase
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
import org.joda.time.DateTime
import cats.implicits.*
import Common.DBAPI._
import Common.API.{PlanContext, Planner}
import cats.effect.IO
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}
import Common.ServiceUtils.schemaName
import Objects.CourseManagementService.CourseInfo
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class QueryCoursePreselectionDataMessagePlanner(
  teacherToken: String,
  courseID: Int,
  override val planContext: PlanContext
) extends Planner[List[SafeUserInfo]] {
  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[List[SafeUserInfo]] = {
    for {
      // Step 1: Validate teacherToken and get teacherID
      teacherID <- validateTeacherID()
      _ <- IO(logger.info(s"Step 1: 教师Token验证成功，TeacherID为: ${teacherID}"))

      // Step 2: Verify the corresponding course exists
      courseExists <- verifyCourseExists()
      _ <- IO(logger.info(s"Step 2: 验证课程存在，结果: ${courseExists}"))

      // Step 3: Validate current phase is Phase1
      currentPhaseIsValid <- validateCurrentPhase()
      _ <- IO(logger.info(s"Step 3: 验证当前学期阶段为Phase1，结果: ${currentPhaseIsValid}"))

      // Step 4: Fetch course preselection data and return it
      preselectionData <- fetchCoursePreselectionData()
      _ <- IO(logger.info(s"Step 4: 成功获取课程预选数据，共${preselectionData.length}位学生"))
    } yield preselectionData
  }

  private def validateTeacherID()(using PlanContext): IO[Int] = {
    validateTeacherToken(teacherToken).flatMap {
      case Some(teacherID) => IO.pure(teacherID)
      case None =>
        val errorMsg = s"教师Token验证失败，Token为: ${teacherToken}"
        logger.error(errorMsg)
        IO.raiseError(new Exception(errorMsg))
    }
  }

  private def verifyCourseExists()(using PlanContext): IO[Boolean] = {
    fetchCourseInfoByID(courseID).flatMap {
      case Some(courseInfo) =>
        IO(logger.info(s"课程信息成功获取，CourseID: ${courseID}, TeacherID: ${courseInfo.teacherID}")) >>
          IO.pure(true)
      case None =>
        val errorMsg = s"课程ID不存在，无法查询课程信息: CourseID=${courseID}"
        logger.error(errorMsg)
        IO.raiseError(new Exception(errorMsg))
    }
  }

  private def validateCurrentPhase()(using PlanContext): IO[Boolean] = {
    checkCurrentPhase().flatMap {
      case Phase.Phase1 =>
        IO(logger.info("当前学期阶段为Phase1")) >>
          IO.pure(true)
      case _ =>
        val errorMsg = "当前阶段无预选数据！"
        logger.error(errorMsg)
        IO.raiseError(new Exception(errorMsg))
    }
  }

  private def fetchCoursePreselectionData()(using PlanContext): IO[List[SafeUserInfo]] = {
    val sqlQuery =
      s"""
         SELECT student_id FROM ${schemaName}.course_preselection_table
         WHERE course_id = ?
       """
    val parameters = List(SqlParameter("Int", courseID.toString))

    readDBRows(sqlQuery, parameters).flatMap { rows =>
      val studentIDs = rows.map(json => decodeField[Int](json, "student_id"))
      logger.info(s"查询到课程ID ${courseID} 的预选名单中有 ${studentIDs.length} 位学生")

      // Fetch SafeUserInfo for each studentID
      studentIDs.map { studentID =>
        val sqlQueryForDetails =
          s"""
             SELECT user_id, user_name, account_name, role
             FROM ${schemaName}.user_table
             WHERE user_id = ?
           """
        val params = List(SqlParameter("Int", studentID.toString))

        readDBJson(sqlQueryForDetails, params).map { json =>
          val userID = decodeField[Int](json, "user_id")
          val userName = decodeField[String](json, "user_name")
          val accountName = decodeField[String](json, "account_name")
          val roleString = decodeField[String](json, "role")
          val role = UserRole.fromString(roleString)

          SafeUserInfo(
            userID = userID,
            userName = userName,
            accountName = accountName,
            role = role
          )
        }
      }.sequence.map(_.toList)
    }
  }
}