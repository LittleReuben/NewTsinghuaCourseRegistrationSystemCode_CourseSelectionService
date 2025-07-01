package APIs.CourseEvaluationService

import Common.API.API
import Global.ServiceCenter.CourseEvaluationServiceCode

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
import Objects.CourseEvaluationService.Rating

/**
 * UpdateCourseEvaluationMessage
 * desc: 用于处理修改课程评价的功能需求。
 * @param studentToken: String (学生登录的身份验证令牌，以便认证学生身份。)
 * @param courseID: Int (课程ID，用于指定需要修改评价的课程。)
 * @param newRating: Rating (新的评分信息，表示课程的评分值。)
 * @param newFeedback: String (新的评价内容，包含学生对课程的文字评价。)
 * @return resultMessage: String (操作结果的消息字符串，例如'更新成功！'或具体错误信息。)
 */

case class UpdateCourseEvaluationMessage(
  studentToken: String,
  courseID: Int,
  newRating: Rating,
  newFeedback: String
) extends API[String](CourseEvaluationServiceCode)



case object UpdateCourseEvaluationMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[UpdateCourseEvaluationMessage] = deriveEncoder
  private val circeDecoder: Decoder[UpdateCourseEvaluationMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[UpdateCourseEvaluationMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[UpdateCourseEvaluationMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[UpdateCourseEvaluationMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given updateCourseEvaluationMessageEncoder: Encoder[UpdateCourseEvaluationMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given updateCourseEvaluationMessageDecoder: Decoder[UpdateCourseEvaluationMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

