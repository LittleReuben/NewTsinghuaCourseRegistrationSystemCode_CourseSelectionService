package Objects.UserAccountService


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
import Objects.UserAccountService.UserRole

/**
 * SafeUserInfo
 * desc: 用户隐私安全信息，不包含敏感字段
 * @param userID: Int (用户的唯一ID)
 * @param userName: String (用户名)
 * @param accountName: String (账号名)
 * @param role: UserRole (用户角色类型)
 */

case class SafeUserInfo(
  userID: Int,
  userName: String,
  accountName: String,
  role: UserRole
){

  //process class code 预留标志位，不要删除


}


case object SafeUserInfo{

    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[SafeUserInfo] = deriveEncoder
  private val circeDecoder: Decoder[SafeUserInfo] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[SafeUserInfo] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[SafeUserInfo] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[SafeUserInfo]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given safeUserInfoEncoder: Encoder[SafeUserInfo] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given safeUserInfoDecoder: Decoder[SafeUserInfo] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }



  //process object code 预留标志位，不要删除


}

