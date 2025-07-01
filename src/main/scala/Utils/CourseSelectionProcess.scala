import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.slf4j.LoggerFactory
import org.joda.time.DateTime

def CourseSelectionProcess()(using PlanContext): IO[String] = {
  val logger = LoggerFactory.getLogger(getClass)
  logger.info("[CourseSelectionProcess] 开始处理选课逻辑")
  
  // 定义 SQL 查询语句，用于获取开放注册的课程信息
  val fetchCoursesSQL = s"""
    SELECT course_id, course_name
    FROM ${schemaName}.courses
    WHERE registration_open = true;
  """
  logger.info(s"[CourseSelectionProcess] 准备执行获取可选课程列表的 SQL: ${fetchCoursesSQL}")
  
  for {
    // 执行 SQL 查询以获取开放选课的课程列表
    courseRows <- readDBRows(fetchCoursesSQL, List.empty)
    // 将查询到的行数据转换为课程信息列表
    courseList <- IO {
      logger.info(s"[CourseSelectionProcess] 获取到的课程信息有 ${courseRows.size} 条")
      courseRows.map { row =>
        val courseId = decodeField[Int](row, "course_id")
        val courseName = decodeField[String](row, "course_name")
        logger.debug(s"[CourseSelectionProcess] 课程详情 - ID: ${courseId}, 名称: ${courseName}")
        (courseId, courseName)
      }
    }

    // 根据课程信息列表的大小来决定返回值
    result <- if (courseList.isEmpty) {
      val message = "[CourseSelectionProcess] 当前没有开放的课程"
      logger.info(message)
      IO.pure(message)
    } else {
      // 构造课程列表信息并返回给调用者
      val availableCourses = courseList.map { case (id, name) => s"课程 ID: ${id}, 名称: ${name}" }.mkString("\n")
      val message = s"[CourseSelectionProcess] 可供选择的课程有:\n${availableCourses}"
      logger.info(message)
      IO.pure(message)
    }
  } yield result
}