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
 * RunCourseRandomSelectionAndMoveToNextPhaseMessage
 * desc: 管理员执行系统抽签，将学生分配至选上列表或Waiting List，完成阶段切换。返回抽签确认信息。
 * @param adminToken: String (管理员的权限验证token。)
 * @return result: String (确认抽签完成及阶段切换的信息。)
 */

case class RunCourseRandomSelectionAndMoveToNextPhaseMessage(
  adminToken: String
) extends API[String](SemesterPhaseServiceCode)



case object RunCourseRandomSelectionAndMoveToNextPhaseMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[RunCourseRandomSelectionAndMoveToNextPhaseMessage] = deriveEncoder
  private val circeDecoder: Decoder[RunCourseRandomSelectionAndMoveToNextPhaseMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[RunCourseRandomSelectionAndMoveToNextPhaseMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[RunCourseRandomSelectionAndMoveToNextPhaseMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[RunCourseRandomSelectionAndMoveToNextPhaseMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given runCourseRandomSelectionAndMoveToNextPhaseMessageEncoder: Encoder[RunCourseRandomSelectionAndMoveToNextPhaseMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given runCourseRandomSelectionAndMoveToNextPhaseMessageDecoder: Decoder[RunCourseRandomSelectionAndMoveToNextPhaseMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

