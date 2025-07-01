import Common.API.{PlanContext}
import Common.DBAPI.{readDBRows, writeDB}
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import org.slf4j.LoggerFactory
import org.joda.time.DateTime
import cats.effect.IO

def CourseSelectionProcess()(using PlanContext): IO[Unit] = {
  // Logger instance
  val logger = LoggerFactory.getLogger("CourseSelectionProcess")

  logger.info("[CourseSelectionProcess] 开始执行选课流程")

  // Step 1: 查询课程表，获取所有课程
  val courseFetchSQL =
    s"""
      SELECT course_id, course_name, capacity, enrolled_students
      FROM ${schemaName}.course
    """
  for {
    _ <- IO(logger.info(s"[Step 1] 执行查询所有课程的 SQL：${courseFetchSQL}"))
    courses <- readDBRows(courseFetchSQL, List())

    // Step 2: 检查课程容量并打印信息
    _ <- IO {
      courses.foreach { course =>
        val courseID = decodeField[Int](course, "course_id")
        val courseName = decodeField[String](course, "course_name")
        val capacity = decodeField[Int](course, "capacity")
        val enrolledStudents = decodeField[Int](course, "enrolled_students")

        if (enrolledStudents >= capacity) {
          logger.info(s"[Step 2] 课程 ${courseName} (ID: ${courseID}) 已满员，当前报名人数: ${enrolledStudents}，容量: ${capacity}")
        } else {
          logger.info(s"[Step 2] 课程 ${courseName} (ID: ${courseID}) 有空余名额，当前报名人数: ${enrolledStudents}，容量: ${capacity}")
        }
      }
    }

    // Step 3: 清理过期数据（为保证流程正确性，假定有一个逻辑清理历史选课记录）
    val cleanupSQL =
      s"DELETE FROM ${schemaName}.course_selection_history WHERE date < ?"
    val cleanupDate = DateTime.now.minusDays(30).getMillis.toString
    _ <- IO(logger.info(s"[Step 3] 执行清理历史选课记录 SQL：${cleanupSQL}，日期参数为: ${cleanupDate}"))
    _ <- writeDB(cleanupSQL, List(SqlParameter("Long", cleanupDate)))

    _ <- IO(logger.info("[Step 3] 历史选课记录清理完成"))

    _ <- IO(logger.info("[CourseSelectionProcess] 选课流程执行完毕"))
  } yield ()
}