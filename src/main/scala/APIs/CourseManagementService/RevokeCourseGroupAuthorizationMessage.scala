package APIs.CourseManagementService

import Common.API.API
import Global.ServiceCenter.CourseManagementServiceCode

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
 * RevokeCourseGroupAuthorizationMessage
 * desc: 用于处理取消授权老师的功能需求。
 * @param teacherToken: String (老师的令牌，用于验证老师身份和权限。)
 * @param courseGroupID: Int (课程组ID，用于定位要操作的课程组。)
 * @param authorizedTeacherID: Int (需要取消授权的老师ID。)
 * @return updatedAuthorizedTeachers: Int (最新的授权老师ID列表。)
 */

case class RevokeCourseGroupAuthorizationMessage(
  teacherToken: String,
  courseGroupID: Int,
  authorizedTeacherID: Int
) extends API[List[Int]](CourseManagementServiceCode)



case object RevokeCourseGroupAuthorizationMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[RevokeCourseGroupAuthorizationMessage] = deriveEncoder
  private val circeDecoder: Decoder[RevokeCourseGroupAuthorizationMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[RevokeCourseGroupAuthorizationMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[RevokeCourseGroupAuthorizationMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[RevokeCourseGroupAuthorizationMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given revokeCourseGroupAuthorizationMessageEncoder: Encoder[RevokeCourseGroupAuthorizationMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given revokeCourseGroupAuthorizationMessageDecoder: Decoder[RevokeCourseGroupAuthorizationMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

