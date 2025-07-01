package APIs.SystemLogService

import Common.API.API
import Global.ServiceCenter.SystemLogServiceCode

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
import Objects.SystemLogService.SystemLogEntry

/**
 * QuerySystemLogsMessage
 * desc: 管理员查询系统日志记录，并可根据时间范围或者用户ID进行过滤，返回日志信息列表。
 * @param adminToken: String (管理员的身份验证令牌，用于确认操作权限。)
 * @param fromTimestamp: DateTime (查询的起始时间，可选项。)
 * @param toTimestamp: DateTime (查询的结束时间，可选项。)
 * @param userIDs: Int (需要过滤的用户ID列表，可选项。)
 * @return logEntries: SystemLogEntry (返回的日志记录，包含日志ID、时间戳、用户ID、操作描述与详细信息)
 */

case class QuerySystemLogsMessage(
  adminToken: String,
  fromTimestamp: Option[DateTime] = None,
  toTimestamp: Option[DateTime] = None,
  userIDs: List[Int]
) extends API[List[SystemLogEntry]](SystemLogServiceCode)



case object QuerySystemLogsMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[QuerySystemLogsMessage] = deriveEncoder
  private val circeDecoder: Decoder[QuerySystemLogsMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[QuerySystemLogsMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[QuerySystemLogsMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[QuerySystemLogsMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given querySystemLogsMessageEncoder: Encoder[QuerySystemLogsMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given querySystemLogsMessageDecoder: Decoder[QuerySystemLogsMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

