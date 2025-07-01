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
 * SelectCourseMessage
 * desc: 用于处理正选课程的功能需求。
 * @param studentToken: String (学生鉴权token，用于验证学生身份。)
 * @param courseID: Int (课程ID，用于指向具体课程。)
 * @return resultMessage: String (返回结果消息，指示选课操作的成功与否。)
 */

case class SelectCourseMessage(
  studentToken: String,
  courseID: Int
) extends API[String](CourseSelectionServiceCode)



case object SelectCourseMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[SelectCourseMessage] = deriveEncoder
  private val circeDecoder: Decoder[SelectCourseMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[SelectCourseMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[SelectCourseMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[SelectCourseMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given selectCourseMessageEncoder: Encoder[SelectCourseMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given selectCourseMessageDecoder: Decoder[SelectCourseMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

