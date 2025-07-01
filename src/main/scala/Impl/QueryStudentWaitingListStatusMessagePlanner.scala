package Impl


import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import Utils.CourseSelectionProcess.{validateStudentToken, fetchCourseInfoByID, checkCurrentPhase, recordCourseSelectionOperationLog}
import Objects.CourseSelectionService.PairOfCourseAndRank
import Objects.CourseManagementService.CourseInfo
import Objects.SemesterPhaseService.Phase
import org.slf4j.LoggerFactory
import cats.effect.IO
import io.circe._
import io.circe.syntax._
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
import Utils.CourseSelectionProcess.checkCurrentPhase
import Objects.CourseManagementService.CourseTime
import Utils.CourseSelectionProcess.validateStudentToken
import Utils.CourseSelectionProcess.fetchCourseInfoByID
import Objects.SystemLogService.SystemLogEntry
import Utils.CourseSelectionProcess.recordCourseSelectionOperationLog
import Objects.CourseManagementService.DayOfWeek
import Objects.CourseManagementService.TimePeriod
import Objects.CourseManagementService.CourseInfo
import io.circe.Json
import io.circe.generic.auto._
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class QueryStudentWaitingListStatusMessagePlanner(
    studentToken: String,
    override val planContext: PlanContext
) extends Planner[List[PairOfCourseAndRank]] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[List[PairOfCourseAndRank]] = {
    for {
      // Step 1: 验证学生Token
      studentIDOpt <- validateStudentToken(studentToken)
      _ <- studentIDOpt match {
        case Some(studentID) => IO(logger.info(s"学生验证成功，Student ID: ${studentID}"))
        case None =>
          IO.raiseError(new IllegalStateException(s"Token验证失败: ${studentToken}"))
      }
      studentID = studentIDOpt.get // 此时已经验证通过

      // Step 2: 检查学期阶段是否为Phase2
      currentPhase <- checkCurrentPhase()
      _ <- if (currentPhase == Phase.Phase2) {
        IO(logger.info(s"当前学期阶段: ${currentPhase}，满足Phase2要求"))
      } else {
        IO.raiseError(new IllegalStateException(s"当前阶段为${currentPhase}，不为Phase2，查询失败"))
      }

      // Step 3: 查询WaitingListTable表中的数据
      waitingListData <- fetchStudentWaitingListData(studentID)
      _ <- IO(logger.info(s"成功获取Waiting List数据，学生ID: ${studentID}, 数据条数: ${waitingListData.size}"))

      // Step 4: 将数据库结果封装成PairOfCourseAndRank
      pairs <- transformToPairOfCourseAndRank(waitingListData)
      _ <- IO(logger.info(s"已完成PairOfCourseAndRank转换，数据条数: ${pairs.size}"))

    } yield pairs
  }

  private def fetchStudentWaitingListData(studentID: Int)(using PlanContext): IO[List[Json]] = {
    logger.info(s"开始查询学生ID为${studentID}的Waiting List数据")
    val query =
      s"""
        SELECT course_id, position
        FROM ${schemaName}.waiting_list_table
        WHERE student_id = ?
        """
    val params = List(SqlParameter("Int", studentID.toString))
    readDBRows(query, params)
  }

  private def transformToPairOfCourseAndRank(waitingListData: List[Json])(using PlanContext): IO[List[PairOfCourseAndRank]] = {
    logger.info("开始将Waiting List数据转换为PairOfCourseAndRank")

    waitingListData.traverse { json =>
      for {
        courseID <- IO(decodeField[Int](json, "course_id"))
        rank <- IO(decodeField[Int](json, "position"))

        // 查询课程详细信息
        courseInfoOpt <- fetchCourseInfoByID(courseID)
        _ <- courseInfoOpt match {
          case Some(courseInfo) => IO(logger.info(s"成功获取课程信息：课程ID ${courseID}"))
          case None =>
            IO.raiseError(new IllegalStateException(s"课程ID为${courseID}的课程信息不存在"))
        }
        courseInfo = courseInfoOpt.get

      } yield PairOfCourseAndRank(course = courseInfo, rank = rank)
    }
  }
}