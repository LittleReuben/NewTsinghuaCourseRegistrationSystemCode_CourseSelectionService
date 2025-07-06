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
import Common.Object.{SqlParameter, ParameterList}
import APIs.UserAuthService.VerifyTokenValidityMessage
import APIs.UserAccountService.QuerySafeUserInfoByTokenMessage
import Objects.UserAccountService.UserRole
import Objects.UserAccountService.SafeUserInfo
import io.circe.Json
import io.circe.parser._

case object CourseSelectionProcess {
  private val logger = LoggerFactory.getLogger(getClass)
  //process plan code 预留标志位，不要删除
  
  def checkCurrentPhase()(using PlanContext): IO[Phase] = {
  // val logger = LoggerFactory.getLogger("checkCurrentPhase")  // 同文后端处理: logger 统一
  
    for {
      // Logging the start of the process
      _ <- IO(logger.info("[checkCurrentPhase] 开始检查当前学期阶段"))
  
      // Fetch SemesterPhase object and read currentPhase field
      query <- IO(s"SELECT current_phase FROM ${schemaName}.semester_phase_table;")
      _ <- IO(logger.info(s"[checkCurrentPhase] 执行查询: ${query}"))
      currentPhaseInt <- readDBInt(query, List())
      _ <- IO(logger.info(s"[checkCurrentPhase] 从数据库中读取到current_phase字段值: ${currentPhaseInt}"))
  
      // Map currentPhaseInt to an appropriate Phase enum
      phase <- IO {
        val phaseEnum = currentPhaseInt match {
          case 1 => Phase.Phase1
          case 2 => Phase.Phase2
          case _ => throw new IllegalArgumentException(s"未知的 current_phase 值: ${currentPhaseInt}")
        }
        logger.info(s"[checkCurrentPhase] 将current_phase转换为Phase枚举值: ${phaseEnum}")
        phaseEnum
      }
    } yield phase
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
  
  def fetchCourseInfoByID(courseID: Int)(using PlanContext): IO[Option[CourseInfo]] = {
    logger.info(s"开始查询课程信息，传入的课程ID为 ${courseID}。")
  
    // 检查课程ID是否有效（非负）
    if (courseID <= 0) {
      logger.warn(s"课程ID ${courseID} 无效，直接返回 None。")
      IO(None)
    } else {
      val courseQuery =
        s"""
  SELECT course_id, course_capacity, time, location, course_group_id, teacher_id
  FROM ${schemaName}.course_table
  WHERE course_id = ?;
           """.stripMargin
  
      logger.info(s"数据库查询SQL生成完成：${courseQuery}")
  
      val queryParameters = IO(List(SqlParameter("Int", courseID.toString)))
  
      for {
        parameters <- queryParameters
        // 执行查询课程的基础信息
        optionalCourseJson <- readDBJsonOptional(courseQuery, parameters)
        _ <- IO(logger.info(s"数据库返回结果是否存在：${optionalCourseJson.isDefined}"))
  
        // 查询 preselectedStudentsSize, selectedStudentsSize, waitingListSize
        preselectedSize <- readDBInt(
          s"SELECT COUNT(*) FROM ${schemaName}.course_preselection_table WHERE course_id = ?;",
          List(SqlParameter("Int", courseID.toString))
        )
        selectedSize <- readDBInt(
          s"SELECT COUNT(*) FROM ${schemaName}.course_selection_table WHERE course_id = ?;",
          List(SqlParameter("Int", courseID.toString))
        )
        waitingListSize <- readDBInt(
          s"SELECT COUNT(*) FROM ${schemaName}.waiting_list_table WHERE course_id = ?;",
          List(SqlParameter("Int", courseID.toString))
        )
  
        // 如果查询结果存在，则解析数据为 CourseInfo
        courseInfo <- IO {
          optionalCourseJson.map { courseJson =>
            logger.info(s"开始解析数据库返回值为 CourseInfo 对象，返回 JSON 为：${courseJson}")
  
            val courseIDValue = decodeField[Int](courseJson, "course_id")
            val courseCapacityValue = decodeField[Int](courseJson, "course_capacity")
            val timeRaw = decodeField[String](courseJson, "time")
            val timeParsed = parse(timeRaw).getOrElse(Json.Null).as[List[Json]].getOrElse(List.empty).map { timeJson =>
              logger.info(s"解析课程时间字段：${timeJson}")
              val dayOfWeekValue = DayOfWeek.fromString(decodeField[String](timeJson, "day_of_week"))
              val timePeriodValue = TimePeriod.fromString(decodeField[String](timeJson, "time_period"))
              CourseTime(dayOfWeekValue, timePeriodValue)
            }
            val locationValue = decodeField[String](courseJson, "location")
            val courseGroupIDValue = decodeField[Int](courseJson, "course_group_id")
            val teacherIDValue = decodeField[Int](courseJson, "teacher_id")
  
            // 构造 CourseInfo 对象
            CourseInfo(
              courseID = courseIDValue,
              courseCapacity = courseCapacityValue,
              time = timeParsed,
              location = locationValue,
              courseGroupID = courseGroupIDValue,
              teacherID = teacherIDValue,
              preselectedStudentsSize = preselectedSize,
              selectedStudentsSize = selectedSize,
              waitingListSize = waitingListSize
            )
          }
        }
      } yield courseInfo
    }
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
          val timestamp = DateTime.now() // 修复错误：在for comprehension外部生成timestamp，避免使用<-赋值
          val logInsertSQL =
            s"""
            INSERT INTO ${schemaName}.system_log (timestamp, user_id, action, details)
            VALUES (?, ?, ?, ?)
            """ // 修复错误：在for comprehension外部生成logInsertSQL，避免使用<-赋值
          val params =
            List(
              SqlParameter("DateTime", timestamp.getMillis.toString),
              SqlParameter("Int", studentID.toString),
              SqlParameter("String", action),
              SqlParameter("String", details)
            ) // 修复错误：在for comprehension外部生成params，避免使用<-赋值
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
            writeDB(insertSelectionQuery, insertSelectionParams).unsafeRunSync()(using cats.effect.unsafe.implicits.global)
  
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
            writeDB(removeWaitingStudentQuery, removeWaitingStudentParams).unsafeRunSync()(using cats.effect.unsafe.implicits.global)
  
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
              writeDBList(updatePositionSQL, updatePositionParams).unsafeRunSync()(using cats.effect.unsafe.implicits.global)
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
  
  // 模型修复的原因: 原代码中使用 unsafeRunSync 方法报错，因为需要 cats.effect.IORuntime 的隐式作用域。修复方式为添加 `import cats.effect.unsafe.implicits.global` 来提供默认的 IORuntime 实现，并保证调用 unsafeRunSync 方法时正常运行。
  
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
        newCourse.map(_.time).getOrElse(List.empty[CourseTime]) // 修复编译错误，防止 None 返回时导致类型不匹配
      }
  
      // 检查选课冲突
      conflictCheckResults <- currentCourseIDs.traverse { cid =>
        fetchCourseInfoByID(cid).map {
          case Some(existingCourse) =>
            val existingCourseTimes = existingCourse.time
            val isConflict = existingCourseTimes.exists(oldTime =>
              newCourseTime.exists(newTime =>
                oldTime.dayOfWeek == newTime.dayOfWeek && // 修复编译错误，确保 newTime 确定为 CourseTime 类型
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
  
  def validateStudentToken(studentToken: String)(using PlanContext): IO[Option[Int]] = {
  // val logger = LoggerFactory.getLogger(getClass)  // 同文后端处理: logger 统一
  
    logger.info(s"开始验证学生Token: ${studentToken}")
  
    for {
      // Step 1: 验证Token有效性
      isValidToken <- VerifyTokenValidityMessage(studentToken).send
      _ <- IO {
        if (!isValidToken)
          logger.error(s"学生Token验证失败: ${studentToken}无效")
        else
          logger.info(s"学生Token验证有效，继续解析学生信息")
      }
  
      // Step 2: 根据Token获取学生信息
      studentInfoOpt <- if (isValidToken) {
        QuerySafeUserInfoByTokenMessage(studentToken).send
      } else {
        IO.pure(None)
      }
  
      // Step 3: 判断角色并获取学生ID
      studentIDOpt = studentInfoOpt.flatMap { userInfo =>
        // 修复逻辑：直接使用Option字段进行校验和获取
        if (userInfo.role == UserRole.Student) Some(userInfo.userID) else None
      }
  
      _ <- IO {
        if (studentInfoOpt.isEmpty)
          logger.error(s"未能根据Token获取学生信息: ${studentToken}")
        else if (studentIDOpt.isEmpty)
          logger.error(s"Token解析用户并非学生角色: ${studentToken}")
        else
          logger.info(s"学生Token验证通过，学生ID: ${studentIDOpt.get}")
      }
    } yield studentIDOpt
  }
  
  def validateTeacherToken(teacherToken: String)(using PlanContext): IO[Option[Int]] = {
  // val logger = LoggerFactory.getLogger("validateTeacherToken")  // 同文后端处理: logger 统一
    
    for {
      // Step 1: 验证 token 的有效性
      _ <- IO(logger.info(s"开始验证传入的教师 token: ${teacherToken}"))
      isTokenValid <- VerifyTokenValidityMessage(teacherToken).send
  
      _ <- IO {
        if (!isTokenValid) logger.warn(s"教师 token 无效或已过期: ${teacherToken}")
      }
  
      // 如果 token 无效，直接返回 None
      result <- if (!isTokenValid) IO.pure(None)
                else {
                  for {
                    // Step 2: 通过 token 查询用户账户的详细信息
                    _ <- IO(logger.info(s"验证有效 token 开始获取用户信息: ${teacherToken}"))
                    userInfoOption <- QuerySafeUserInfoByTokenMessage(teacherToken).send
  
                    _ <- IO(logger.info(s"用户信息查询结果: ${userInfoOption}"))
                    
                    // 如果未找到用户信息，返回 None
                    result <- userInfoOption match {
                      case None =>
                        for {
                          _ <- IO(logger.warn(s"通过 token 获取不到任何用户信息: ${teacherToken}"))
                        } yield None
  
                      case Some(userInfo) =>
                        for {
                          _ <- IO(logger.info(s"解析用户信息, ID: ${userInfo.userID}, 角色: ${userInfo.role}"))
  
                          // 如果角色不是 Teacher，返回 None
                          teacherID <- if (userInfo.role != UserRole.Teacher) {
                            IO(logger.warn(s"用户角色不是教师: ${userInfo.role}, token: ${teacherToken}")) >>
                            IO.pure(None)
                          } else {
                            IO(logger.info(s"用户角色是教师, ID 为: ${userInfo.userID}")) >>
                            IO.pure(Some(userInfo.userID))
                          }
                        } yield teacherID
                    }
                  } yield result
                }
    } yield result
  }
}
