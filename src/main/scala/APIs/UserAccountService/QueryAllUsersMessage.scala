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
 * QueryAllUsersMessage
 * desc: 管理员传入adminToken验证权限后，根据用户角色（Teacher或Student）过滤查询，系统返回对应的用户账号信息列表。
 * @param adminToken: String (管理员权限验证的Token，用于确认调用者具有足够权限。)
 * @param role: UserRole:1186 (用户角色类型，用于筛选用户列表（枚举：SuperAdmin、Teacher、Student）。)
 * @return userList: UserInfo:1129 (用户信息列表，包含符合筛选条件的用户数据。)
 */

case class QueryAllUsersMessage(
  adminToken: String,
  role: UserRole
) extends API[List[UserInfo]](UserAccountServiceCode)



case object QueryAllUsersMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[QueryAllUsersMessage] = deriveEncoder
  private val circeDecoder: Decoder[QueryAllUsersMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[QueryAllUsersMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[QueryAllUsersMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[QueryAllUsersMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given queryAllUsersMessageEncoder: Encoder[QueryAllUsersMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given queryAllUsersMessageDecoder: Decoder[QueryAllUsersMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

