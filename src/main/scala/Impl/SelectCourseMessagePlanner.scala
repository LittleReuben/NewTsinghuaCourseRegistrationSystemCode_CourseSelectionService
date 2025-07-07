package Impl


import Objects.SemesterPhaseService.Phase
import Utils.CourseSelectionProcess.checkCurrentPhase
import Utils.CourseSelectionProcess.checkIsSelectionAllowed
import Utils.CourseSelectionProcess.validateStudentCourseTimeConflict
import Utils.CourseSelectionProcess.validateStudentToken
import Utils.CourseSelectionProcess.recordCourseSelectionOperationLog
import Utils.CourseSelectionProcess.fetchCourseInfoByID
import Utils.CourseSelectionProcess.removeStudentFromWaitingList
import Utils.CourseSelectionProcess.removeStudentFromSelectedCourses
import Objects.CourseManagementService.{CourseTime, CourseInfo, DayOfWeek, TimePeriod}
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
import Common.DBAPI._
import Common.API.{PlanContext, Planner}
import cats.effect.IO
import Common.Object.SqlParameter
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}
import Common.ServiceUtils.schemaName
import Objects.CourseManagementService.CourseTime
import Objects.SystemLogService.SystemLogEntry
import Objects.CourseManagementService.TimePeriod
import Objects.CourseManagementService.DayOfWeek
import Objects.CourseManagementService.CourseInfo
import Objects.SemesterPhaseService.Permissions
import Common.Object.ParameterList
import Utils.CourseSelectionProcess._
import Objects.SemesterPhaseService.{Phase, Permissions}
import org.joda.time.DateTime
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}
import Objects.SemesterPhaseService.Permissions

case class SelectCourseMessagePlanner(
  studentToken: String,
  courseID: Int,
  override val planContext: PlanContext
) extends Planner[String] {

  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using PlanContext): IO[String] = {
    for {
      // Step 1: 验证学生Token并获取学生ID
      _ <- IO(logger.info(s"验证学生Token: ${studentToken}"))
      studentIDOpt <- validateStudentToken(studentToken)
      studentID <- studentIDOpt match {
        case Some(id) =>
          IO(logger.info(s"学生Token验证成功，学生ID: ${id}")).as(id)
        case None =>
          IO(logger.error(s"学生Token验证失败")).flatMap(_ =>
            IO.raiseError(new IllegalArgumentException("Token验证失败"))
          )
      }

      // Step 2: 验证课程是否存在
      _ <- IO(logger.info(s"验证课程ID: ${courseID}是否存在"))
      courseInfoOpt <- fetchCourseInfoByID(courseID)
      courseInfo <- courseInfoOpt match {
        case Some(info) =>
          IO(logger.info(s"查询课程信息成功，课程ID: ${courseID}"))
            .as(info)
        case None =>
          IO(logger.error(s"课程ID: ${courseID}不存在"))
            .flatMap(_ => IO.raiseError(new IllegalArgumentException("课程不存在！")))
      }

      // Step 3: 验证当前阶段和正选权限
      _ <- IO(logger.info(s"检查当前阶段是Phase2"))
      currentPhase <- checkCurrentPhase()
      _ <- IO {
        if (currentPhase != Phase.Phase2) {
          logger.error(s"当前阶段不是Phase2，无法进行选课")
          throw new IllegalArgumentException("当前阶段不允许选课。")
        }
      }

      _ <- IO(logger.info(s"检查选课权限是否开启"))
      isSelectionAllowed <- checkIsSelectionAllowed()
      _ <- IO {
        if (!isSelectionAllowed) {
          logger.error(s"选课权限未开启，无法进行选课")
          throw new IllegalArgumentException("选课权限未开启")
        }
      }

      // Step 4: 检查课程是否已选以及时间冲突
      _ <- checkAlreadySelected(studentID, courseID)
      _ <- checkAlreadyWaited(studentID, courseID)
      isConflict <- validateStudentCourseTimeConflict(studentID, courseID)
      _ <- IO {
        if (isConflict) {
          logger.error(s"学生ID=${studentID}选课时间冲突，课程ID=${courseID}")
          throw new IllegalArgumentException("时间冲突，无法选课。")
        }
      }

      // Step 5: 处理选课逻辑，根据课程容量的情况选择是否加入Waiting List或选课成功
      resultMessage <-
        if (courseInfo.selectedStudentsSize >= courseInfo.courseCapacity) {
          enrollToWaitingList(studentID, courseID)
        } else {
          addStudentToSelectedCourses(studentID, courseID)
        }

      // Step 6: 记录选课操作日志
      // Edited on 7.6 by Alex_Wei: when LogRecording raises an error, we should raise an error
      _ <- IO(logger.info(s"记录选课操作日志"))
      logRecorded <- recordCourseSelectionOperationLog(
        studentID,
        action = "PRESELECT_COURSE",
        courseID = Some(courseID),
        details = s"选课操作结果: ${resultMessage}"
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

      // Step 7: 返回结果消息
    } yield resultMessage
  }

  private def checkAlreadySelected(studentID: Int, courseID: Int)(using PlanContext): IO[Unit] = {
    val query =
      s"""
      SELECT course_id
      FROM ${schemaName}.course_selection_table
      WHERE student_id = ? AND course_id = ?
    """
    readDBJsonOptional(query, List(SqlParameter("Int", studentID.toString), SqlParameter("Int", courseID.toString))).flatMap {
      case Some(_) =>
        IO(logger.error(s"学生ID=${studentID}已选课程ID=${courseID}"))
          .flatMap(_ => IO.raiseError(new IllegalArgumentException("该门课程已选！")))
      case None =>
        IO(logger.info(s"学生ID=${studentID}未选择课程ID=${courseID}"))
    }
  }
  
  private def checkAlreadyWaited(studentID: Int, courseID: Int)(using PlanContext): IO[Unit] = {
    val query =
      s"""
      SELECT course_id
      FROM ${schemaName}.waiting_list_table
      WHERE student_id = ? AND course_id = ?
    """
    readDBJsonOptional(query, List(SqlParameter("Int", studentID.toString), SqlParameter("Int", courseID.toString))).flatMap {
      case Some(_) =>
        IO(logger.error(s"学生ID=${studentID}已候选课程ID=${courseID}"))
          .flatMap(_ => IO.raiseError(new IllegalArgumentException("该门课程已候选！")))
      case None =>
        IO(logger.info(s"学生ID=${studentID}未候选课程ID=${courseID}"))
    }
  }

  private def enrollToWaitingList(studentID: Int, courseID: Int)(using PlanContext): IO[String] = {
    val positionQuery =
      s"""
      SELECT COUNT(*) + 1 AS position
      FROM ${schemaName}.waiting_list_table
      WHERE course_id = ?
    """
    val positionParams = List(SqlParameter("Int", courseID.toString))
    for {
      _ <- IO(logger.info(s"将学生加入Waiting List，课程ID=${courseID}, 学生ID=${studentID}"))
      waitingPosition <- readDBInt(positionQuery, positionParams)
      _ <- addStudentToWaitingList(studentID, courseID, waitingPosition)
    } yield "课程已满员，已加入Waiting List。"
  }

  private def addStudentToWaitingList(studentID: Int, courseID: Int, position: Int)(using PlanContext): IO[Unit] = {
    val query =
      s"""
      INSERT INTO ${schemaName}.waiting_list_table (course_id, student_id, position)
      VALUES (?, ?, ?)
    """
    val params = List(
      SqlParameter("Int", courseID.toString),
      SqlParameter("Int", studentID.toString),
      SqlParameter("Int", position.toString)
    )
    writeDB(query, params).flatMap(_ => IO(logger.info(s"学生已加入Waiting List，位置=${position}")))
  }

  private def addStudentToSelectedCourses(studentID: Int, courseID: Int)(using PlanContext): IO[String] = {
    val query =
      s"""
      INSERT INTO ${schemaName}.course_selection_table (course_id, student_id)
      VALUES (?, ?)
    """
    val params = List(
      SqlParameter("Int", courseID.toString),
      SqlParameter("Int", studentID.toString)
    )
    writeDB(query, params).map(_ => "选课成功！")
  }
}