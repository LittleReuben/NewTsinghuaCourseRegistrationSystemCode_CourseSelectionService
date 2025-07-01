package Impl


import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import Objects.CourseManagementService.{CourseInfo, CourseTime, DayOfWeek, TimePeriod}
import Objects.SemesterPhaseService.{Permissions, Phase}
import Objects.SystemLogService.SystemLogEntry
import Utils.CourseSelectionProcess._
import cats.effect.IO
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
import org.slf4j.LoggerFactory
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
import Objects.SemesterPhaseService.Phase
import Utils.CourseSelectionProcess.checkCurrentPhase
import Objects.CourseManagementService.CourseTime
import Utils.CourseSelectionProcess.validateStudentCourseTimeConflict
import Utils.CourseSelectionProcess.validateStudentToken
import Utils.CourseSelectionProcess.recordCourseSelectionOperationLog
import Objects.CourseManagementService.TimePeriod
import Utils.CourseSelectionProcess.checkIsSelectionAllowed
import Utils.CourseSelectionProcess.fetchCourseInfoByID
import Utils.CourseSelectionProcess.removeStudentFromWaitingList
import Utils.CourseSelectionProcess.removeStudentFromSelectedCourses
import Objects.CourseManagementService.DayOfWeek
import Objects.CourseManagementService.CourseInfo
import Objects.SemesterPhaseService.Permissions
import Objects.CourseManagementService._
import Objects.SemesterPhaseService._
import Objects.SystemLogService._
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
      // Step 1: 验证学生Token
      _ <- IO(logger.info("步骤1: 验证学生Token"))
      studentIDOpt <- validateStudentToken(studentToken)
      studentID <- IO {
        studentIDOpt.getOrElse({
          logger.error("Token验证失败")
          throw new IllegalArgumentException("Token验证失败")
        })
      }
      _ <- IO(logger.info(s"Token验证成功，学生ID为: ${studentID}"))

      // Step 2: 验证课程是否存在
      _ <- IO(logger.info("步骤2: 验证课程是否存在"))
      courseInfoOpt <- fetchCourseInfoByID(courseID)
      courseInfo <- IO {
        courseInfoOpt.getOrElse({
          logger.error("课程不存在!")
          throw new IllegalArgumentException("课程不存在!")
        })
      }
      _ <- IO(logger.info(s"课程信息验证成功，课程ID为: ${courseInfo.courseID}"))

      // Step 3: 验证当前阶段和正选权限
      _ <- IO(logger.info("步骤3: 验证当前阶段和正选权限"))
      currentPhase <- checkCurrentPhase()
      _ <- IO {
        if (currentPhase != Phase.Phase2) {
          logger.error("当前阶段不允许选课")
          throw new IllegalStateException("当前阶段不允许选课")
        }
      }
      selectionAllowed <- checkIsSelectionAllowed()
      _ <- IO {
        if (!selectionAllowed) {
          logger.error("选课权限未开启")
          throw new IllegalStateException("选课权限未开启")
        }
      }
      _ <- IO(logger.info("阶段和权限验证成功"))

      // Step 4: 检查课程是否已选以及时间冲突
      _ <- IO(logger.info("步骤4: 检查课程是否已选以及时间冲突"))
      isAlreadySelected <- {
        val sql =
          s"""
            SELECT COUNT(*) > 0 
            FROM ${schemaName}.course_selection_table 
            WHERE student_id = ? AND course_id = ?
            UNION ALL
            SELECT COUNT(*) > 0 
            FROM ${schemaName}.waiting_list_table 
            WHERE student_id = ? AND course_id = ?
          """
        val params = List(
          SqlParameter("Int", studentID.toString),
          SqlParameter("Int", courseID.toString),
          SqlParameter("Int", studentID.toString),
          SqlParameter("Int", courseID.toString)
        )
        readDBBoolean(sql, params)
      }
      _ <- IO {
        if (isAlreadySelected) {
          logger.error("该门课程已选!")
          throw new IllegalStateException("该门课程已选!")
        }
      }
      isTimeConflict <- validateStudentCourseTimeConflict(studentID, courseID)
      _ <- IO {
        if (isTimeConflict) {
          logger.error("时间冲突，无法选课")
          throw new IllegalStateException("时间冲突，无法选课")
        }
      }
      _ <- IO(logger.info("课程尚未选且无时间冲突"))

      // Step 5: 根据课程容量处理选课逻辑
      _ <- IO(logger.info("步骤5: 处理选课逻辑"))
      _ <- if (courseInfo.selectedStudentsSize >= courseInfo.courseCapacity) {
        // 满员，加入Waiting List
        val addWaitingListQuery =
          s"""
            INSERT INTO ${schemaName}.waiting_list_table (course_id, student_id, position)
            VALUES (?, ?, ?)
          """
        val waitingListParams = List(
          SqlParameter("Int", courseID.toString),
          SqlParameter("Int", studentID.toString),
          SqlParameter("Int", (courseInfo.waitingListSize + 1).toString)
        )
        writeDB(addWaitingListQuery, waitingListParams).flatMap(_ =>
          IO(logger.info("课程已满员，学生已加入等待名单"))
        )
      } else {
        // 未满员，加入选课名单
        val addSelectionQuery =
          s"""
            INSERT INTO ${schemaName}.course_selection_table (course_id, student_id)
            VALUES (?, ?)
          """
        val selectionParams = List(
          SqlParameter("Int", courseID.toString),
          SqlParameter("Int", studentID.toString)
        )
        writeDB(addSelectionQuery, selectionParams).flatMap(_ =>
          IO(logger.info("课程未满员，学生已选上课程"))
        )
      }

      // Step 6: 记录选课操作日志
      _ <- IO(logger.info("步骤6: 记录选课操作日志"))
      logDetails = s"学生ID ${studentID} 选课成功，课程ID为 ${courseID}"
      logResult <- recordCourseSelectionOperationLog(
        studentID = studentID,
        action = "SELECT_COURSE",
        courseID = Some(courseID),
        details = logDetails
      )
      _ <- IO {
        if (!logResult) {
          logger.warn("选课操作日志记录失败")
        }
      }

      // Step 7: 返回选课成功结果消息
      _ <- IO(logger.info("步骤7: 选课成功"))
    } yield "选课成功！"
  }
}