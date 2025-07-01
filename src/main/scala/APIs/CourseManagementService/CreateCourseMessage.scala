package APIs.CourseManagementService

import Common.API.API
import Global.ServiceCenter.CourseManagementServiceCode

import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.*
import io.circe.parser.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}

import com.fasterxml.jackson.core.`type`.TypeReference
import Common.Serialize.JacksonSerializeUtils

import scala.util.Try

import org.joda.time.DateTime
import java.util.UUID

import Objects.CourseManagementService.CourseTime
import Objects.CourseManagementService.CourseInfo
import Objects.CourseManagementService.DayOfWeek
import Objects.CourseManagementService.TimePeriod

import Common.API.{PlanContext, Planner}
import Common.DBAPI._
import Common.Object.ParameterList
import Common.Object.SqlParameter
import Common.ServiceUtils.schemaName
import cats.effect.IO
import org.slf4j.LoggerFactory

/**
 * CreateCourseMessage
 * desc: 用于处理创建课程的功能需求。
 * @param teacherToken: String (教师的身份验证令牌，用于鉴权。)
 * @param courseGroupID: Int (课程组ID，用于标识所属课程组。)
 * @param courseCapacity: Int (课程容量，表示该课程允许的最大学生人数。)
 * @param time: List[CourseTime] (课程上课时间列表，包含每节课的时间信息。)
 * @param location: String (课程的上课地点。)
 * @return createdCourse: CourseInfo (新创建的课程信息，包括课程ID、课程容量、时间、地点、课程组信息等。)
 */

case class CreateCourseMessage(
  teacherToken: String,
  courseGroupID: Int,
  courseCapacity: Int,
  time: List[CourseTime],
  location: String
) extends API[CourseInfo](CourseManagementServiceCode)

object CreateCourseMessage {

  import Common.Serialize.CustomColumnTypes.{decodeDateTime, encodeDateTime}

  private val circeEncoder: Encoder[CreateCourseMessage] = deriveEncoder
  private val circeDecoder: Decoder[CreateCourseMessage] = deriveDecoder

  private val jacksonEncoder: Encoder[CreateCourseMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }
  private val jacksonDecoder: Decoder[CreateCourseMessage] = Decoder.instance { cursor =>
    try {
      Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[CreateCourseMessage]() {}))
    } catch {
      case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history))
    }
  }

  given createCourseMessageEncoder: Encoder[CreateCourseMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  given createCourseMessageDecoder: Decoder[CreateCourseMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }

  def createCourse(message: CreateCourseMessage)(using PlanContext): IO[CourseInfo] = {
    for {
      _ <- IO(LoggerFactory.getLogger(getClass).info(s"Processing CreateCourseMessage for teacher token: ${message.teacherToken}"))

      // Step 1: 验证教师身份
      teacherID <- validateTeacherToken(message.teacherToken)

      // Step 2: 插入课程信息
      courseID <- insertCourseIntoDatabase(
        teacherID,
        message.courseGroupID,
        message.courseCapacity,
        message.location
      )

      // Step 3: 插入课程时间信息
      _ <- insertCourseTimes(courseID, message.time)

      // Step 4: 构造返回结果
      createdCourse <- buildCourseInfo(courseID)
      
    } yield createdCourse
  }

  private def validateTeacherToken(token: String)(using PlanContext): IO[Int] = {
    val sql = s"SELECT teacher_id FROM ${schemaName}.teacher WHERE token = ?;"
    readDBInt(sql, List(SqlParameter("String", token)))
  }

  private def insertCourseIntoDatabase(
    teacherID: Int,
    courseGroupID: Int,
    courseCapacity: Int,
    location: String
  )(using PlanContext): IO[Int] = {
    val sql =
      s"""
         |INSERT INTO ${schemaName}.course (teacher_id, course_group_id, capacity, location)
         |VALUES (?, ?, ?, ?)
         |RETURNING course_id;
       """.stripMargin
    readDBInt(sql, List(
      SqlParameter("Int", teacherID.toString),
      SqlParameter("Int", courseGroupID.toString),
      SqlParameter("Int", courseCapacity.toString),
      SqlParameter("String", location)
    ))
  }

  private def insertCourseTimes(courseID: Int, times: List[CourseTime])(using PlanContext): IO[String] = {
    val sql =
      s"""
         |INSERT INTO ${schemaName}.course_time (course_id, day_of_week, time_period)
         |VALUES (?, ?, ?);
      """.stripMargin

    val parametersList = times.map { time =>
      ParameterList(List(
        SqlParameter("Int", courseID.toString),
        SqlParameter("String", time.dayOfWeek.toString),
        SqlParameter("String", time.timePeriod.toString)
      ))
    }

    writeDBList(sql, parametersList)
  }

  private def buildCourseInfo(courseID: Int)(using PlanContext): IO[CourseInfo] = {
    val sql = s"SELECT * FROM ${schemaName}.course WHERE course_id = ?;"
    readDBJson(sql, List(SqlParameter("Int", courseID.toString)))
      .map(decodeType[CourseInfo])
  }
}