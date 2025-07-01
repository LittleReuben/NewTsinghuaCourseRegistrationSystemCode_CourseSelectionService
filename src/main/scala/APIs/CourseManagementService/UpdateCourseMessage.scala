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
import Objects.CourseManagementService.CourseInfo

/**
 * UpdateCourseMessage
 * desc: 用于处理修改课程的功能需求
 * @param teacherToken: String (教师身份验证Token，用于鉴权)
 * @param courseID: Int (需要修改的课程ID)
 * @param newCapacity: Int (新的课程容量)
 * @param newLocation: String (新的课程地点)
 * @return updatedCourse: CourseInfo (更新后的课程信息)
 */

case class UpdateCourseMessage(
  teacherToken: String,
  courseID: Int,
  newCapacity: Option[Int] = None,
  newLocation: Option[String] = None
) extends API[CourseInfo](CourseManagementServiceCode)



case object UpdateCourseMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[UpdateCourseMessage] = deriveEncoder
  private val circeDecoder: Decoder[UpdateCourseMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[UpdateCourseMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[UpdateCourseMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[UpdateCourseMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given updateCourseMessageEncoder: Encoder[UpdateCourseMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given updateCourseMessageDecoder: Decoder[UpdateCourseMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

