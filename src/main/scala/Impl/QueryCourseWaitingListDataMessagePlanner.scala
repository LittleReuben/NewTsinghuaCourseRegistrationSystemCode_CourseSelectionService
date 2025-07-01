package Impl


import Objects.SemesterPhaseService.Phase
import Objects.UserAccountService.SafeUserInfo
import Utils.CourseSelectionProcess.checkCurrentPhase
import Utils.CourseSelectionProcess.validateTeacherToken
import Objects.CourseManagementService.CourseTime
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
import Objects.CourseManagementService.CourseInfo
import Utils.CourseSelectionProcess.{checkCurrentPhase, fetchCourseInfoByID, validateTeacherToken}
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class QueryCourseWaitingListDataMessagePlanner(
    teacherToken: String,
    courseID: Int,
    override val planContext: PlanContext
) extends Planner[List[SafeUserInfo]] {

  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[List[SafeUserInfo]] = {
    for {
      // Step 1: Validate teacher token
      _ <- IO(logger.info("开始验证教师Token"))
      teacherIDOpt <- validateTeacherToken(teacherToken)
      teacherID <- IO {
        teacherIDOpt match {
          case Some(value) => value
          case None =>
            throw new IllegalArgumentException("教师验证失败！")
        }
      }
      _ <- IO(logger.info(s"教师验证成功，TeacherID: ${teacherID}"))

      // Step 2: Fetch course information
      _ <- IO(logger.info(s"获取课程信息，课程ID: ${courseID}"))
      courseInfoOpt <- fetchCourseInfoByID(courseID)
      courseInfo <- IO {
        courseInfoOpt match {
          case Some(value) => value
          case None =>
            throw new IllegalArgumentException("课程不存在！")
        }
      }
      _ <- IO(logger.info(s"课程信息获取成功: ${courseInfo}"))

      // Step 3: Verify current phase
      _ <- IO(logger.info("检查当前学期阶段"))
      currentPhase <- checkCurrentPhase()
      _ <- IO {
        if (currentPhase != Phase.Phase2) {
          throw new IllegalArgumentException("当前阶段尚未抽签。")
        }
      }
      _ <- IO(logger.info("当前学期阶段验证通过，已进入Phase2"))

      // Step 4: Query WaitingListTable to get all student IDs and positions
      _ <- IO(logger.info("开始查询等待队列中的学生信息"))
      waitingListQuery =
        s"""
           |SELECT student_id, position 
           |FROM ${schemaName}.waiting_list_table 
           |WHERE course_id = ?
         """.stripMargin
      waitingListParams = List(SqlParameter("Int", courseID.toString))
      waitingListRows <- readDBRows(waitingListQuery, waitingListParams)
      waitingList <- IO {
        waitingListRows.map { row =>
          val studentID = decodeField[Int](row, "student_id")
          val position = decodeField[Int](row, "position")
          (studentID, position)
        }
      }
      _ <- IO(logger.info(s"查询到等待队列信息: ${waitingList}"))

      // Step 5: Convert student IDs into SafeUserInfo
      _ <- IO(logger.info(s"转换学生ID为SafeUserInfo"))
      studentIDs = waitingList.map(_._1)
      safeUserInfoList <- QuerySafeUserInfoByUserIDListMessage(studentIDs).send
      _ <- IO(logger.info(s"转换完成，SafeUserInfo信息: ${safeUserInfoList}"))

      // Step 6: Construct final waiting list data with SafeUserInfo and order based on the position
      _ <- IO(logger.info("构造最终的等待队列信息"))
      waitingListData = waitingList
        .zip(safeUserInfoList)
        .map { case ((_, position), safeUserInfo) =>
          (safeUserInfo, position)
        }
        .sortBy(_._2)
        .map(_._1)

      _ <- IO(logger.info(s"等待队列数据构造完成: ${waitingListData}"))
    } yield waitingListData
  }
}