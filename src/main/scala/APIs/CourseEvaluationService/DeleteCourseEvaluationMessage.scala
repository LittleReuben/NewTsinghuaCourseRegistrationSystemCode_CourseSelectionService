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


/**
 * DeleteCourseEvaluationMessage
 * desc: 用于处理删除课程评价的功能需求
 * @param studentToken: String (学生身份验证的Token)
 * @param courseID: Int (要删除评价的课程ID)
 * @return resultMessage: String (操作结果信息，例如：删除成功或失败原因)
 */

case class DeleteCourseEvaluationMessage(
  studentToken: String,
  courseID: Int
) extends API[String](CourseEvaluationServiceCode)



case object DeleteCourseEvaluationMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[DeleteCourseEvaluationMessage] = deriveEncoder
  private val circeDecoder: Decoder[DeleteCourseEvaluationMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[DeleteCourseEvaluationMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[DeleteCourseEvaluationMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[DeleteCourseEvaluationMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given deleteCourseEvaluationMessageEncoder: Encoder[DeleteCourseEvaluationMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given deleteCourseEvaluationMessageDecoder: Decoder[DeleteCourseEvaluationMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

