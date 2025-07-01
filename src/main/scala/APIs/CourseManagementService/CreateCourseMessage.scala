package APIs.CourseManagementService

import Common.API.API
import Global.ServiceCenter.CourseManagementServiceCode

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
import Objects.CourseManagementService.CourseTime
import Objects.CourseManagementService.CourseInfo

/**
 * CreateCourseMessage
 * desc: 用于处理创建课程的功能需求。
 * @param teacherToken: String (教师的身份验证令牌，用于鉴权。)
 * @param courseGroupID: Int (课程组ID，用于标识所属课程组。)
 * @param courseCapacity: Int (课程容量，表示该课程允许的最大学生人数。)
 * @param time: CourseTime:1173 (课程上课时间列表，包含每节课的时间信息。)
 * @param location: String (课程的上课地点。)
 * @return createdCourse: CourseInfo:1180 (新创建的课程信息，包括课程ID、课程容量、时间、地点、课程组信息等。)
 */

case class CreateCourseMessage(
  teacherToken: String,
  courseGroupID: Int,
  courseCapacity: Int,
  time: List[CourseTime],
  location: String
) extends API[CourseInfo](CourseManagementServiceCode)



case object CreateCourseMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[CreateCourseMessage] = deriveEncoder
  private val circeDecoder: Decoder[CreateCourseMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[CreateCourseMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[CreateCourseMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[CreateCourseMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given createCourseMessageEncoder: Encoder[CreateCourseMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given createCourseMessageDecoder: Decoder[CreateCourseMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

