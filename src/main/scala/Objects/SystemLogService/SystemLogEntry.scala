package Objects.SystemLogService


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
 * SystemLogEntry
 * desc: 系统日志条目，记录用户操作及相关信息
 * @param logID: Int (日志的唯一标识)
 * @param timestamp: DateTime (日志生成的时间戳)
 * @param userID: Int (用户的唯一标识)
 * @param action: String (用户执行的操作)
 * @param details: String (操作的详细信息)
 */

case class SystemLogEntry(
  logID: Int,
  timestamp: DateTime,
  userID: Int,
  action: String,
  details: String
){

  //process class code 预留标志位，不要删除


}


case object SystemLogEntry{

    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[SystemLogEntry] = deriveEncoder
  private val circeDecoder: Decoder[SystemLogEntry] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[SystemLogEntry] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[SystemLogEntry] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[SystemLogEntry]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given systemLogEntryEncoder: Encoder[SystemLogEntry] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given systemLogEntryDecoder: Decoder[SystemLogEntry] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }



  //process object code 预留标志位，不要删除


}

