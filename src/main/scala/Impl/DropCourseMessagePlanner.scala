package Impl


import Objects.SemesterPhaseService.Phase
import Utils.CourseSelectionProcess._
import Objects.CourseManagementService.CourseInfo
import Objects.SystemLogService.SystemLogEntry
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
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Utils.CourseSelectionProcess.checkCurrentPhase
import Objects.CourseManagementService.CourseTime
import Utils.CourseSelectionProcess.validateStudentToken
import Utils.CourseSelectionProcess.checkIsDropAllowed
import Utils.CourseSelectionProcess.recordCourseSelectionOperationLog
import Objects.CourseManagementService.TimePeriod
import Utils.CourseSelectionProcess.fetchCourseInfoByID
import Utils.CourseSelectionProcess.removeStudentFromWaitingList
import Utils.CourseSelectionProcess.removeStudentFromSelectedCourses
import Objects.CourseManagementService.DayOfWeek
import Objects.SemesterPhaseService.Permissions
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
import Objects.SemesterPhaseService.Permissions
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class DropCourseMessagePlanner(
                                     studentToken: String,
                                     courseID: Int,
                                     override val planContext: PlanContext
                                   ) extends Planner[String] {
  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[String] = {
    for {
      // Step 1: Validate the student token and retrieve student ID
      _ <- IO(logger.info(s"开始验证学生Token并获取学生ID，Token: ${studentToken}"))
      studentIDOpt <- validateStudentToken(studentToken)
      studentID <- IO(studentIDOpt match {
        case Some(id) => id
        case None =>
          logger.error("Token无效或鉴权失败")
          throw new IllegalArgumentException("Token无效或鉴权失败")
      })

      // Step 2: Fetch course information by courseID to check existence
      _ <- IO(logger.info(s"开始查询课程信息以检查课程是否存在，课程ID: ${courseID}"))
      courseInfoOpt <- fetchCourseInfoByID(courseID)
      _ <- IO(courseInfoOpt match {
        case Some(_) =>
          logger.info(s"课程ID=${courseID} 存在")
        case None =>
          logger.error(s"课程不存在，课程ID=${courseID}")
          throw new IllegalArgumentException("课程不存在！")
      })

      // Step 3: Check the current semester phase
      _ <- IO(logger.info(s"开始验证当前阶段"))
      currentPhase <- checkCurrentPhase()
      _ <- IO {
        if (currentPhase != Phase.Phase2) {
          logger.error(s"当前阶段不允许退课，当前阶段: ${currentPhase}")
          throw new IllegalArgumentException("当前阶段不允许退课。")
        }
      }

      // Step 4: Check if drop permission is allowed
      _ <- IO(logger.info(s"检查退课权限是否被开启"))
      permissionsQuery = s"SELECT allow_student_drop FROM ${schemaName}.semester_phase_permissions"
      allowStudentDrop <- readDBBoolean(permissionsQuery, List())
      isDropAllowed <- checkIsDropAllowed(currentPhase, allowStudentDrop)
      _ <- IO {
        if (!isDropAllowed) {
          logger.error("退课权限未开启")
          throw new IllegalArgumentException("退课权限未开启")
        }
      }

      // Step 5: Check if student is enrolled in the course
      _ <- IO(logger.info(s"检查学生是否已选该门课，学生ID=${studentID}，课程ID=${courseID}"))
      selectionCheckQuery = s"SELECT student_id FROM ${schemaName}.course_selection_table WHERE student_id = ? AND course_id = ?"
      waitingListCheckQuery = s"SELECT student_id FROM ${schemaName}.waiting_list_table WHERE student_id = ? AND course_id = ?"
      isStudentInSelection <- readDBJsonOptional(selectionCheckQuery, List(SqlParameter("Int", studentID.toString), SqlParameter("Int", courseID.toString))).map(_.nonEmpty)
      isStudentInWaitingList <- readDBJsonOptional(waitingListCheckQuery, List(SqlParameter("Int", studentID.toString), SqlParameter("Int", courseID.toString))).map(_.nonEmpty)
      _ <- IO {
        if (!isStudentInSelection && !isStudentInWaitingList) {
          logger.error(s"学生未选择该门课，学生ID=${studentID}，课程ID=${courseID}")
          throw new IllegalArgumentException("未选择该门课！")
        }
      }

      // Step 6: Remove student from selected courses if enrolled
      _ <- if (isStudentInSelection) {
        IO(logger.info(s"学生在课程选上名单中，即将移除，学生ID=${studentID}，课程ID=${courseID}")) >>
          removeStudentFromSelectedCourses(studentID, courseID)
      } else {
        IO.unit
      }

      // Step 7: Remove student from waiting list if enrolled
      _ <- if (isStudentInWaitingList) {
        IO(logger.info(s"学生在课程的等待名单中，即将移除，学生ID=${studentID}，课程ID=${courseID}")) >>
          removeStudentFromWaitingList(courseID, studentID)
      } else {
        IO.unit
      }

      // Step 8: Log the drop course operation
      _ <- IO(logger.info(s"记录退课操作日志，学生ID=${studentID}，课程ID=${courseID}"))
      logRecorded <- recordCourseSelectionOperationLog(
        studentID,
        action = "DROP_COURSE",
        courseID = Some(courseID),
        details = s"学生退选课程，课程ID=${courseID}"
      )

      _ <- IO(logger.info(s"退课操作日志记录完成，结果: ${if (logRecorded) "成功" else "失败"}"))
    } yield "退课成功！"
  }
}