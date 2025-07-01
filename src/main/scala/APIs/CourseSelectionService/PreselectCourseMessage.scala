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
 * PreselectCourseMessage
 * desc: 用于处理预选课程的功能需求。
 * @param studentToken: String (学生身份验证的token，用于鉴权和识别学生.)
 * @param courseID: Int (课程ID，指定要进行预选的课程.)
 * @return resultMessage: String (操作结果的返回信息，例如预选成功或错误信息.)
 */

case class PreselectCourseMessage(
  studentToken: String,
  courseID: Int
) extends API[String](CourseSelectionServiceCode)



case object PreselectCourseMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[PreselectCourseMessage] = deriveEncoder
  private val circeDecoder: Decoder[PreselectCourseMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[PreselectCourseMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[PreselectCourseMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[PreselectCourseMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given preselectCourseMessageEncoder: Encoder[PreselectCourseMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given preselectCourseMessageDecoder: Decoder[PreselectCourseMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

