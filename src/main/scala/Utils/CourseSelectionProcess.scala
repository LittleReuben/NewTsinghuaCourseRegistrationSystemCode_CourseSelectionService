package Utils

//process plan import 预留标志位，不要删除
import io.circe._
import io.circe.syntax._
import io.circe.generic.auto._
import org.joda.time.DateTime
import Common.DBAPI._
import Common.ServiceUtils.schemaName
import org.slf4j.LoggerFactory
import Common.API.{PlanContext, Planner}
import Common.Object.SqlParameter
import cats.effect.IO
import Objects.SemesterPhaseService.Phase
import Objects.CourseManagementService.CourseTime
import Objects.SystemLogService.SystemLogEntry
import Objects.CourseManagementService.DayOfWeek
import Objects.CourseManagementService.TimePeriod
import Objects.CourseManagementService.CourseInfo
import Objects.SemesterPhaseService.Permissions
import cats.implicits.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}
import Common.API.PlanContext
import cats.implicits._
import Utils.CourseSelectionProcess.recordCourseSelectionOperationLog

case object CourseSelectionProcess {
  private val logger = LoggerFactory.getLogger(getClass)
  //process plan code 预留标志位，不要删除
  
  def checkIsSelectionAllowed()(using PlanContext): IO[Boolean] = {
  // val logger = LoggerFactory.getLogger("checkIsSelectionAllowed")  // 同文后端处理: logger 统一
    logger.info("开始检查当前是否可以进行选课操作")
  
    for {
      // Step 1: 查询当前学期阶段状态
      _ <- IO(logger.info("调用checkCurrentPhase方法以获取当前学期阶段"))
      currentPhase <- checkCurrentPhase()
      _ <- IO(logger.info(s"当前学期阶段为: ${currentPhase}"))
  
      // Step 2: 获取权限信息，并检查是否允许选课
      permissionQuery <- IO {
        s"""
        SELECT allow_student_select
        FROM ${schemaName}.permissions
        WHERE current_phase = ?
        """
      }
      permissionParams <- IO(List(SqlParameter("String", currentPhase.toString)))
      allowStudentSelect <- readDBBoolean(permissionQuery, permissionParams)
      _ <- IO(logger.info(s"权限开关是否允许学生选课: ${allowStudentSelect}"))
  
      // Step 3: 根据权限和阶段状态判断是否允许选课
      selectionAllowed <- IO {
        val isAllowed = allowStudentSelect && (currentPhase == Phase.Phase2)
        logger.info(s"选课操作是否被允许: ${isAllowed}")
        isAllowed
      }
  
      // Step 4: 记录选课检查的相关日志
      _ <- recordCourseSelectionOperationLog(
        studentID = 0, // 假设此为系统操作，使用0代表无具体学生ID
        action = "CHECK_SELECTION_ALLOWED",
        courseID = None,
        details = s"选课检查结果: ${selectionAllowed}"
      ).flatMap(logRecorded =>
        IO(logger.info(s"选课检查日志记录${if (logRecorded) "成功" else "失败"}"))
      )
    } yield selectionAllowed
  }
  
  def removeStudentFromWaitingList(courseID: Int, studentID: Int)(using PlanContext): IO[String] = {
    logger.info(s"开始移除等待列表中的学生操作：课程ID=${courseID}, 学生ID=${studentID}")
  
    val checkCourseQuery = s"SELECT * FROM ${schemaName}.course_table WHERE course_id = ?"
    val checkStudentInWaitingListQuery =
      s"SELECT * FROM ${schemaName}.waiting_list_table WHERE course_id = ? AND student_id = ?"
    val deleteStudentQuery =
      s"DELETE FROM ${schemaName}.waiting_list_table WHERE course_id = ? AND student_id = ?"
  
    for {
      // 验证课程ID是否有效
      validCourse <- readDBJsonOptional(checkCourseQuery, List(SqlParameter("Int", courseID.toString))).map {
        case Some(_) =>
          logger.info(s"课程ID=${courseID}存在，验证通过")
          true
        case None =>
          logger.error(s"课程ID=${courseID}不存在")
          false
      }
  
      // 如果课程无效，直接返回错误
      _ <- if (!validCourse) {
        IO.raiseError(new IllegalArgumentException(s"课程ID=${courseID}无效，请检查输入参数"))
      } else {
        IO.unit
      }
  
      // 验证学生是否在指定课程的WaitingList中
      studentInWaitingList <- readDBJsonOptional(
        checkStudentInWaitingListQuery,
        List(SqlParameter("Int", courseID.toString), SqlParameter("Int", studentID.toString))
      ).map {
        case Some(_) =>
          logger.info(s"验证通过，学生ID=${studentID}在课程ID=${courseID}的等待名单中")
          true
        case None =>
          logger.error(s"学生ID=${studentID}不在课程ID=${courseID}的等待名单中")
          false
      }
  
      // 如果学生不在等待名单中，直接返回错误
      _ <- if (!studentInWaitingList) {
        IO.raiseError(new IllegalArgumentException(s"学生ID=${studentID}不在课程ID=${courseID}的等待名单中，无法移除"))
      } else {
        IO.unit
      }
  
      // 从数据库删除该学生
      deleteResult <- writeDB(
        deleteStudentQuery,
        List(SqlParameter("Int", courseID.toString), SqlParameter("Int", studentID.toString))
      ).flatMap(result =>
        IO {
          logger.info(s"成功移除学生ID=${studentID}从课程ID=${courseID}的等待名单，数据库操作结果：${result}")
        }
      )
  
      // 记录操作日志
      operationDetails = s"学生ID=${studentID} 从课程ID=${courseID} 的等待名单中移除"
      logSuccess <- recordCourseSelectionOperationLog(
        studentID = studentID,
        action = "REMOVE_WAITING_LIST",
        courseID = Some(courseID),
        details = operationDetails
      ).flatMap { logResult =>
        IO {
          if (logResult)
            logger.info(s"成功记录操作日志: ${operationDetails}")
          else
            logger.error(s"记录操作日志失败: ${operationDetails}")
        }
      }
    } yield s"学生ID=${studentID}成功移除课程ID=${courseID}的等待名单"
  }
  
  def checkIsDropAllowed(
    currentPhase: Phase,
    allowStudentDrop: Boolean
  )(using PlanContext): IO[Boolean] = {
  // val logger = LoggerFactory.getLogger("checkIsDropAllowed")  // 同文后端处理: logger 统一
    for {
      _ <- IO(logger.info(s"[checkIsDropAllowed] 开始检查是否允许退课操作"))
      _ <- IO(logger.info(s"[checkIsDropAllowed] 当前阶段为: ${currentPhase}, 退课权限状态: ${allowStudentDrop}"))
  
      // 判断是否可以退课
      isDropAllowed <- IO {
        if (currentPhase == Phase.Phase2 && allowStudentDrop) {
          logger.info("[checkIsDropAllowed] 当前阶段为退课阶段且退课权限已开启，可以退课")
          true
        } else {
          logger.info("[checkIsDropAllowed] 当前阶段非退课阶段或退课权限未开启，不允许退课")
          false
        }
      }
  
      // 记录操作日志
      _ <- recordCourseSelectionOperationLog(
        studentID = 0, // 假设为系统操作，不涉及具体学生，因此设为0
        action = "CheckDropAllowed",
        courseID = None,
        details = s"Checked drop permission state: Current Phase=${currentPhase}, Allow Student Drop=${allowStudentDrop}"
      ).flatMap(logRecorded =>
        IO(logger.info(s"[checkIsDropAllowed] 记录退课权限检查的操作日志，结果: ${if (logRecorded) "成功" else "失败"}"))
      )
    } yield isDropAllowed
  }
  
  def fetchCourseInfoByID(courseID: Int)(using PlanContext): IO[Option[CourseInfo]] = {
    logger.info(s"开始查询课程ID为${courseID}的课程信息")
  
    val sqlQuery =
      s"""
        SELECT course_id, course_capacity, time, location, course_group_id, teacher_id, 
               preselected_students_size, selected_students_size, waiting_list_size
        FROM ${schemaName}.course_table
        WHERE course_id = ?
      """
    val parameters = List(SqlParameter("Int", courseID.toString))
  
    for {
      _ <- IO(logger.info(s"执行数据库查询获取课程信息，查询SQL为：${sqlQuery}"))
      queryResult <- readDBJsonOptional(sqlQuery, parameters)
      courseInfo <- IO {
        queryResult.map { json =>
          val courseID = decodeField[Int](json, "course_id")
          val courseCapacity = decodeField[Int](json, "course_capacity")
          val timeJsonList = decodeField[List[Json]](json, "time")
          val timeList = timeJsonList.map(decodeType[CourseTime])
          val location = decodeField[String](json, "location")
          val courseGroupID = decodeField[Int](json, "course_group_id")
          val teacherID = decodeField[Int](json, "teacher_id")
          val preselectedStudentsSize = decodeField[Int](json, "preselected_students_size")
          val selectedStudentsSize = decodeField[Int](json, "selected_students_size")
          val waitingListSize = decodeField[Int](json, "waiting_list_size")
  
          CourseInfo(
            courseID,
            courseCapacity,
            timeList,
            location,
            courseGroupID,
            teacherID,
            preselectedStudentsSize,
            selectedStudentsSize,
            waitingListSize
          )
        }
      }
      _ <- IO {
        if (queryResult.isEmpty)
          logger.info(s"查询结果为空，课程ID${courseID}不存在")
        else
          logger.info(s"成功获取课程ID为${courseID}的课程信息")
      }
      _ <- recordCourseSelectionOperationLog(
        studentID = 0, // 假设为系统操作，这里用0替代具体学生ID
        action = "QUERY_COURSE",
        courseID = Some(courseID),
        details = if (queryResult.isDefined) s"查询成功" else s"查询失败：课程ID不存在"
      ).flatMap(logRecorded =>
        IO(logger.info(s"查询操作日志记录${if (logRecorded) "成功" else "失败"}"))
      )
    } yield courseInfo
  }
  
  def validateStudentCourseTimeConflict(studentID: Int, courseID: Int)(using PlanContext): IO[Boolean] = {
  
    for {
      // 获取当前阶段
      phase <- readDBString("SELECT phase FROM ${schemaName}.system_config LIMIT 1", List())
  
      // 查询当前学生已选课程的课程ID
      sqlQueryPhase1 <- IO {
        s"""
        SELECT course_id
        FROM ${schemaName}.course_preselection_table
        WHERE student_id = ?
        """
      }
      sqlQueryPhase2 <- IO {
        s"""
        SELECT course_id
        FROM ${schemaName}.course_selection_table
        WHERE student_id = ?
        UNION
        SELECT course_id
        FROM ${schemaName}.waiting_list_table
        WHERE student_id = ?
        """
      }
      studentParam <- IO { SqlParameter("Int", studentID.toString) }
      currentCourses <- if (phase == "1") {
        readDBRows(sqlQueryPhase1, List(studentParam))
      } else {
        readDBRows(sqlQueryPhase2, List(studentParam, studentParam))
      }
  
      // 解码当前已选课程ID列表
      currentCourseIDs <- IO {
        val ids = currentCourses.map(row => decodeField[Int](row, "course_id"))
        logger.info(s"学生ID ${studentID} 当前已选课程列表: ${ids.mkString(", ")}")
        ids
      }
  
      // 获取待检查课程详细信息
      newCourse <- fetchCourseInfoByID(courseID)
      newCourseTime <- IO {
        val courseTime = newCourse.flatMap(_.time)
        if (courseTime.isEmpty) logger.warn(s"待选课程ID ${courseID} 的时间信息为空或无效！")
        courseTime
      }
  
      // 检查选课冲突
      conflictCheckResults <- currentCourseIDs.traverse { cid =>
        fetchCourseInfoByID(cid).map {
          case Some(existingCourse) =>
            val existingCourseTimes = existingCourse.time
            val isConflict = existingCourseTimes.exists(oldTime =>
              newCourseTime.exists(newTime =>
                oldTime.dayOfWeek == newTime.dayOfWeek &&
                  oldTime.timePeriod == newTime.timePeriod
              )
            )
            if (isConflict) {
              logger.info(s"检测到课程冲突，学生ID ${studentID}: 已选课程 ${cid} 与待选课程 ${courseID} 时间重复！")
            }
            isConflict
          case None =>
            logger.warn(s"已选课程ID ${cid} 查询失败，跳过冲突检查")
            false
        }
      }
      isConflict <- IO {
        conflictCheckResults.exists(identity)
      }
  
      // 记录日志
      logDetails <- IO {
        if (isConflict) {
          s"课程冲突: 已选课程列表 ${currentCourseIDs.mkString(", ")} 与待选课程 ${courseID} 存在冲突"
        } else {
          s"无课程冲突: 学生ID ${studentID} 可选择课程 ${courseID}"
        }
      }
      logResult <- recordCourseSelectionOperationLog(
        studentID = studentID,
        action = "CONFLICT_CHECK",
        courseID = Some(courseID),
        details = logDetails
      )
      _ <- IO {
        logger.info(s"课程冲突检查日志记录${if (logResult) "成功" else "失败"}")
      }
    } yield isConflict
  }
  
  def removeStudentFromSelectedCourses(studentID: Int, courseID: Int)(using PlanContext): IO[String] = {
    logger.info(s"开始处理从课程列表中移除学生，studentID: ${studentID}, courseID: ${courseID}")
  
    for {
      // Step 1: 验证输入参数的有效性
      _ <- IO {
        if (studentID == 0 || courseID == 0) {
          logger.error(s"无效的输入参数，studentID: ${studentID}, courseID: ${courseID}")
          throw new IllegalArgumentException("无效的输入参数")
        }
      }
      _ <- IO(logger.info(s"验证输入参数非空通过，studentID: ${studentID}, courseID: ${courseID}"))
      courseInfoOpt <- fetchCourseInfoByID(courseID)
      _ <- IO {
        if (courseInfoOpt.isEmpty) {
          logger.error(s"课程ID: ${courseID}不存在")
          throw new IllegalArgumentException(s"课程ID: ${courseID}不存在")
        }
      }
  
      // Step 2: 从选上记录中移除该学生
      _ <- IO(logger.info(s"开始尝试移除学生的选上记录，studentID: ${studentID}, courseID: ${courseID}"))
      removeQuery <- IO {
        s"""
  DELETE FROM ${schemaName}.course_selection_table
  WHERE course_id = ? AND student_id = ?
  """.stripMargin
      }
      removeParams <- IO {
        List(
          SqlParameter("Int", courseID.toString),
          SqlParameter("Int", studentID.toString)
        )
      }
      removalResult <- writeDB(removeQuery, removeParams)
      _ <- IO(logger.info(s"从选上记录中移除学生完成，结果: ${removalResult}"))
  
      // Step 3: 查询并调整 Waiting List
      waitingListQuery <- IO {
        s"""
  SELECT student_id, position
  FROM ${schemaName}.waiting_list_table
  WHERE course_id = ?
  ORDER BY position ASC
  """.stripMargin
      }
      waitingListParams <- IO {
        List(SqlParameter("Int", courseID.toString))
      }
      waitingList <- readDBRows(waitingListQuery, waitingListParams)
  
      _ <- if (waitingList.nonEmpty) {
        IO(logger.info(s"Waiting List 存在记录，即将调整，记录数: ${waitingList.size}")) >>
          IO {
            val firstStudentID = decodeField[Int](waitingList.head, "student_id")
            logger.info(s"第一个等待学生 ID: ${firstStudentID}")
  
            // Step 3.2: 将 Waiting List 第一名学生移至选上名单
            val insertSelectionQuery =
              s"""
  INSERT INTO ${schemaName}.course_selection_table (course_id, student_id)
  VALUES (?, ?)
  """.stripMargin
            val insertSelectionParams = List(
              SqlParameter("Int", courseID.toString),
              SqlParameter("Int", firstStudentID.toString)
            )
            writeDB(insertSelectionQuery, insertSelectionParams).unsafeRunSync()
  
            // Step 3.3: 从 Waiting List 移除第一名学生
            val removeWaitingStudentQuery =
              s"""
  DELETE FROM ${schemaName}.waiting_list_table
  WHERE course_id = ? AND student_id = ?
  """.stripMargin
            val removeWaitingStudentParams = List(
              SqlParameter("Int", courseID.toString),
              SqlParameter("Int", firstStudentID.toString)
            )
            writeDB(removeWaitingStudentQuery, removeWaitingStudentParams).unsafeRunSync()
  
            // 更新剩余学生的 position
            val updatePositionParams: List[ParameterList] = waitingList.tail.zipWithIndex.map { case (record, idx) =>
              val studentID = decodeField[Int](record, "student_id")
              ParameterList(
                List(
                  SqlParameter("Int", (idx + 1).toString), // 新 position，从 1 开始
                  SqlParameter("Int", courseID.toString),
                  SqlParameter("Int", studentID.toString)
                )
              )
            }
  
            val updatePositionSQL =
              s"""
  UPDATE ${schemaName}.waiting_list_table
  SET position = ?
  WHERE course_id = ? AND student_id = ?
  """.stripMargin
            if (updatePositionParams.nonEmpty) {
              writeDBList(updatePositionSQL, updatePositionParams).unsafeRunSync()
            }
          }
      } else {
        IO(logger.info(s"Waiting List 为空，无需调整"))
      }
  
      // Step 4: 记录操作日志
      logResult <- recordCourseSelectionOperationLog(
        studentID = studentID,
        action = "REMOVE_COURSE_SELECTION",
        courseID = Some(courseID),
        details = s"学生被从选上名单中移除"
      )
      _ <- IO(logger.info(s"操作日志记录结果: ${if (logResult) "成功" else "失败"}"))
  
    } yield s"操作成功：学生ID ${studentID} 从课程ID ${courseID} 的选上列表中移除"
  }
  
  def checkCurrentPhase()(using PlanContext): IO[Phase] = {
  // val logger = LoggerFactory.getLogger("checkCurrentPhase")  // 同文后端处理: logger 统一
    
    for {
      _ <- IO(logger.info("[checkCurrentPhase] 开始检查当前学期阶段"))
  
      // Fetch SemesterPhase object and read currentPhase field
      query <- IO(s"SELECT current_phase FROM ${schemaName}.semester_phase;")
      _ <- IO(logger.info(s"[checkCurrentPhase] 执行查询: ${query}"))
      json <- readDBJson(query, List())
  
      currentPhaseString <- IO {
        val phase = decodeField[String](json, "current_phase")
        logger.info(s"[checkCurrentPhase] 从数据库中读取到current_phase字段值: ${phase}")
        phase
      }
  
      // Convert currentPhase string to Phase enum
      phase <- IO {
        val phaseEnum = Phase.fromString(currentPhaseString)
        logger.info(s"[checkCurrentPhase] 将current_phase转换为 Phase 枚举值: ${phaseEnum}")
        phaseEnum
      }
  
      // Log the operation using SystemLogEntry
      _ <- {
        val logEntry = SystemLogEntry(
          logID = 0, // Placeholder as we don't generate logID here
          timestamp = DateTime.now,
          userID = 0, // Placeholder as no userID context is provided
          action = "CheckCurrentPhase",
          details = s"检查当前学期阶段, 当前阶段: ${phase.toString}"
        )
        IO(logger.info(s"[checkCurrentPhase] 创建 SystemLogEntry: ${logEntry.toString}"))>>
        IO.unit // Placeholder to indicate logging action; actual persistence is not implemented here
      }
      
    } yield phase
  }
  
  
  def validateTeacherToken(teacherToken: String)(using PlanContext): IO[Option[Int]] = {
  // val logger = LoggerFactory.getLogger("validateTeacherToken")  // 同文后端处理: logger 统一
  
    for {
      // Step 1: Decode the token
      _ <- IO(logger.info(s"开始解析老师Token: ${teacherToken}"))
      decodedTeacherIdOpt <- parseTeacherToken(teacherToken).send // 调用解析工具，返回的可能是 Option[Int]
  
      // Step 1.2: Log and handle invalid token
      result <- decodedTeacherIdOpt match {
        case Some(teacherID) =>
          // Step 2: Log authentication success and record the operation log
          for {
            _ <- IO(logger.info(s"Token解析成功，TeacherID: ${teacherID}"))
            _ <- recordTeacherAuthLog(teacherID).send
            _ <- IO(logger.info(s"Teacher鉴权日志记录成功，TeacherID: ${teacherID}"))
          } yield Some(teacherID)
        case None =>
          // Step 1.2: Record failure and return None
          IO {
            logger.warn(s"Token解析失败或无效，返回None")
            None
          }
      }
    } yield result
  }
  
  def validateStudentToken(studentToken: String)(using PlanContext): IO[Option[Int]] = {
    logger.info(s"开始验证学生Token: ${studentToken}")
  
    val query =
      s"""
      SELECT student_id
      FROM ${schemaName}.student_tokens
      WHERE token = ? AND is_valid = true
      """
    val parameters = List(SqlParameter("String", studentToken))
  
    for {
      // Step 1: 查询数据库验证Token
      tokenValidationResult <- readDBJsonOptional(query, parameters)
      studentIDOpt <- IO {
        tokenValidationResult.map(json => decodeField[Int](json, "student_id"))
      }
  
      _ <- IO {
        if (studentIDOpt.isEmpty)
          logger.error(s"学生Token验证失败: ${studentToken}无效或不存在")
        else
          logger.info(s"学生Token验证成功，解析得到学生ID: ${studentIDOpt.get}")
      }
  
      // Step 2: 若验证失败，记录日志
      logResult <- studentIDOpt match {
        case None =>
          recordCourseSelectionOperationLog(
            studentID = 0, // 学生尚未验证成功，无具体ID，此处用0表示
            action = "AUTHENTICATION_FAILED",
            courseID = None,
            details = s"学生鉴权失败，Token: ${studentToken}"
          ).flatMap(logRecorded =>
            IO(logger.info(s"记录鉴权失败日志${if (logRecorded) "成功" else "失败"}"))
          )
        case Some(_) =>
          IO(logger.info(s"学生Token验证通过，无需记录鉴权失败日志"))
      }
    } yield studentIDOpt
  }
  
  def recordCourseSelectionOperationLog(
    studentID: Int,
    action: String,
    courseID: Option[Int],
    details: String
  )(using PlanContext): IO[Boolean] = {
    logger.info(s"开始记录选课操作日志，学生ID: ${studentID}, 动作: ${action}, 课程ID: ${courseID.getOrElse("无")}, 详情: ${details}")
  
    for {
      // 验证studentID是否有效
      studentCheckQuery <- IO {
        s"""
        SELECT student_id
        FROM ${schemaName}.student
        WHERE student_id = ?
        """
      }
      isValidStudent <- readDBJsonOptional(studentCheckQuery, List(SqlParameter("Int", studentID.toString))).map {
        case Some(_) =>
          logger.info(s"验证成功，学生ID: ${studentID}存在于系统中")
          true
        case None =>
          logger.error(s"验证失败，学生ID: ${studentID}不存在")
          false
      }
  
      // 验证action是否符合选课操作范围
      validActions <- IO { Set("选课", "退课", "预选") }
      isValidAction <- IO { validActions.contains(action) }
      _ <- IO {
        if (!isValidAction)
          logger.error(s"验证失败，动作: ${action}不符合选课操作范围")
      }
  
      // 如果提供了课程ID，验证该课程是否有效
      validCourseInfo <- courseID match {
        case Some(id) =>
          fetchCourseInfoByID(id).map {
            case Some(info) =>
              logger.info(s"验证成功，课程ID: ${id}有效，课程信息: ${info}")
              true
            case None =>
              logger.error(s"验证失败，课程ID: ${id}不存在")
              false
          }
        case None =>
          IO(logger.info("课程ID未提供，跳过验证")).as(true)
      }
  
      // 构造日志记录对象并保存至数据库
      logRecorded <- {
        val isValidLog = isValidStudent && isValidAction && validCourseInfo
        if (isValidLog) {
          val timestamp <- IO { DateTime.now() }
          val logInsertSQL <- IO {
            s"""
            INSERT INTO ${schemaName}.system_log (timestamp, user_id, action, details)
            VALUES (?, ?, ?, ?)
            """
          }
          val params <- IO {
            List(
              SqlParameter("DateTime", timestamp.getMillis.toString),
              SqlParameter("Int", studentID.toString),
              SqlParameter("String", action),
              SqlParameter("String", details)
            )
          }
          writeDB(logInsertSQL, params).map(_ => {
            logger.info("选课操作日志记录成功")
            true
          })
        } else {
          IO {
            logger.error("选课操作日志记录失败，原因: 参数验证未通过")
            false
          }
        }
      }
    } yield logRecorded
  }
}
