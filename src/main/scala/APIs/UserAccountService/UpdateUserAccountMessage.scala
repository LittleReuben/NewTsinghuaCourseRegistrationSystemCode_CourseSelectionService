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
import Objects.UserAccountService.UserInfo

/**
 * UpdateUserAccountMessage
 * desc: 管理员传入adminToken验证权限后，根据userID查找用户，并根据传入的更新字段完成信息修改，返回最新用户信息。同时，如果被修改的用户处于登录状态需要强制登出。
 * @param adminToken: String (管理员权限验证的token，确保操作的管理权限)
 * @param userID: Int (目标用户的ID，用来定位用户在数据库中的记录)
 * @param newName: String (用户的新名字，系统会根据传入字段进行更新)
 * @param newAccountName: String (用户的新账号名，需要验证唯一性)
 * @param newPassword: String (用户的新密码，需要加密存储)
 * @return updatedUserInfo: UserInfo (更新后的用户完整信息，包括userID、userName、accountName、密码和用户角色)
 */

case class UpdateUserAccountMessage(
  adminToken: String,
  userID: Int,
  newName: Option[String] = None,
  newAccountName: Option[String] = None,
  newPassword: Option[String] = None
) extends API[UserInfo](UserAccountServiceCode)



case object UpdateUserAccountMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[UpdateUserAccountMessage] = deriveEncoder
  private val circeDecoder: Decoder[UpdateUserAccountMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[UpdateUserAccountMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[UpdateUserAccountMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[UpdateUserAccountMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given updateUserAccountMessageEncoder: Encoder[UpdateUserAccountMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given updateUserAccountMessageDecoder: Decoder[UpdateUserAccountMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

