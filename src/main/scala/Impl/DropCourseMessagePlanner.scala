package Impl


import Objects.SemesterPhaseService.Phase
import Utils.CourseSelectionProcess.checkCurrentPhase
import Objects.CourseManagementService.CourseTime
import Utils.CourseSelectionProcess.validateStudentToken
import Utils.CourseSelectionProcess.checkIsDropAllowed
import Objects.SystemLogService.SystemLogEntry
import Utils.CourseSelectionProcess.recordCourseSelectionOperationLog
import Objects.CourseManagementService.TimePeriod
import Utils.CourseSelectionProcess.fetchCourseInfoByID
import Utils.CourseSelectionProcess.removeStudentFromWaitingList
import Utils.CourseSelectionProcess.removeStudentFromSelectedCourses
import Objects.CourseManagementService.DayOfWeek
import Objects.CourseManagementService.CourseInfo
import Objects.SemesterPhaseService.Permissions
import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.slf4j.LoggerFactory
import org.joda.time.DateTime
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
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
import Objects.SemesterPhaseService.Permissions
import Utils.CourseSelectionProcess._
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class DropCourseMessagePlanner(studentToken: String, courseID: Int, override val planContext: PlanContext)
    extends Planner[String] {
  private val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[String] = {
    for {
      // Step 1: Validate Student Token
      _ <- IO(logger.info(s"[Step 1] 开始验证学生Token: ${studentToken}"))
      studentIDOpt <- validateStudentToken(studentToken)
      studentID <- IO {
        studentIDOpt.getOrElse(
          throw new IllegalArgumentException("学生Token验证失败！")
        )
      }
      _ <- IO(logger.info(s"[Step 1] 学生Token验证通过，学生ID: ${studentID}"))

      // Step 2: Fetch Course Info
      _ <- IO(logger.info(s"[Step 2] 验证课程ID ${courseID} 是否存在"))
      courseInfoOpt <- fetchCourseInfoByID(courseID)
      courseInfo <- IO {
        courseInfoOpt.getOrElse(
          throw new IllegalArgumentException("课程不存在！")
        )
      }
      _ <- IO(logger.info(s"[Step 2] 课程验证通过，课程信息: ${courseInfo}"))

      // Step 3: Check Current Phase
      _ <- IO(logger.info("[Step 3] 开始检查当前学期阶段"))
      currentPhase <- checkCurrentPhase()
      _ <- IO {
        if (currentPhase != Phase.Phase2)
          throw new IllegalArgumentException("当前阶段不允许退课。")
      }
      _ <- IO(logger.info("[Step 3] 当前学期阶段验证通过，为 Phase2"))

      // Step 4: Check Drop Permission
      _ <- IO(logger.info("[Step 4] 检查退课权限是否开启"))
      isDropAllowed <- checkIsDropAllowed()
      _ <- IO {
        if (!isDropAllowed)
          throw new IllegalArgumentException("退课权限未开启！")
      }
      _ <- IO(logger.info("[Step 4] 退课权限检查通过，允许退课"))

      // Step 5: Check Student Course Status
      _ <- IO(logger.info("[Step 5] 检查学生是否在选上的课程或等待列表中"))
      studentSelectedQuery <- IO {
        s"""
        SELECT student_id FROM ${schemaName}.course_selection_table
        WHERE course_id = ? AND student_id = ?
        """
      }
      waitingListQuery <- IO {
        s"""
        SELECT student_id FROM ${schemaName}.waiting_list_table
        WHERE course_id = ? AND student_id = ?
        """
      }
      studentSelected <- readDBJsonOptional(
        studentSelectedQuery,
        List(
          SqlParameter("Int", courseID.toString),
          SqlParameter("Int", studentID.toString)
        )
      ).map(_.isDefined)
      studentInWaitingList <- readDBJsonOptional(
        waitingListQuery,
        List(
          SqlParameter("Int", courseID.toString),
          SqlParameter("Int", studentID.toString)
        )
      ).map(_.isDefined)

      _ <- IO {
        if (!studentSelected && !studentInWaitingList)
          throw new IllegalArgumentException("未选择该门课！")
      }
      _ <- IO(logger.info("[Step 5] 学生选课状态验证通过"))

      // Step 6 & Step 7: Remove Student from Selection or Waiting List
      operationResult <- if (studentSelected) {
        removeStudentFromSelectedCourses(studentID, courseID)
      } else if (studentInWaitingList) {
        removeStudentFromWaitingList(courseID, studentID)
      } else {
        IO.raiseError(new IllegalStateException("[Step 6] 学生选课状态逻辑异常"))
      }
      _ <- IO(logger.info(s"[Step 6] 处理学生退课操作结果: ${operationResult}"))

      // Step 8: Record Operation Log
      _ <- IO(logger.info("[Step 8] 记录退课操作日志"))
      logRecorded <- recordCourseSelectionOperationLog(
        studentID = studentID,
        action = "退课",
        courseID = Some(courseID),
        details = s"学生退课，课程ID: ${courseID}"
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
      // Step 10: Check Participation History and Add Entry if Missing
      _ <- IO(logger.info("[Step 9] 检查课程参与历史记录如果不存在则新增记录"))
      participationHistoryQuery <- IO {
        s"""
        SELECT student_id FROM ${schemaName}.course_participation_history_table
        WHERE course_id = ? AND student_id = ?
        """
      }
      historyExists <- readDBJsonOptional(
        participationHistoryQuery,
        List(
          SqlParameter("Int", courseID.toString),
          SqlParameter("Int", studentID.toString)
        )
      ).map(_.isDefined)
      _ <- IO {
        if (!historyExists) {
          val insertHistoryQuery =
            s"""
            INSERT INTO ${schemaName}.course_participation_history_table (course_id, student_id)
            VALUES (?, ?)
            """
          val params = List(
            SqlParameter("Int", courseID.toString),
            SqlParameter("Int", studentID.toString)
          )
          writeDB(insertHistoryQuery, params).unsafeRunSync()(using cats.effect.unsafe.implicits.global)
          logger.info(s"新增课程参与历史记录完成，学生ID: ${studentID}, 课程ID: ${courseID}")
        } else {
          logger.info(s"课程参与历史记录已存在，学生ID: ${studentID}, 课程ID: ${courseID}")
        }
      }
    } yield "退课成功！"
  }
}