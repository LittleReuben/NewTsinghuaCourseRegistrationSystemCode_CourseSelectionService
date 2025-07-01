package APIs.UserAccountService

import Common.API.API
import Global.ServiceCenter.UserAccountServiceCode

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
import Objects.UserAccountService.SafeUserInfo

/**
 * QuerySafeUserInfoByTokenMessage
 * desc: 根据用户传入的token验证并返回对应的安全用户信息。
 * @param userToken: String (用户的访问令牌，用于验证身份)
 * @return safeUserInfo: SafeUserInfo (包含用户ID、用户名、账号名及角色的安全用户信息，不返回密码。)
 */

case class QuerySafeUserInfoByTokenMessage(
  userToken: String
) extends API[Option[SafeUserInfo]](UserAccountServiceCode)



case object QuerySafeUserInfoByTokenMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[QuerySafeUserInfoByTokenMessage] = deriveEncoder
  private val circeDecoder: Decoder[QuerySafeUserInfoByTokenMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[QuerySafeUserInfoByTokenMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[QuerySafeUserInfoByTokenMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[QuerySafeUserInfoByTokenMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given querySafeUserInfoByTokenMessageEncoder: Encoder[QuerySafeUserInfoByTokenMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given querySafeUserInfoByTokenMessageDecoder: Decoder[QuerySafeUserInfoByTokenMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

