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
 * QueryOwnCourseGroupsMessage
 * desc: 用于处理查看课程组的功能需求。
 * @param teacherToken: String (教师的鉴权令牌，用于验证教师身份)
 * @return ownCourseGroups: CourseGroup (教师创建的课程组列表)
 */

case class QueryOwnCourseGroupsMessage(
  teacherToken: String
) extends API[List[CourseGroup]](CourseManagementServiceCode)



case object QueryOwnCourseGroupsMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[QueryOwnCourseGroupsMessage] = deriveEncoder
  private val circeDecoder: Decoder[QueryOwnCourseGroupsMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[QueryOwnCourseGroupsMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[QueryOwnCourseGroupsMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[QueryOwnCourseGroupsMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given queryOwnCourseGroupsMessageEncoder: Encoder[QueryOwnCourseGroupsMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given queryOwnCourseGroupsMessageDecoder: Decoder[QueryOwnCourseGroupsMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

