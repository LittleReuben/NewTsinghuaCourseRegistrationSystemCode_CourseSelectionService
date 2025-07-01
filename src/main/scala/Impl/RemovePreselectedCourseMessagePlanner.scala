package Impl


import Objects.SemesterPhaseService.Phase
import Utils.CourseSelectionProcess.checkCurrentPhase
import Utils.CourseSelectionProcess.validateStudentToken
import Utils.CourseSelectionProcess.fetchCourseInfoByID
import Utils.CourseSelectionProcess.recordCourseSelectionOperationLog
import Objects.CourseManagementService.CourseInfo
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.slf4j.LoggerFactory
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
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
import Objects.CourseManagementService.DayOfWeek
import Objects.CourseManagementService.TimePeriod
import Objects.CourseManagementService.CourseInfo
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class RemovePreselectedCourseMessagePlanner(
  studentToken: String,
  courseID: Int,
  override val planContext: PlanContext
) extends Planner[String] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[String] = {
    for {
      // Step 1: 验证学生Token并获取studentID
      _ <- IO(logger.info(s"[Step 1] 验证学生Token: ${studentToken}"))
      studentIDOpt <- validateStudentToken(studentToken)
      studentID <- IO {
        studentIDOpt.getOrElse(
          throw new IllegalArgumentException(s"[Step 1.1] 学生Token验证失败: ${studentToken}")
        )
      }
      _ <- IO(logger.info(s"[Step 1.2] 学生Token验证成功，学生ID: ${studentID}"))

      // Step 2: 验证课程是否存在
      _ <- IO(logger.info(s"[Step 2] 验证课程ID: ${courseID}是否存在"))
      courseInfoOpt <- fetchCourseInfoByID(courseID)
      _ <- IO {
        courseInfoOpt.getOrElse(
          throw new IllegalStateException(s"[Step 2.1] 课程不存在, ID: ${courseID}")
        )
      }
      _ <- IO(logger.info(s"[Step 2.2] 课程验证通过，课程ID: ${courseID}"))

      // Step 3: 检查当前学期阶段是否为Phase1
      _ <- IO(logger.info(s"[Step 3] 检查当前学期阶段是否为Phase1"))
      currentPhase <- checkCurrentPhase()
      _ <- IO {
        if (currentPhase != Phase.Phase1) {
          throw new IllegalStateException(s"[Step 3.1] 当前阶段不允许移除预选课程, 当前阶段: ${currentPhase}")
        }
      }
      _ <- IO(logger.info(s"[Step 3.2] 当前学期阶段为Phase1，允许移除预选课程"))

      // Step 4: 移除学生的预选记录
      _ <- IO(logger.info(s"[Step 4] 从数据库移除学生ID: ${studentID} 和课程ID: ${courseID} 的预选记录"))
      deleteSql <- IO {
        s"""
        DELETE FROM ${schemaName}.course_preselection_table
        WHERE course_id = ? AND student_id = ?
        """
      }
      deleteResult <- writeDB(deleteSql, List(
        SqlParameter("Int", courseID.toString), 
        SqlParameter("Int", studentID.toString)
      ))
      _ <- IO(logger.info(s"[Step 4.1] 预选记录移除结果: ${deleteResult}"))

      // Step 5: 记录操作日志
      _ <- IO(logger.info(s"[Step 5] 记录移除预选课程的操作日志"))
      logResult <- recordCourseSelectionOperationLog(
        studentID = studentID,
        action = "REMOVE_PRESELECTED_COURSE",
        courseID = Some(courseID),
        details = "移除预选课程成功"
      )
      _ <- IO(logger.info(s"[Step 5.1] 操作日志记录结果: ${if (logResult) "成功" else "失败"}"))

    } yield "移除预选成功！"
  }
}