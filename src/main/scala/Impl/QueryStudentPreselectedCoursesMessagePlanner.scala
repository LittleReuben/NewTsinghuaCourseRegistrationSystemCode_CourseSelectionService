package Impl


import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import Objects.CourseManagementService.CourseInfo
import Utils.CourseSelectionProcess._
import Objects.SemesterPhaseService.Phase
import cats.effect.IO
import org.slf4j.LoggerFactory
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
import cats.implicits.*
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
import Utils.CourseSelectionProcess.recordCourseSelectionOperationLog
import Objects.CourseManagementService.CourseInfo
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

case class QueryStudentPreselectedCoursesMessagePlanner(
    studentToken: String,
    override val planContext: PlanContext
) extends Planner[List[CourseInfo]] {
  val logger = LoggerFactory.getLogger(this.getClass.getSimpleName + "_" + planContext.traceID.id)

  override def plan(using planContext: PlanContext): IO[List[CourseInfo]] = {
    for {
      // Step 1: Validate student token and get student ID
      _ <- IO(logger.info(s"[Step 1] 开始验证学生Token: ${studentToken}"))
      studentIDOpt <- validateStudentToken(studentToken)
      studentID <- IO {
        studentIDOpt.getOrElse {
          val errorMessage = "[Step 1.1] 学生Token无效或鉴权失败"
          logger.error(errorMessage)
          throw new IllegalArgumentException(errorMessage)
        }
      }

      // Step 2: Check current semester phase
      _ <- IO(logger.info(s"[Step 2] 开始查询当前学期阶段"))
      currentPhase <- checkCurrentPhase()
      _ <- {
        if (currentPhase != Phase.Phase1) {
          val errorMessage = "[Step 2.1] 当前阶段已抽签完成，不允许查询预选课程"
          IO(logger.error(errorMessage)) *> IO.raiseError(new IllegalStateException(errorMessage))
        } else IO.unit
      }

      // Step 3: Query CoursePreselectionTable to get preselected course IDs
      _ <- IO(logger.info(s"[Step 3] 开始获取学生ID: ${studentID}的预选课程ID"))
      courseIDs <- getPreselectedCourseIDs(studentID)

      // Step 4: Fetch detailed course information by course IDs
      _ <- IO(logger.info(s"[Step 4] 开始获取预选课程的详细信息"))
      courses <- fetchDetailedCourseInfoByCourseIDs(courseIDs)

      _ <- IO(logger.info(s"[Step 5] API流程完成，返回学生的预选课程信息"))
    } yield courses
  }

  private def getPreselectedCourseIDs(studentID: Int)(using PlanContext): IO[List[Int]] = {
    val query =
      s"""
      SELECT course_id
      FROM ${schemaName}.course_preselection_table
      WHERE student_id = ?
      """
    val parameters = List(SqlParameter("Int", studentID.toString))

    for {
      queryResult <- readDBRows(query, parameters)
      courseIDs <- IO {
        queryResult.map(json => decodeField[Int](json, "course_id"))
      }
      _ <- IO(logger.info(s"[getPreselectedCourseIDs] 获取到学生预选课程ID: ${courseIDs.mkString(", ")}"))
    } yield courseIDs
  }

  private def fetchDetailedCourseInfoByCourseIDs(courseIDs: List[Int])(using PlanContext): IO[List[CourseInfo]] = {
    logger.info(s"[fetchDetailedCourseInfoByCourseIDs] 开始查询课程详细信息，共有${courseIDs.length}个课程ID")
    for {
      courseInfos <- courseIDs.traverse(courseID => fetchCourseInfoByID(courseID))
      validCourseInfos <- IO {
        courseInfos.flatten // 过滤掉返回None的课程ID
      }
      _ <- IO(logger.info(s"[fetchDetailedCourseInfoByCourseIDs] 成功查询课程详细信息，共有${validCourseInfos.length}条记录"))
    } yield validCourseInfos
  }
}