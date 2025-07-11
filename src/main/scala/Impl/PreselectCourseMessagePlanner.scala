package Impl

import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import Objects.SemesterPhaseService.Phase
import Objects.CourseManagementService.CourseTime
import Objects.CourseManagementService.DayOfWeek
import Objects.CourseManagementService.TimePeriod
import Objects.CourseManagementService.CourseInfo
import Objects.SystemLogService.SystemLogEntry
import Utils.CourseSelectionProcess._
import Utils.CourseSelectionProcess.validateStudentToken
import Utils.CourseSelectionProcess.fetchCourseInfoByID
import Utils.CourseSelectionProcess.checkCurrentPhase
import Utils.CourseSelectionProcess.validateStudentCourseTimeConflict
import Utils.CourseSelectionProcess.recordCourseSelectionOperationLog
import Utils.CourseSelectionProcess.checkIsSelectionAllowed
import cats.effect.IO
import org.slf4j.LoggerFactory
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}

case class PreselectCourseMessagePlanner(
  studentToken: String,
  courseID: Int,
  override val planContext: PlanContext
) extends Planner[String] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[String] = {
    for {
      // Step 1: Validate student token and retrieve student ID
      _ <- IO(logger.info(s"开始验证学生Token: ${studentToken}"))
      studentIDOpt <- validateStudentToken(studentToken)
      studentID <- studentIDOpt match {
        case Some(id) => IO(id)
        case None =>
          IO.raiseError(new IllegalArgumentException("学生Token无效或不存在"))
      }

      _ <- IO(logger.info(s"学生Token验证通过，学生ID为: ${studentID}"))

      // Step 2: Fetch course info and validate course existence
      _ <- IO(logger.info(s"开始查询课程ID: ${courseID}"))
      courseInfoOpt <- fetchCourseInfoByID(courseID)
      courseInfo <- courseInfoOpt match {
        case Some(info) => IO(info)
        case None =>
          IO.raiseError(new IllegalArgumentException("课程不存在！"))
      }

      _ <- IO(logger.info(s"课程信息查询成功，课程ID: ${courseInfo.courseID}"))

      // Step 3: Validate current phase
      _ <- IO(logger.info(s"验证当前阶段是否允许预选课程"))
      currentPhase <- checkCurrentPhase()
      _ <- IO {
        if (currentPhase != Phase.Phase1)
          throw new IllegalArgumentException("当前阶段下不允许预选课程！")
      }

      // Step 3.1: Ensure selection is allowed
      // Edited on 7.6 by Alex_Wei: checkIsSelectionAllowed -> checkIsPreselectionAllowed
      selectionAllowed <- checkIsPreselectionAllowed()
      _ <- if (!selectionAllowed)
        IO.raiseError(new IllegalArgumentException("预选权限未开启！"))
      else IO.unit
      
      alreadyPreselected <- isCourseAlreadyPreselected(studentID, courseID)
      _ <- if (alreadyPreselected)
        IO.raiseError(new IllegalArgumentException("已预选该课程"))
      else IO.unit

      // Step 4: Course conflict validation
      _ <- IO(logger.info(s"检测预选此课程是否与已选课程时间冲突"))
      isConflict <- validateStudentCourseTimeConflict(studentID, courseID)
      _ <- if (isConflict)
        IO.raiseError(new IllegalArgumentException("时间冲突，无法预选课程。"))
      else IO.unit

      // Step 5: Add student to preselection pool
      _ <- IO(logger.info(s"将学生加入到课程的预选池"))
      insertSQL =
        s"""
           INSERT INTO ${schemaName}.course_preselection_table (course_id, student_id)
           VALUES (?, ?)
        """
      insertParams = List(
        SqlParameter("Int", courseID.toString),
        SqlParameter("Int", studentID.toString)
      )
      _ <- writeDB(insertSQL, insertParams)

      // Step 6: Record operation log
      // Edited on 7.6 by Alex_Wei: when LogRecording raises an error, we should raise an error
      _ <- IO(logger.info(s"记录课程预选操作日志"))
      logRecorded <- recordCourseSelectionOperationLog(
        studentID,
        action = "预选",
        courseID = Some(courseID),
        details = s"学生ID: ${studentID}预选课程ID: ${courseID}"
      )
      _ <- IO {
        if (logRecorded) {
          logger.info(s"操作日志记录成功")
        } else {
          val errorMessage = "操作日志记录失败"
          logger.error(errorMessage)
          throw new IllegalStateException(errorMessage)
        }
      }
    } yield "预选成功！"
  }
  private def isCourseAlreadyPreselected(studentID: Int, courseID: Int)(using PlanContext): IO[Boolean] = {
    val querySQL =
      s"""
          SELECT 1
          FROM ${schemaName}.course_preselection_table
          WHERE course_id = ? AND student_id = ?
      """
    val queryParams = List(
      SqlParameter("Int", courseID.toString),
      SqlParameter("Int", studentID.toString)
    )
    for {
      resultOpt <- readDBJsonOptional(querySQL, queryParams)
      alreadyExists = resultOpt.isDefined
      _ <- IO(logger.info(s"学生ID ${studentID} 预检查课程ID ${courseID} 是否已预选: ${alreadyExists}"))
    } yield alreadyExists
  }
}