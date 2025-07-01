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
 * GrantCourseGroupAuthorizationMessage
 * desc: 用于处理授权老师开设课程组相关课程的功能需求。
 * @param teacherToken: String (教师鉴权Token，用于确认教师身份是否有效。)
 * @param courseGroupID: Int (课程组ID标识，用于指定需进行授权操作的课程组。)
 * @param authorizedTeacherID: Int (被授权教师的ID，用于指定需要授予权限的教师。)
 * @return updatedAuthorizedTeachers: Int (更新后的授权教师ID列表，与课程组关联。)
 */

case class GrantCourseGroupAuthorizationMessage(
  teacherToken: String,
  courseGroupID: Int,
  authorizedTeacherID: Int
) extends API[List[Int]](CourseManagementServiceCode)



case object GrantCourseGroupAuthorizationMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[GrantCourseGroupAuthorizationMessage] = deriveEncoder
  private val circeDecoder: Decoder[GrantCourseGroupAuthorizationMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[GrantCourseGroupAuthorizationMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[GrantCourseGroupAuthorizationMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[GrantCourseGroupAuthorizationMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given grantCourseGroupAuthorizationMessageEncoder: Encoder[GrantCourseGroupAuthorizationMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given grantCourseGroupAuthorizationMessageDecoder: Decoder[GrantCourseGroupAuthorizationMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

