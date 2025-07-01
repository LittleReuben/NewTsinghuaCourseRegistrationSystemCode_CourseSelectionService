package APIs.UserAuthService

import Common.API.API
import Global.ServiceCenter.UserAuthServiceCode

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
 * VerifyTokenValidityMessage
 * desc: 验证Token有效性的接口
 * @param userToken: String (用户的登录token，用于验证其是否合法且未过期。)
 * @return isValid: Boolean (判断Token是否有效，返回布尔值。)
 */

case class VerifyTokenValidityMessage(
  userToken: String
) extends API[Boolean](UserAuthServiceCode)



case object VerifyTokenValidityMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[VerifyTokenValidityMessage] = deriveEncoder
  private val circeDecoder: Decoder[VerifyTokenValidityMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[VerifyTokenValidityMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[VerifyTokenValidityMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[VerifyTokenValidityMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given verifyTokenValidityMessageEncoder: Encoder[VerifyTokenValidityMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given verifyTokenValidityMessageDecoder: Decoder[VerifyTokenValidityMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

