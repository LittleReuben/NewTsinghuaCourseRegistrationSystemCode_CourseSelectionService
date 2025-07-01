package Impl


import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import Objects.CourseSelectionService.PairOfCourseAndRank
import Objects.CourseManagementService.CourseInfo
import cats.effect.IO
import Utils.CourseSelectionProcess.validateStudentToken
import Utils.CourseSelectionProcess.checkCurrentPhase
import Utils.CourseSelectionProcess.fetchCourseInfoByID
import Objects.SemesterPhaseService.Phase
import org.slf4j.LoggerFactory
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
import cats.implicits.*
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class QueryStudentWaitingListStatusMessagePlanner(
    studentToken: String,
    override val planContext: PlanContext
) extends Planner[List[PairOfCourseAndRank]] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[List[PairOfCourseAndRank]] = {
    for {
      // Step 1: Validate student token and get student ID
      _ <- IO(logger.info(s"验证学生Token: ${studentToken}"))
      studentIDOpt <- validateStudentToken(studentToken)
      studentID <- IO {
        studentIDOpt.getOrElse(throw new IllegalStateException("Token验证失败"))
      }

      _ <- IO(logger.info(s"学生ID: ${studentID}"))

      // Step 2: Check current phase
      _ <- IO(logger.info("检查当前学期阶段"))
      currentPhase <- checkCurrentPhase()
      _ <- IO {
        if (currentPhase != Phase.Phase2) {
          throw new IllegalStateException("当前阶段尚未抽签完成")
        }
      }

      // Step 3: Query Waiting List courses and rank info
      _ <- IO(logger.info("查询学生Waiting List数据"))
      waitingListData <- queryStudentWaitingListData(studentID)

    } yield waitingListData
  }

  private def queryStudentWaitingListData(studentID: Int)(using PlanContext): IO[List[PairOfCourseAndRank]] = {
    logger.info(s"开始查询学生ID: ${studentID}的Waiting List数据")

    val sqlQuery =
      s"""
SELECT course_id, position
FROM ${schemaName}.waiting_list_table
WHERE student_id = ?
""".stripMargin

    val parameters = List(SqlParameter("Int", studentID.toString))

    for {
      queryResult <- readDBRows(sqlQuery, parameters)
      result <- queryResult.traverse { json =>
        val courseID = decodeField[Int](json, "course_id")
        val position = decodeField[Int](json, "position")

        for {
          courseInfoOpt <- fetchCourseInfoByID(courseID)
          courseInfo <- IO {
            courseInfoOpt.getOrElse(throw new IllegalStateException(s"课程ID不存在: ${courseID}"))
          }
        } yield PairOfCourseAndRank(courseInfo, position)
      }
    } yield result
  }
}