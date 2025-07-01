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
import Objects.CourseManagementService.CourseGroup

/**
 * QueryCourseGroupAuthorizationReceivedMessage
 * desc: 用于处理查看被授权课程组的功能需求。
 * @param teacherToken: String (教师登录的鉴权Token，用以验证权限并获取teacherID。)
 * @return receivedCourseGroups: CourseGroup (被授权的课程组信息列表，包含课程组的ID、名称、学分和创建者信息。)
 */

case class QueryCourseGroupAuthorizationReceivedMessage(
  teacherToken: String
) extends API[List[CourseGroup]](CourseManagementServiceCode)



case object QueryCourseGroupAuthorizationReceivedMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[QueryCourseGroupAuthorizationReceivedMessage] = deriveEncoder
  private val circeDecoder: Decoder[QueryCourseGroupAuthorizationReceivedMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[QueryCourseGroupAuthorizationReceivedMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[QueryCourseGroupAuthorizationReceivedMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[QueryCourseGroupAuthorizationReceivedMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given queryCourseGroupAuthorizationReceivedMessageEncoder: Encoder[QueryCourseGroupAuthorizationReceivedMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given queryCourseGroupAuthorizationReceivedMessageDecoder: Decoder[QueryCourseGroupAuthorizationReceivedMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

