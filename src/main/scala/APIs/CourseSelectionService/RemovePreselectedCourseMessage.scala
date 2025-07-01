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
 * RemovePreselectedCourseMessage
 * desc: 用于处理移除预选课程的功能需求
 * @param studentToken: String (学生登录的鉴权Token，用于确认身份)
 * @param courseID: Int (课程的唯一标识ID，用于确定需要移除预选记录的课程)
 * @return resultMessage: String (操作执行结果的信息反馈，例如成功或错误信息)
 */

case class RemovePreselectedCourseMessage(
  studentToken: String,
  courseID: Int
) extends API[String](CourseSelectionServiceCode)



case object RemovePreselectedCourseMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[RemovePreselectedCourseMessage] = deriveEncoder
  private val circeDecoder: Decoder[RemovePreselectedCourseMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[RemovePreselectedCourseMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[RemovePreselectedCourseMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[RemovePreselectedCourseMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given removePreselectedCourseMessageEncoder: Encoder[RemovePreselectedCourseMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given removePreselectedCourseMessageDecoder: Decoder[RemovePreselectedCourseMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

