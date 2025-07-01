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
import Objects.CourseManagementService.DayOfWeek
import Objects.CourseManagementService.TimePeriod

/**
 * CourseTime
 * desc: 表示课程的具体时间安排，包括星期几和时间段
 * @param dayOfWeek: DayOfWeek:1035 (表示课程在哪一天进行)
 * @param timePeriod: TimePeriod:1151 (表示课程在哪个时间段进行)
 */

case class CourseTime(
  dayOfWeek: DayOfWeek,
  timePeriod: TimePeriod
){

  //process class code 预留标志位，不要删除


}


case object CourseTime{

    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[CourseTime] = deriveEncoder
  private val circeDecoder: Decoder[CourseTime] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[CourseTime] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[CourseTime] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[CourseTime]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given courseTimeEncoder: Encoder[CourseTime] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given courseTimeDecoder: Decoder[CourseTime] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }



  //process object code 预留标志位，不要删除


}

