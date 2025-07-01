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
import Objects.CourseSelectionService.PairOfCourseAndRank

/**
 * QueryStudentWaitingListStatusMessage
 * desc: 用于处理查询Waiting List状态及排名的功能需求。
 * @param studentToken: String (学生的身份验证Token，用于鉴权和获取学生ID。)
 * @return waitingListStatus: PairOfCourseAndRank:1195 (Waiting List状态，包括课程信息和排名。)
 */

case class QueryStudentWaitingListStatusMessage(
  studentToken: String
) extends API[List[PairOfCourseAndRank]](CourseSelectionServiceCode)



case object QueryStudentWaitingListStatusMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[QueryStudentWaitingListStatusMessage] = deriveEncoder
  private val circeDecoder: Decoder[QueryStudentWaitingListStatusMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[QueryStudentWaitingListStatusMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[QueryStudentWaitingListStatusMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[QueryStudentWaitingListStatusMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given queryStudentWaitingListStatusMessageEncoder: Encoder[QueryStudentWaitingListStatusMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given queryStudentWaitingListStatusMessageDecoder: Decoder[QueryStudentWaitingListStatusMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

