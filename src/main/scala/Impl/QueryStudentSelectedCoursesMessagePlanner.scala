package Impl


import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import Objects.SemesterPhaseService.Phase
import Objects.SemesterPhaseService.SemesterPhase
import Objects.CourseManagementService.CourseTime
import APIs.SemesterPhaseService.QuerySemesterPhaseStatusMessage
import Utils.CourseSelectionProcess.validateStudentToken
import Utils.CourseSelectionProcess.fetchCourseInfoByID
import Objects.CourseManagementService.CourseInfo
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
import cats.implicits._
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
import Objects.SystemLogService.SystemLogEntry
import Utils.CourseSelectionProcess.recordCourseSelectionOperationLog
import Objects.CourseManagementService.DayOfWeek
import Objects.CourseManagementService.TimePeriod
import Objects.SemesterPhaseService.Permissions
import cats.effect.IO
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}
import Objects.SemesterPhaseService.Permissions

case class QueryStudentSelectedCoursesMessagePlanner(
  studentToken: String,
  override val planContext: PlanContext
) extends Planner[List[CourseInfo]] {
  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[List[CourseInfo]] = {
    for {
      // Step 1: 验证studentToken并获取studentID
      _ <- IO(logger.info(s"验证学生Token：$studentToken"))
      studentIDOpt <- validateStudentToken(studentToken)
      studentID <- IO {
        studentIDOpt.getOrElse {
          logger.error(s"学生Token验证失败或Token无效，无法获取学生ID")
          throw new IllegalStateException(s"学生Token验证失败，无法获取学生ID")
        }
      }

      // Step 2: 验证当前学期阶段是否为Phase2
      _ <- IO(logger.info(s"查询当前学期阶段信息"))
      semesterPhaseResult <- QuerySemesterPhaseStatusMessage(studentToken).send
      _ <- IO(logger.info(s"成功获取当前学期阶段信息：${semesterPhaseResult}"))
      currentPhase = semesterPhaseResult.currentPhase
      _ <- IO {
        if (currentPhase != Phase.Phase2) {
          logger.error("当前阶段尚未抽签完成，无法查询选中课程！")
          throw new IllegalStateException("当前阶段尚未抽签完成，无法查询选中课程！")
        }
      }
      _ <- IO(logger.info(s"当前阶段验证通过：$currentPhase（正选阶段Phase2）"))

      // Step 3: 查询studentID对应的选中课程信息
      _ <- IO(logger.info(s"查询学生ID为${studentID}的选中课程信息"))
      selectedCoursesQuery =
        s"""
          SELECT course_id
          FROM ${schemaName}.course_selection_table
          WHERE student_id = ?
        """
      selectedCoursesRaw <- readDBRows(
        selectedCoursesQuery,
        List(SqlParameter("Int", studentID.toString))
      )
      courseIDs <- IO {
        selectedCoursesRaw.map(json => decodeField[Int](json, "course_id"))
      }
      _ <- IO(logger.info(s"学生ID为${studentID}的选中课程ID列表：${courseIDs.mkString(", ")}"))

      // Step 4: 构造CourseInfo对象列表
      courseInfoList <- courseIDs
        .map(fetchCourseInfoByID)
        .sequence
        .map(_.flatten) // 过滤掉结果中可能的None值
      _ <- IO(logger.info(s"成功构造课程信息列表，包含${courseInfoList.length}门课程：${courseInfoList.map(_.courseID).mkString(", ")}"))

    } yield courseInfoList
  }
}