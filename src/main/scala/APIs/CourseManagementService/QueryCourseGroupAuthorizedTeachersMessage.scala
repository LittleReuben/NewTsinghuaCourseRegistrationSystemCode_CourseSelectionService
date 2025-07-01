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
 * QueryCourseGroupAuthorizedTeachersMessage
 * desc: 用于处理查看授权老师的功能需求。
 * @param teacherToken: String (教师身份验证的Token。)
 * @param courseGroupID: Int (需要查询的课程组ID。)
 * @return authorizedTeachers: Int (授权老师的ID列表。)
 */

case class QueryCourseGroupAuthorizedTeachersMessage(
  teacherToken: String,
  courseGroupID: Int
) extends API[List[Int]](CourseManagementServiceCode)



case object QueryCourseGroupAuthorizedTeachersMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[QueryCourseGroupAuthorizedTeachersMessage] = deriveEncoder
  private val circeDecoder: Decoder[QueryCourseGroupAuthorizedTeachersMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[QueryCourseGroupAuthorizedTeachersMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[QueryCourseGroupAuthorizedTeachersMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[QueryCourseGroupAuthorizedTeachersMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given queryCourseGroupAuthorizedTeachersMessageEncoder: Encoder[QueryCourseGroupAuthorizedTeachersMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given queryCourseGroupAuthorizedTeachersMessageDecoder: Decoder[QueryCourseGroupAuthorizedTeachersMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

