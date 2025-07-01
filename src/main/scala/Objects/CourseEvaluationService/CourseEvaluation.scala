package Objects.CourseEvaluationService


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
 * CourseEvaluation
 * desc: 课程评价，包括评分和反馈
 * @param evaluatorID: Int (评价者的唯一ID)
 * @param courseID: Int (课程唯一ID)
 * @param rating: Rating (评价等级)
 * @param feedback: String (评价的具体内容)
 */

case class CourseEvaluation(
  evaluatorID: Int,
  courseID: Int,
  rating: Rating,
  feedback: Option[String] = None
){

  //process class code 预留标志位，不要删除


}


case object CourseEvaluation{

    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[CourseEvaluation] = deriveEncoder
  private val circeDecoder: Decoder[CourseEvaluation] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[CourseEvaluation] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[CourseEvaluation] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[CourseEvaluation]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given courseEvaluationEncoder: Encoder[CourseEvaluation] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given courseEvaluationDecoder: Decoder[CourseEvaluation] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }



  //process object code 预留标志位，不要删除


}

