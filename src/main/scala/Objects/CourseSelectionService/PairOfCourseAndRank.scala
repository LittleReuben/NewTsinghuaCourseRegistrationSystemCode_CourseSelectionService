package Objects.CourseSelectionService


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
 * PairOfCourseAndRank
 * desc: 
 * @param course: CourseInfo:1180 (课程信息)
 * @param rank: Int (waiting list中的排名)
 */

case class PairOfCourseAndRank(
  course: CourseInfo,
  rank: Int
){

  //process class code 预留标志位，不要删除


}


case object PairOfCourseAndRank{

    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[PairOfCourseAndRank] = deriveEncoder
  private val circeDecoder: Decoder[PairOfCourseAndRank] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[PairOfCourseAndRank] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[PairOfCourseAndRank] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[PairOfCourseAndRank]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given pairOfCourseAndRankEncoder: Encoder[PairOfCourseAndRank] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given pairOfCourseAndRankDecoder: Decoder[PairOfCourseAndRank] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }



  //process object code 预留标志位，不要删除


}

