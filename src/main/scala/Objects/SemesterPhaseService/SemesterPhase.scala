package Objects.SemesterPhaseService


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
import Objects.SemesterPhaseService.Phase
import Objects.SemesterPhaseService.Permissions

/**
 * SemesterPhase
 * desc: 学期阶段信息
 * @param currentPhase: Phase:1069 (当前的学期阶段)
 * @param permissions: Permissions:1171 (阶段对应的权限配置)
 */

case class SemesterPhase(
  currentPhase: Phase,
  permissions: Permissions
){

  //process class code 预留标志位，不要删除


}


case object SemesterPhase{

    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[SemesterPhase] = deriveEncoder
  private val circeDecoder: Decoder[SemesterPhase] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[SemesterPhase] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[SemesterPhase] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[SemesterPhase]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given semesterPhaseEncoder: Encoder[SemesterPhase] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given semesterPhaseDecoder: Decoder[SemesterPhase] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }



  //process object code 预留标志位，不要删除


}

