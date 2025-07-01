package Impl


import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import Objects.SemesterPhaseService.Phase
import Utils.CourseSelectionProcess.checkCurrentPhase
import Objects.CourseManagementService.CourseInfo
import Utils.CourseSelectionProcess.validateStudentToken
import Utils.CourseSelectionProcess.fetchCourseInfoByID
import org.slf4j.LoggerFactory
import cats.effect.IO
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
import cats.implicits._
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
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class QueryStudentPreselectedCoursesMessagePlanner(
  studentToken: String,
  override val planContext: PlanContext
) extends Planner[List[CourseInfo]] {
  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[List[CourseInfo]] = {
    for {
      // Step 1: Validate student token and extract student ID
      _ <- IO(logger.info(s"开始鉴权学生的Token: ${studentToken}"))
      studentIDOpt <- validateStudentToken(studentToken)
      studentID <- IO {
        studentIDOpt.getOrElse {
          logger.error("学生Token验证失败，不存在对应的学生ID")
          throw new IllegalStateException("学生Token验证失败")
        }
      }
      _ <- IO(logger.info(s"学生ID解析成功: ${studentID}"))

      // Step 2: Verify the current phase is Phase1
      _ <- IO(logger.info("检查当前学期阶段是否为Phase1"))
      currentPhase <- checkCurrentPhase()
      _ <- IO {
        if (currentPhase != Phase.Phase1) {
          logger.error(s"当前阶段[${currentPhase}]已抽签完成，无法查询预选课程数据")
          throw new IllegalStateException("当前阶段已抽签完成")
        }
      }
      _ <- IO(logger.info("当前学期阶段验证成功，阶段为Phase1"))

      // Step 3: Query CoursePreselectionTable for preselected courses
      _ <- IO(logger.info(s"查询学生[${studentID}]的预选课程数据"))
      preselectionQuery =
        s"SELECT course_id FROM ${schemaName}.course_preselection_table WHERE student_id = ?"
      preselectionResult <- readDBRows(preselectionQuery, List(SqlParameter("Int", studentID.toString)))
      courseIDs = preselectionResult.map(json => decodeField[Int](json, "course_id"))
      _ <- IO(logger.info(s"学生[${studentID}]预选的课程ID列表: ${courseIDs}"))

      // Step 4: Fetch detailed course information for each course ID
      _ <- IO(logger.info("开始查询每个课程ID的详细信息"))
      courseInfoList <- courseIDs.traverse(fetchCourseInfoByID).map(_.flatten)
      _ <- IO(logger.info(s"成功查询学生[${studentID}]预选课程详细信息，共计${courseInfoList.size}门课程"))
    } yield courseInfoList
  }
}