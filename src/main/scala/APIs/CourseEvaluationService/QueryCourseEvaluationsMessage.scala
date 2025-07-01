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
import Objects.CourseEvaluationService.CourseEvaluation

/**
 * QueryCourseEvaluationsMessage
 * desc: 用于处理查看课程评价的功能需求，返回某门课程的所有评价条目，包括评分及评价文字。
 * @param userToken: String (用户鉴权的Token，用于验证权限。)
 * @param courseID: Int (被查询的课程ID，用于课程评价的筛选。)
 * @return evaluations: CourseEvaluation (包含课程评价的列表信息，包括评价人的ID、评分和反馈内容。)
 */

case class QueryCourseEvaluationsMessage(
  userToken: String,
  courseID: Int
) extends API[List[CourseEvaluation]](CourseEvaluationServiceCode)



case object QueryCourseEvaluationsMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[QueryCourseEvaluationsMessage] = deriveEncoder
  private val circeDecoder: Decoder[QueryCourseEvaluationsMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[QueryCourseEvaluationsMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[QueryCourseEvaluationsMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[QueryCourseEvaluationsMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given queryCourseEvaluationsMessageEncoder: Encoder[QueryCourseEvaluationsMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given queryCourseEvaluationsMessageDecoder: Decoder[QueryCourseEvaluationsMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

