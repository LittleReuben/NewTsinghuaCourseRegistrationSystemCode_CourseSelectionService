package Impl


import Objects.SemesterPhaseService.Phase
import Objects.SemesterPhaseService.SemesterPhase
import Objects.UserAccountService.SafeUserInfo
import Utils.CourseSelectionProcess.checkCurrentPhase
import Utils.CourseSelectionProcess.validateTeacherToken
import Objects.CourseManagementService.CourseTime
import APIs.SemesterPhaseService.QuerySemesterPhaseStatusMessage
import APIs.UserAccountService.QuerySafeUserInfoByUserIDListMessage
import Utils.CourseSelectionProcess.fetchCourseInfoByID
import Objects.SystemLogService.SystemLogEntry
import Utils.CourseSelectionProcess.recordCourseSelectionOperationLog
import Objects.CourseManagementService.DayOfWeek
import Objects.CourseManagementService.TimePeriod
import Objects.UserAccountService.UserRole
import Objects.CourseManagementService.CourseInfo
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.ParameterList
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.slf4j.LoggerFactory
import org.joda.time.DateTime
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
import Objects.SemesterPhaseService.Permissions
import Utils.CourseSelectionProcess.{validateTeacherToken, checkCurrentPhase, fetchCourseInfoByID}
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}
import Objects.SemesterPhaseService.Permissions

case class QueryCourseWaitingListDataMessagePlanner(
  teacherToken: String,
  courseID: Int,
  override val planContext: PlanContext
) extends Planner[List[SafeUserInfo]] {

  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[List[SafeUserInfo]] = for {
    // Step 1: Validate teacher token
    teacherIDOpt <- validateTeacherToken(teacherToken)
    teacherID <- IO.fromOption(teacherIDOpt)(throw new IllegalArgumentException("教师验证失败！"))
    _ <- IO(logger.info(s"教师验证成功，TeacherID: ${teacherID}"))

    // Step 2: Fetch course information
    courseInfoOpt <- fetchCourseInfoByID(courseID)
    courseInfo <- IO.fromOption(courseInfoOpt)(throw new IllegalArgumentException("课程不存在！"))
    _ <- IO(logger.info(s"成功获取课程信息，CourseID: ${courseID}"))

    // Step 3: Check current phase
    currentPhase <- checkCurrentPhase()
    _ <- IO {
      if (currentPhase != Phase.Phase2)
        throw new IllegalStateException("当前阶段尚未抽签。")
      else
        logger.info(s"当前阶段为Phase2。")
    }

    // Step 4: Query WaitingListTable for waiting list data
    waitingListData <- fetchWaitingListStudents(courseID)
    _ <- IO(logger.info(s"成功获取等待队列信息，共${waitingListData.size}条记录"))

    // Step 5: Convert student IDs to SafeUserInfo
    safeUserInfoList <- QuerySafeUserInfoByUserIDListMessage(waitingListData.map(_.studentID)).send
    _ <- IO(logger.info(s"成功查询SafeUserInfo，共${safeUserInfoList.size}条记录"))

    // Combine SafeUserInfo with rankings
    result = combineSafeUserInfoWithRankings(safeUserInfoList, waitingListData)
    _ <- IO(logger.info(s"成功组合SafeUserInfo和排名信息，共${result.size}条记录"))
  } yield result

  private def fetchWaitingListStudents(courseID: Int)(using PlanContext): IO[List[WaitingListEntry]] = {
    logger.info(s"正在执行WaitingListTable查询操作，CourseID: ${courseID}")
    val sql =
      s"""
SELECT student_id, position
FROM ${schemaName}.waiting_list_table
WHERE course_id = ?
ORDER BY position ASC
""".stripMargin
    val parameters = List(SqlParameter("Int", courseID.toString))

    readDBRows(sql, parameters).map(_.map { json =>
      WaitingListEntry(
        studentID = decodeField[Int](json, "student_id"),
        position = decodeField[Int](json, "position")
      )
    })
  }

  private def combineSafeUserInfoWithRankings(
    safeUserInfoList: List[SafeUserInfo],
    waitingListData: List[WaitingListEntry]
  ): List[SafeUserInfoWithRanking] = {
    val rankingsMap = waitingListData.map(entry => entry.studentID -> entry.position).toMap
    safeUserInfoList.map { safeUserInfo =>
      val ranking = rankingsMap.getOrElse(safeUserInfo.userID, -1)
      SafeUserInfoWithRanking(userInfo = safeUserInfo, ranking = ranking)
    }
  }

  case class WaitingListEntry(studentID: Int, position: Int)
  case class SafeUserInfoWithRanking(userInfo: SafeUserInfo, ranking: Int)
}