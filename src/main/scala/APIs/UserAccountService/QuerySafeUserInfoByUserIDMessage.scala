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
 * QuerySafeUserInfoByUserIDMessage
 * desc: 用户传入userID进行查询，系统返回该用户的安全用户信息。
 * @param userID: Int (用户的唯一标识符，用于查询用户信息。)
 * @return safeUserInfo: SafeUserInfo (安全的用户信息，不包含密码，供外部使用。)
 */

case class QuerySafeUserInfoByUserIDMessage(
  userID: Int
) extends API[Option[SafeUserInfo]](UserAccountServiceCode)



case object QuerySafeUserInfoByUserIDMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[QuerySafeUserInfoByUserIDMessage] = deriveEncoder
  private val circeDecoder: Decoder[QuerySafeUserInfoByUserIDMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[QuerySafeUserInfoByUserIDMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[QuerySafeUserInfoByUserIDMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[QuerySafeUserInfoByUserIDMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given querySafeUserInfoByUserIDMessageEncoder: Encoder[QuerySafeUserInfoByUserIDMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given querySafeUserInfoByUserIDMessageDecoder: Decoder[QuerySafeUserInfoByUserIDMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

