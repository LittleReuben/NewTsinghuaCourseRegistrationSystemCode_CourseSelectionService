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
 * UserInfo
 * desc: 用户信息，包括用户的基本信息和权限角色
 * @param userID: Int (用户的唯一ID)
 * @param userName: String (用户名)
 * @param accountName: String (账户名)
 * @param password: String (账户密码)
 * @param role: UserRole (用户权限角色)
 */

case class UserInfo(
  userID: Int,
  userName: String,
  accountName: String,
  password: String,
  role: UserRole
){

  //process class code 预留标志位，不要删除


}


case object UserInfo{

    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[UserInfo] = deriveEncoder
  private val circeDecoder: Decoder[UserInfo] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[UserInfo] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[UserInfo] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[UserInfo]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given userInfoEncoder: Encoder[UserInfo] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given userInfoDecoder: Decoder[UserInfo] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }



  //process object code 预留标志位，不要删除


}

