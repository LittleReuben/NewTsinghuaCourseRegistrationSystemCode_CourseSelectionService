package APIs.SemesterPhaseService

import Common.API.API
import Global.ServiceCenter.SemesterPhaseServiceCode

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
import Objects.SemesterPhaseService.SemesterPhase

/**
 * QuerySemesterPhaseStatusMessage
 * desc: 用户传入userToken验证权限后，系统返回目前的学期阶段信息及操作权限状态。
 * @param userToken: String (用户唯一认证的token，用于验证权限)
 * @return semesterPhase: SemesterPhase (包含学期阶段当前状态及权限信息的SemesterPhase对象)
 */

case class QuerySemesterPhaseStatusMessage(
  userToken: String
) extends API[SemesterPhase](SemesterPhaseServiceCode)



case object QuerySemesterPhaseStatusMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[QuerySemesterPhaseStatusMessage] = deriveEncoder
  private val circeDecoder: Decoder[QuerySemesterPhaseStatusMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[QuerySemesterPhaseStatusMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[QuerySemesterPhaseStatusMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[QuerySemesterPhaseStatusMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given querySemesterPhaseStatusMessageEncoder: Encoder[QuerySemesterPhaseStatusMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given querySemesterPhaseStatusMessageDecoder: Decoder[QuerySemesterPhaseStatusMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

