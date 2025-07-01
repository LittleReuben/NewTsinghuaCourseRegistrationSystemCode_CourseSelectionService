package Objects.CourseManagementService


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
import Objects.CourseManagementService.CourseGroup
import Objects.CourseManagementService.CourseInfo

/**
 * PairOfGroupAndCourse
 * desc: 
 * @param CourseGroup: CourseGroup (课程组)
 * @param Course: CourseInfo:1180 (课程信息)
 */

case class PairOfGroupAndCourse(
  CourseGroup: CourseGroup,
  Course: CourseInfo
){

  //process class code 预留标志位，不要删除


}


case object PairOfGroupAndCourse{

    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[PairOfGroupAndCourse] = deriveEncoder
  private val circeDecoder: Decoder[PairOfGroupAndCourse] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[PairOfGroupAndCourse] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[PairOfGroupAndCourse] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[PairOfGroupAndCourse]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given pairOfGroupAndCourseEncoder: Encoder[PairOfGroupAndCourse] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given pairOfGroupAndCourseDecoder: Decoder[PairOfGroupAndCourse] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }



  //process object code 预留标志位，不要删除


}

