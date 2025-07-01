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
 * QuerySafeUserInfoByUserIDListMessage
 * desc: 批量查询用户安全信息，返回结果的顺序与输入的userID列表顺序一致。
 * @param userIDList: Int (用户ID列表，用于筛选安全用户信息，需保持返回结果顺序与输入顺序一致。)
 * @return safeUserInfoList: SafeUserInfo (安全用户信息列表，对应输入的用户ID列表。)
 */

case class QuerySafeUserInfoByUserIDListMessage(
  userIDList: List[Int]
) extends API[List[SafeUserInfo]](UserAccountServiceCode)



case object QuerySafeUserInfoByUserIDListMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[QuerySafeUserInfoByUserIDListMessage] = deriveEncoder
  private val circeDecoder: Decoder[QuerySafeUserInfoByUserIDListMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[QuerySafeUserInfoByUserIDListMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[QuerySafeUserInfoByUserIDListMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[QuerySafeUserInfoByUserIDListMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given querySafeUserInfoByUserIDListMessageEncoder: Encoder[QuerySafeUserInfoByUserIDListMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given querySafeUserInfoByUserIDListMessageDecoder: Decoder[QuerySafeUserInfoByUserIDListMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

