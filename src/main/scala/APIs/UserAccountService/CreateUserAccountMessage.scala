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
import Objects.UserAccountService.UserRole
import Objects.UserAccountService.UserInfo

/**
 * CreateUserAccountMessage
 * desc: 管理员传入adminToken验证权限后，传入用户的名字、账号名、密码以及角色类型（Teacher或Student），系统自动生成userID并返回新增用户信息。
 * @param adminToken: String (管理员权限校验令牌)
 * @param name: String (用户的姓名)
 * @param accountName: String (用户的账号名，唯一标识)
 * @param password: String (用户的登录密码)
 * @param role: UserRole:1186 (用户角色类型，可选择Teacher或Student)
 * @return userInfo: UserInfo:1129 (新增用户的完整信息)
 */

case class CreateUserAccountMessage(
  adminToken: String,
  name: String,
  accountName: String,
  password: String,
  role: UserRole
) extends API[UserInfo](UserAccountServiceCode)



case object CreateUserAccountMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[CreateUserAccountMessage] = deriveEncoder
  private val circeDecoder: Decoder[CreateUserAccountMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[CreateUserAccountMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[CreateUserAccountMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[CreateUserAccountMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given createUserAccountMessageEncoder: Encoder[CreateUserAccountMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given createUserAccountMessageDecoder: Decoder[CreateUserAccountMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

