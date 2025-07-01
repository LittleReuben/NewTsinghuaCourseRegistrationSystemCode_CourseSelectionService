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
 * QueryCourseGroupByIDMessage
 * desc: 根据课程组ID查询课程组信息
 * @param userToken: String (用户认证的token，用于验证权限。)
 * @param courseGroupID: Int (课程组ID，用于指定查询的目标。)
 * @return courseGroup: CourseGroup (返回的课程组信息，包含课程组的ID、名称、学分、创建者及授权教师列表。)
 */

case class QueryCourseGroupByIDMessage(
  userToken: String,
  courseGroupID: Int
) extends API[CourseGroup](CourseManagementServiceCode)



case object QueryCourseGroupByIDMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[QueryCourseGroupByIDMessage] = deriveEncoder
  private val circeDecoder: Decoder[QueryCourseGroupByIDMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[QueryCourseGroupByIDMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[QueryCourseGroupByIDMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[QueryCourseGroupByIDMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given queryCourseGroupByIDMessageEncoder: Encoder[QueryCourseGroupByIDMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given queryCourseGroupByIDMessageDecoder: Decoder[QueryCourseGroupByIDMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

