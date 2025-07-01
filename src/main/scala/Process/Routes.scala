package Process

import Common.API.PlanContext
import Common.DBAPI.DidRollbackException
import Impl.*
import cats.effect.*
import fs2.concurrent.Topic
import io.circe.*
import io.circe.derivation.Configuration
import io.circe.generic.auto.*
import io.circe.parser.decode
import io.circe.syntax.*
import org.http4s.*
import org.http4s.client.Client
import org.http4s.dsl.io.*
import org.http4s.circe.*
import scala.collection.concurrent.TrieMap
import Common.API.TraceID


object Routes:
  val projects: TrieMap[String, Topic[IO, String]] = TrieMap.empty

  private def executePlan(messageType: String, str: String): IO[String] =
    messageType match {
      case _ =>
        IO.raiseError(new Exception(s"Unknown type: $messageType"))
    }
  def handlePostRequest(req: Request[IO]): IO[String] = {
    req.as[Json].map {
      bodyJson => {
        val planContext = PlanContext(TraceID("default"), transactionLevel = 0)
        val planContextJson = planContext.asJson
        val updatedJson = bodyJson.deepMerge(Json.obj("planContext" -> planContextJson))
        updatedJson.toString
      }
    }
  }
  val service: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "stream" / projectName =>
      projects.get(projectName) match {
        case Some(topic) =>
          val stream = topic.subscribe(10)
          Ok(stream)
        case None =>
          Topic[IO, String].flatMap { topic =>
            projects.putIfAbsent(projectName, topic) match {
              case None =>
                val stream = topic.subscribe(10)
                Ok(stream)
              case Some(existingTopic) =>
                val stream = existingTopic.subscribe(10)
                Ok(stream)
            }
          }
      }
    case req@POST -> Root / "api" / name =>
      handlePostRequest(req).flatMap {
          executePlan(name, _)
        }.flatMap(Ok(_))
        .handleErrorWith {
          case e: DidRollbackException =>
            println(s"Rollback error: $e")
            val headers = Headers("X-DidRollback" -> "true")
            BadRequest(e.getMessage.asJson.toString).map(_.withHeaders(headers))

          case e: Throwable =>
            println(s"General error: $e")
            BadRequest(e.getMessage.asJson.toString)
        }
  }