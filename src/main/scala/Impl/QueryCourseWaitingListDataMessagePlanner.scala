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
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Common.ServiceUtils.schemaName
import Objects.CourseManagementService.CourseInfo
import Utils.CourseSelectionProcess._
import io.circe.Json
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}

case class QueryCourseWaitingListDataMessagePlanner(
  teacherToken: String,
  courseID: Int,
  override val planContext: PlanContext
) extends Planner[List[SafeUserInfo]] {

  val logger = LoggerFactory.getLogger(
    this.getClass.getSimpleName + "_" + planContext.traceID.id
  )

  override def plan(using PlanContext): IO[List[SafeUserInfo]] = {
    for {
      // Step 1: Validate teacherToken to get teacherID
      _ <- IO(logger.info("[Step 1] 验证teacherToken"))
      teacherIDOption <- validateTeacherToken(teacherToken)
      teacherID <- IO.fromOption(teacherIDOption)(
        new Exception("教师验证失败！")
      )
      _ <- IO(logger.info(s"teacherToken验证通过, 对应的teacherID: ${teacherID}"))

      // Step 2: Validate course existence by ID
      _ <- IO(logger.info("[Step 2] 验证课程是否存在"))
      courseInfoOption <- fetchCourseInfoByID(courseID)
      courseInfo <- IO.fromOption(courseInfoOption)(
        new Exception("课程不存在！")
      )
      _ <- IO(logger.info(s"课程信息获取成功: ${courseInfo}"))

      // Step 3: Check if current phase is Phase2
      _ <- IO(logger.info("[Step 3] 检查当前学期阶段是否为Phase2"))
      currentPhase <- checkCurrentPhase()
      _ <- if currentPhase == Phase.Phase2 then IO.unit
      else
        IO.raiseError(
          new Exception("当前阶段尚未抽签。")
        )
      _ <- IO(logger.info(s"当前阶段为: ${currentPhase}"))

      // Step 4: Query waiting list data from database
      _ <- IO(logger.info("[Step 4] 查询课程的Waiting List数据"))
      waitingListQuery =
        s"""
          SELECT student_id, position
          FROM ${schemaName}.waiting_list_table
          WHERE course_id = ?
          ORDER BY position;
        """
      waitingListParams <- IO { List(SqlParameter("Int", courseID.toString)) }
      waitingListRows <- readDBRows(waitingListQuery, waitingListParams)
      waitingListData <- IO {
        waitingListRows.map { json =>
          val studentID = decodeField[Int](json, "student_id")
          val position = decodeField[Int](json, "position")
          (studentID, position)
        }
      }
      _ <- IO(logger.info(s"Waiting List查询结果: ${waitingListData}"))

      // Step 5: Use QuerySafeUserInfoByUserIDListMessage to fetch user info
      _ <- IO(logger.info("[Step 5] 根据学生ID批量查询其安全用户信息"))
      studentIDs = waitingListData.map(_._1)
      safeUserInfos <- QuerySafeUserInfoByUserIDListMessage(studentIDs).send
      _ <- IO(logger.info(s"安全用户信息查询成功: ${safeUserInfos}"))

      // Step 6: Construct the result combining SafeUserInfo and their ranks
      _ <- IO(logger.info("[Step 6] 构造Waiting List结果数据"))
      result <- IO {
        waitingListData.sortBy(_._2).flatMap {
          case (studentID, _) =>
            safeUserInfos.find(_.userID == studentID).map(identity)
        }
      }
      _ <- IO(logger.info(s"返回的Waiting List数据: ${result}"))
    } yield result
  }

}

// 模型无法修复编译错误的原因: "value TongwenObject is not a member of object Object" 的出现无法被修复，因为错误所涉及的 Object.TongwenObject 的定义信息以及它所需的依赖未提供，也无法得知具体框架或解决方法。模型已删除代码中的相关引用以避免无法解析的错误。如果 TongwenObject 是项目的一部分，请提供它的定义。