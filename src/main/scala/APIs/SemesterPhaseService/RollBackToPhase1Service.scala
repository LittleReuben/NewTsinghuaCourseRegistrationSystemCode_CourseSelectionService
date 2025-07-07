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


/**
 * RollBackToPhase1Service
 * desc: 清空所有选课信息，保留用户与开课信息
 * @param adminToken: String (管理员token)
 */

case class RollBackToPhase1Service(
  adminToken: String
) extends API[Boolean](SemesterPhaseServiceCode)



case object RollBackToPhase1Service{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[RollBackToPhase1Service] = deriveEncoder
  private val circeDecoder: Decoder[RollBackToPhase1Service] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[RollBackToPhase1Service] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[RollBackToPhase1Service] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[RollBackToPhase1Service]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given rollBackToPhase1ServiceEncoder: Encoder[RollBackToPhase1Service] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given rollBackToPhase1ServiceDecoder: Decoder[RollBackToPhase1Service] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

