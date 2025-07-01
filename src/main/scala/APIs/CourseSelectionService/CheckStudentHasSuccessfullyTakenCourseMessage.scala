package APIs.CourseSelectionService

import Common.API.API
import Global.ServiceCenter.CourseSelectionServiceCode

import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.syntax.*
import io.circe.parser.*
import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

import com.fasterxml.jackson.core.`type`.TypeReference
import Common.Serialize.JacksonSerializeUtils

import scala.util.Try

import org.joda.time.DateTime
import java.util.UUID


/**
 * CheckStudentHasSuccessfullyTakenCourseMessage
 * desc: 查询学生是否曾选上某门课程的功能需求
 * @param studentToken: String (学生登录令牌，用于验证身份)
 * @param courseID: Int (课程ID，用于指定查询的课程)
 * @return hasTakenCourse: Boolean (布尔值，表示学生是否选上过该课程)
 */

case class CheckStudentHasSuccessfullyTakenCourseMessage(
  studentToken: String,
  courseID: Int
) extends API[Boolean](CourseSelectionServiceCode)



case object CheckStudentHasSuccessfullyTakenCourseMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[CheckStudentHasSuccessfullyTakenCourseMessage] = deriveEncoder
  private val circeDecoder: Decoder[CheckStudentHasSuccessfullyTakenCourseMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[CheckStudentHasSuccessfullyTakenCourseMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[CheckStudentHasSuccessfullyTakenCourseMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[CheckStudentHasSuccessfullyTakenCourseMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given checkStudentHasSuccessfullyTakenCourseMessageEncoder: Encoder[CheckStudentHasSuccessfullyTakenCourseMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given checkStudentHasSuccessfullyTakenCourseMessageDecoder: Decoder[CheckStudentHasSuccessfullyTakenCourseMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

