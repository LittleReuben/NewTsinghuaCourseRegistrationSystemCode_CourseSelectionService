
package Process

import Common.API.{API, PlanContext, TraceID}
import Common.DBAPI.{initSchema, writeDB}
import Common.ServiceUtils.schemaName
import Global.ServerConfig
import cats.effect.IO
import io.circe.generic.auto.*
import java.util.UUID
import Global.DBConfig
import Process.ProcessUtils.server2DB
import Global.GlobalVariables

object Init {
  def init(config: ServerConfig): IO[Unit] = {
    given PlanContext = PlanContext(traceID = TraceID(UUID.randomUUID().toString), 0)
    given DBConfig = server2DB(config)

    val program: IO[Unit] = for {
      _ <- IO(GlobalVariables.isTest=config.isTest)
      _ <- API.init(config.maximumClientConnection)
      _ <- Common.DBAPI.SwitchDataSourceMessage(projectName = Global.ServiceCenter.projectName).send
      _ <- initSchema(schemaName)
            /** 课程参与历史表，记录学生参与课程的历史
       * course_id: 课程ID，关联到CourseTable中的course_id
       * student_id: 学生ID
       */
      _ <- writeDB(
        s"""
        CREATE TABLE IF NOT EXISTS "${schemaName}"."course_participation_history_table" (
            course_id INT NOT NULL,
            student_id INT NOT NULL
        );
         
        """,
        List()
      )
      /** 存储课程预选阶段的课程和学生关系
       * course_id: 课程ID，关联到CourseTable中的course_id
       * student_id: 学生ID
       */
      _ <- writeDB(
        s"""
        CREATE TABLE IF NOT EXISTS "${schemaName}"."course_preselection_table" (
            course_id INT NOT NULL,
            student_id INT NOT NULL
        );
         
        """,
        List()
      )
      /** 课程选择表，记录学生选择的课程信息
       * course_id: 课程ID，关联到CourseTable中的course_id
       * student_id: 学生ID
       */
      _ <- writeDB(
        s"""
        CREATE TABLE IF NOT EXISTS "${schemaName}"."course_selection_table" (
            course_id INT NOT NULL,
            student_id INT NOT NULL
        );
         
        """,
        List()
      )
      /** 学生等待队列表，记录学生在等待队列中的位置
       * course_id: 课程ID，关联到CourseTable中的course_id
       * student_id: 学生ID
       * position: 学生在等待队列中的位置
       */
      _ <- writeDB(
        s"""
        CREATE TABLE IF NOT EXISTS "${schemaName}"."waiting_list_table" (
            course_id INT NOT NULL,
            student_id INT NOT NULL,
            position INT NOT NULL
        );
         
        """,
        List()
      )
    } yield ()

    program.handleErrorWith(err => IO {
      println("[Error] Process.Init.init 失败, 请检查 db-manager 是否启动及端口问题")
      err.printStackTrace()
    })
  }
}
    