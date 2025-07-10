package Impl

import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import Objects.SemesterPhaseService.Phase
import Utils.CourseSelectionProcess._
import Objects.CourseManagementService.CourseInfo
import cats.effect.IO
import org.slf4j.LoggerFactory
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
import cats.implicits._
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Objects.CourseManagementService.CourseTime
import Objects.SystemLogService.SystemLogEntry
import Objects.CourseManagementService.DayOfWeek
import Objects.CourseManagementService.TimePeriod
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
import Utils.CourseSelectionProcess.validateStudentToken
import Utils.CourseSelectionProcess.fetchCourseInfoByID
import Utils.CourseSelectionProcess.checkIsDropAllowed
import Utils.CourseSelectionProcess.recordCourseSelectionOperationLog
import Objects.CourseManagementService.CourseInfo
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class RemovePreselectedCourseMessagePlanner(
  studentToken: String,
  courseID: Int,
  override val planContext: PlanContext
) extends Planner[String] {
  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[String] = {
    for {
      // Step 1: Validate student token
      _ <- IO(logger.info(s"[Step 1] 验证学生Token: ${studentToken}"))
      studentIDOpt <- validateStudentToken(studentToken)
      studentID <- IO {
        studentIDOpt.getOrElse {
          val errorMessage = "学生Token验证失败！"
          logger.error(errorMessage)
          throw new IllegalStateException(errorMessage)
        }
      }
      _ <- IO(logger.info(s"[Step 1] 学生ID验证成功: ${studentID}"))

      // Step 2: Verify course existence
      _ <- IO(logger.info(s"[Step 2] 检查课程是否存在，课程ID: ${courseID}"))
      courseInfoOpt <- fetchCourseInfoByID(courseID)
      _ <- IO {
        if (courseInfoOpt.isEmpty) {
          val errorMessage = "课程不存在！"
          logger.error(errorMessage)
          throw new IllegalStateException(errorMessage)
        }
      }
      _ <- IO(logger.info(s"[Step 2] 课程ID存在: ${courseID}"))

      // Step 3: Validate current phase
      _ <- IO(logger.info("[Step 3] 验证当前阶段是否允许移除预选课程"))
      currentPhase <- checkCurrentPhase()
      _ <- IO {
        if (currentPhase != Phase.Phase1)
          throw new IllegalArgumentException("当前阶段下不允许预选课程！")
      }

      // Edited by Alex_Wei on 7.6: checkIsDropAllowed -> checkIsRemovePreselectionAllowed
      removeAllowed <- checkIsRemovePreselectionAllowed()
      _ <- if (!removeAllowed)
        IO.raiseError(new IllegalArgumentException("未开启移除预选权限！"))
      else IO.unit
      _ <- IO(logger.info("[Step 3] 当前阶段允许移除预选课程"))

      alreadyPreselected <- isCourseAlreadyPreselected(studentID, courseID)
      _ <- if (! alreadyPreselected)
        IO.raiseError(new IllegalArgumentException("未预选该课程"))
      else IO.unit

      // Step 4: Remove preselected course record
      _ <- IO(logger.info(s"[Step 4] 移除预选课程记录，学生ID: ${studentID}, 课程ID: ${courseID}"))
      deleteQuery <- IO(s"""
        DELETE FROM ${schemaName}.course_preselection_table
        WHERE student_id = ? AND course_id = ?
      """)
      deleteParams <- IO(
        List(
          SqlParameter("Int", studentID.toString),
          SqlParameter("Int", courseID.toString)
        )
      )
      deleteResult <- writeDB(deleteQuery, deleteParams)
      _ <- IO {
        if (deleteResult == "Operation(s) done successfully") {
          logger.info(s"[Step 4] 移除预选记录成功，学生ID: ${studentID}, 课程ID: ${courseID}")
        } else {
          val errorMessage = s"[Step 4] 移除预选记录失败，学生ID: ${studentID}, 课程ID: ${courseID}"
          logger.error(errorMessage)
          throw new IllegalStateException(errorMessage)
        }
      }

      // Step 5: Record operation log
      _ <- IO(logger.info(s"[Step 5] 记录移除预选课程的操作日志"))
      logRecorded <- recordCourseSelectionOperationLog(
        studentID,
        action = "REMOVE_PRESELECTED_COURSE",
        courseID = Some(courseID),
        details = s"学生ID: ${studentID}移除了预选课程ID: ${courseID}"
      )
      _ <- IO {
        if (logRecorded) {
          logger.info(s"[Step 5] 操作日志记录成功")
        } else {
          val errorMessage = "[Step 5] 操作日志记录失败"
          logger.error(errorMessage)
          throw new IllegalStateException(errorMessage)
        }
      }
    } yield "移除预选成功！"
  }
  private def isCourseAlreadyPreselected(studentID: Int, courseID: Int)(using PlanContext): IO[Boolean] = {
    querySQL =
      s"""
          SELECT 1
          FROM ${schemaName}.course_preselection_table
          WHERE course_id = ? AND student_id = ?
      """
    queryParams = List(
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