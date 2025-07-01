package Impl


import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import Utils.CourseSelectionProcess.validateStudentToken
import Utils.CourseSelectionProcess.fetchCourseInfoByID
import Utils.CourseSelectionProcess.recordCourseSelectionOperationLog
import Utils.CourseSelectionProcess.validateStudentCourseTimeConflict
import Objects.CourseManagementService.CourseInfo
import org.slf4j.LoggerFactory
import cats.effect.IO
import io.circe._
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
import Objects.CourseManagementService.DayOfWeek
import Objects.CourseManagementService.TimePeriod
import Objects.CourseManagementService.CourseInfo
import io.circe.syntax._
import io.circe.generic.auto._
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class PreselectCourseMessagePlanner(
                                          studentToken: String,
                                          courseID: Int,
                                          override val planContext: PlanContext
                                        ) extends Planner[String] {
  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[String] = {
    for {
      // Step 1: Authenticate the studentToken and obtain studentID
      _ <- IO(logger.info(s"开始验证学生Token: $studentToken"))
      studentIDOpt <- validateStudentToken(studentToken)
      studentID <- IO.fromOption(studentIDOpt)(new Exception("学生Token无效或鉴权失败"))

      // Step 2: Verify courseID validity
      _ <- IO(logger.info(s"检查课程ID $courseID 是否有效"))
      courseInfoOpt <- fetchCourseInfoByID(courseID)
      courseInfo <- IO.fromOption(courseInfoOpt)(new Exception("课程不存在！"))

      // Step 3: Check if the current phase is Phase1
      _ <- IO(logger.info("检查当前阶段是否为Phase1"))
      currentPhase <- getCurrentPhase
      _ <- if (currentPhase != "Phase1") {
        IO.raiseError(new Exception("当前阶段不允许预选课程。"))
      } else {
        IO.unit
      }

      // Step 4: Check time conflict
      _ <- IO(logger.info(s"检查学生ID为 $studentID 和课程ID为 $courseID 的时间冲突"))
      isConflict <- validateStudentCourseTimeConflict(studentID, courseID)
      _ <- if (isConflict) {
        IO.raiseError(new Exception("时间冲突，无法预选课程。"))
      } else {
        IO.unit
      }

      // Step 5: Add the student to the preselection pool
      _ <- IO(logger.info(s"将学生ID $studentID 添加到课程ID $courseID 的预选池"))
      preselectionInsertResult <- addStudentToPreselectionPool(studentID, courseID)
      _ <- if (preselectionInsertResult == "Operation(s) done successfully") {
        IO.unit
      } else {
        IO.raiseError(new Exception("预选操作失败，请稍后再试。"))
      }

      // Step 6: Record course selection operation log
      _ <- IO(logger.info("记录此次预选操作的日志"))
      logRecorded <- recordCourseSelectionOperationLog(
        studentID = studentID,
        action = "PRESELECT_COURSE",
        courseID = Some(courseID),
        details = s"学生ID $studentID 预选课程ID $courseID"
      )

      _ <- if (logRecorded) {
        IO(logger.info("日志记录成功"))
      } else {
        IO(logger.error("日志记录失败"))
      }

      // Step 7: Return success message
      _ <- IO(logger.info("课程预选成功！"))
    } yield "预选成功！"
  }

  private def getCurrentPhase(using PlanContext): IO[String] = {
    val sqlQuery = s"SELECT current_phase FROM ${schemaName}.system_state LIMIT 1"
    readDBString(sqlQuery, List())
  }

  private def addStudentToPreselectionPool(studentID: Int, courseID: Int)(using PlanContext): IO[String] = {
    val sqlQuery =
      s"""
         |INSERT INTO ${schemaName}.course_preselection_table (course_id, student_id)
         |VALUES (?, ?)
       """.stripMargin
    val parameters = List(
      SqlParameter("Int", courseID.toString),
      SqlParameter("Int", studentID.toString)
    )
    writeDB(sqlQuery, parameters)
  }
}