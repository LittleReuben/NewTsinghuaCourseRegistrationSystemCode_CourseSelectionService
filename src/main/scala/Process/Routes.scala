
package Process

import Common.API.PlanContext
import Common.DBAPI.DidRollbackException
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
import scala.collection.concurrent.TrieMap
import Common.Serialize.CustomColumnTypes.*
import Impl.QueryStudentSelectedCoursesMessagePlanner
import Impl.QueryCourseWaitingListDataMessagePlanner
import Impl.QueryStudentWaitingListStatusMessagePlanner
import Impl.RemovePreselectedCourseMessagePlanner
import Impl.PreselectCourseMessagePlanner
import Impl.CheckStudentHasSuccessfullyTakenCourseMessagePlanner
import Impl.DropCourseMessagePlanner
import Impl.QueryCourseSelectionDataMessagePlanner
import Impl.QueryCoursePreselectionDataMessagePlanner
import Impl.SelectCourseMessagePlanner
import Impl.QueryStudentPreselectedCoursesMessagePlanner
import Common.API.TraceID
import org.joda.time.DateTime
import org.http4s.circe.*
import java.util.UUID
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

object Routes:
  val projects: TrieMap[String, Topic[IO, String]] = TrieMap.empty

  private def executePlan(messageType: String, str: String): IO[String] =
    messageType match {
      case "QueryStudentSelectedCoursesMessage" =>
        IO(
          decode[QueryStudentSelectedCoursesMessagePlanner](str) match
            case Left(err) => err.printStackTrace(); throw new Exception(s"Invalid JSON for QueryStudentSelectedCoursesMessage[${err.getMessage}]")
            case Right(value) => value.fullPlan.map(_.asJson.toString)
        ).flatten
       
      case "QueryCourseWaitingListDataMessage" =>
        IO(
          decode[QueryCourseWaitingListDataMessagePlanner](str) match
            case Left(err) => err.printStackTrace(); throw new Exception(s"Invalid JSON for QueryCourseWaitingListDataMessage[${err.getMessage}]")
            case Right(value) => value.fullPlan.map(_.asJson.toString)
        ).flatten
       
      case "QueryStudentWaitingListStatusMessage" =>
        IO(
          decode[QueryStudentWaitingListStatusMessagePlanner](str) match
            case Left(err) => err.printStackTrace(); throw new Exception(s"Invalid JSON for QueryStudentWaitingListStatusMessage[${err.getMessage}]")
            case Right(value) => value.fullPlan.map(_.asJson.toString)
        ).flatten
       
      case "RemovePreselectedCourseMessage" =>
        IO(
          decode[RemovePreselectedCourseMessagePlanner](str) match
            case Left(err) => err.printStackTrace(); throw new Exception(s"Invalid JSON for RemovePreselectedCourseMessage[${err.getMessage}]")
            case Right(value) => value.fullPlan.map(_.asJson.toString)
        ).flatten
       
      case "PreselectCourseMessage" =>
        IO(
          decode[PreselectCourseMessagePlanner](str) match
            case Left(err) => err.printStackTrace(); throw new Exception(s"Invalid JSON for PreselectCourseMessage[${err.getMessage}]")
            case Right(value) => value.fullPlan.map(_.asJson.toString)
        ).flatten
       
      case "CheckStudentHasSuccessfullyTakenCourseMessage" =>
        IO(
          decode[CheckStudentHasSuccessfullyTakenCourseMessagePlanner](str) match
            case Left(err) => err.printStackTrace(); throw new Exception(s"Invalid JSON for CheckStudentHasSuccessfullyTakenCourseMessage[${err.getMessage}]")
            case Right(value) => value.fullPlan.map(_.asJson.toString)
        ).flatten
       
      case "DropCourseMessage" =>
        IO(
          decode[DropCourseMessagePlanner](str) match
            case Left(err) => err.printStackTrace(); throw new Exception(s"Invalid JSON for DropCourseMessage[${err.getMessage}]")
            case Right(value) => value.fullPlan.map(_.asJson.toString)
        ).flatten
       
      case "QueryCourseSelectionDataMessage" =>
        IO(
          decode[QueryCourseSelectionDataMessagePlanner](str) match
            case Left(err) => err.printStackTrace(); throw new Exception(s"Invalid JSON for QueryCourseSelectionDataMessage[${err.getMessage}]")
            case Right(value) => value.fullPlan.map(_.asJson.toString)
        ).flatten
       
      case "QueryCoursePreselectionDataMessage" =>
        IO(
          decode[QueryCoursePreselectionDataMessagePlanner](str) match
            case Left(err) => err.printStackTrace(); throw new Exception(s"Invalid JSON for QueryCoursePreselectionDataMessage[${err.getMessage}]")
            case Right(value) => value.fullPlan.map(_.asJson.toString)
        ).flatten
       
      case "SelectCourseMessage" =>
        IO(
          decode[SelectCourseMessagePlanner](str) match
            case Left(err) => err.printStackTrace(); throw new Exception(s"Invalid JSON for SelectCourseMessage[${err.getMessage}]")
            case Right(value) => value.fullPlan.map(_.asJson.toString)
        ).flatten
       
      case "QueryStudentPreselectedCoursesMessage" =>
        IO(
          decode[QueryStudentPreselectedCoursesMessagePlanner](str) match
            case Left(err) => err.printStackTrace(); throw new Exception(s"Invalid JSON for QueryStudentPreselectedCoursesMessage[${err.getMessage}]")
            case Right(value) => value.fullPlan.map(_.asJson.toString)
        ).flatten
       

      case "test" =>
        for {
          output  <- Utils.Test.test(str)(using  PlanContext(TraceID(""), 0))
        } yield output
      case _ =>
        IO.raiseError(new Exception(s"Unknown type: $messageType"))
    }

  def handlePostRequest(req: Request[IO]): IO[String] = {
    req.as[Json].map { bodyJson =>
      val hasPlanContext = bodyJson.hcursor.downField("planContext").succeeded

      val updatedJson = if (hasPlanContext) {
        bodyJson
      } else {
        val planContext = PlanContext(TraceID(UUID.randomUUID().toString), transactionLevel = 0)
        val planContextJson = planContext.asJson
        bodyJson.deepMerge(Json.obj("planContext" -> planContextJson))
      }
      updatedJson.toString
    }
  }
  val service: HttpRoutes[IO] = HttpRoutes.of[IO] {
    case GET -> Root / "health" =>
      Ok("OK")
      
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
  