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
 * SubmitCourseEvaluationMessage
 * desc: 用于处理提交课程评价的功能需求。
 * @param studentToken: String (学生身份验证的Token，用于鉴权及获取学生ID)
 * @param courseID: Int (课程的唯一标识，用于定位评价的目标课程)
 * @param rating: Rating (课程评价的评分，表示对课程的评分级别)
 * @param feedback: String (课程评价的详细文字描述)
 * @return resultMessage: String (确认处理成功或返回错误信息)
 */

case class SubmitCourseEvaluationMessage(
  studentToken: String,
  courseID: Int,
  rating: Rating,
  feedback: String
) extends API[String](CourseEvaluationServiceCode)



case object SubmitCourseEvaluationMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[SubmitCourseEvaluationMessage] = deriveEncoder
  private val circeDecoder: Decoder[SubmitCourseEvaluationMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[SubmitCourseEvaluationMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[SubmitCourseEvaluationMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[SubmitCourseEvaluationMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given submitCourseEvaluationMessageEncoder: Encoder[SubmitCourseEvaluationMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given submitCourseEvaluationMessageDecoder: Decoder[SubmitCourseEvaluationMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

