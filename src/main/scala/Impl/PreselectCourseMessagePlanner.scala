package Impl


import Objects.CourseManagementService.CourseTime
import Utils.CourseSelectionProcess.validateStudentCourseTimeConflict
import Utils.CourseSelectionProcess.validateStudentToken
import Utils.CourseSelectionProcess.fetchCourseInfoByID
import Objects.SystemLogService.SystemLogEntry
import Utils.CourseSelectionProcess.recordCourseSelectionOperationLog
import Objects.CourseManagementService.DayOfWeek
import Objects.CourseManagementService.TimePeriod
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
import Objects.CourseManagementService.CourseInfo
import Utils.CourseSelectionProcess._
import Objects.CourseManagementService.{CourseInfo, CourseTime}
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class PreselectCourseMessagePlanner(
    studentToken: String,
    courseID: Int,
    override val planContext: PlanContext
) extends Planner[String] {

  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  // Helper function for logging and exception handling
  private def handleError(message: String): IO[Unit] =
    IO(logger.error(message)) *> IO.raiseError(new IllegalStateException(message))

  override def plan(using planContext: PlanContext): IO[String] = {
    for {
      // Step 1: Validate studentToken and obtain studentID
      _ <- IO(logger.info("开始验证学生Token"))
      studentIDOpt <- validateStudentToken(studentToken)
      studentID <- studentIDOpt.fold(handleError(s"无效的学生Token: ${studentToken}"))(IO.pure)
      _ <- IO(logger.info(s"学生ID: ${studentID}验证通过"))

      // Step 2: Fetch course information and validate existence
      _ <- IO(logger.info(s"开始检查课程ID ${courseID}是否有效"))
      courseInfoOpt <- fetchCourseInfoByID(courseID)
      courseInfo <- courseInfoOpt.fold(handleError("课程不存在！"))(IO.pure)
      _ <- IO(logger.info(s"课程信息: ${courseInfo}"))

      // Step 3: Validate phase
      _ <- IO(logger.info("开始检查当前阶段"))
      currentPhase <- readDBString(s"SELECT current_phase FROM ${schemaName}.system_config", Nil)
      _ <- if (currentPhase != "Phase1") handleError("当前阶段不允许预选课程。") else IO(logger.info("当前阶段为Phase1，可以进行预选课程"))

      // Step 4: Validate time conflict
      _ <- IO(logger.info("开始检查时间冲突"))
      isConflict <- validateStudentCourseTimeConflict(studentID, courseID)
      _ <- if (isConflict) handleError("时间冲突，无法预选课程。") else IO(logger.info("时间检查通过，无冲突"))

      // Step 5: Add student to course preselection table
      _ <- IO(logger.info("开始将学生加入课程预选池"))
      _ <- writeDB(
        s"INSERT INTO ${schemaName}.course_preselection_table (course_id, student_id) VALUES (?, ?)",
        List(
          SqlParameter("Int", courseID.toString),
          SqlParameter("Int", studentID.toString)
        )
      )
      _ <- IO(logger.info(s"成功将学生 ${studentID} 加入课程 ${courseID} 的预选池"))

      // Step 6: Record operation log
      _ <- IO(logger.info("开始记录选课操作日志"))
      _ <- recordCourseSelectionOperationLog(
        studentID = studentID,
        action = "PRESELECT_COURSE",
        courseID = Some(courseID),
        details = s"学生ID ${studentID} 预选课程ID ${courseID} 成功"
      )
      _ <- IO(logger.info("日志记录成功"))

    } yield "预选成功！"
  }
}