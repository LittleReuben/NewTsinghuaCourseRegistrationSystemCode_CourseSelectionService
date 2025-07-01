package Process

import Common.API.{API, PlanContext, TraceID}
import Common.DBAPI.{initSchema, writeDB}
import Common.ServiceUtils.schemaName
import Global.ServerConfig
import cats.effect.IO
import io.circe.generic.auto.*
import java.util.UUID
import Process.ProcessUtils.server2DB
import Common.DBUtils.initDatabase

object Init {
  def init(config:ServerConfig): IO[Unit] ={
    given PlanContext = PlanContext(traceID = TraceID(UUID.randomUUID().toString), 0)
    given DBConfig = server2DB(config)

    GlobalVariables.isTest=config.isTest
    val program: IO[Unit] = for {
      _ <- DBUtils.initDatabase()
      _ <- initSchema(schemaName)
      _ <- API.init(config.maximumClientConnection)
      _ <- Common.DBAPI.SwitchDataSourceMessage(projectName = Global.ServiceCenter.projectName).send
    } yield ()
    program.handleErrorWith(err => IO{
      println("[Error] Process.Init.init 失败, 请检查 db-manager 是否启动及端口问题")
    })
  }
}
