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
import Objects.CourseManagementService.CourseInfo

/**
 * QueryCoursesByCourseGroupMessage
 * desc: 查询课程组特定课程信息的功能
 * @param userToken: String (用户认证的token，用于验证权限。)
 * @param courseGroupID: Int (课程组ID，用于定位需要查询的课程组)
 * @return queriedCourses: CourseInfo (根据课程组ID查询到的所有课程信息列表)
 */

case class QueryCoursesByCourseGroupMessage(
  userToken: String,
  courseGroupID: Int
) extends API[List[CourseInfo]](CourseManagementServiceCode)



case object QueryCoursesByCourseGroupMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[QueryCoursesByCourseGroupMessage] = deriveEncoder
  private val circeDecoder: Decoder[QueryCoursesByCourseGroupMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[QueryCoursesByCourseGroupMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[QueryCoursesByCourseGroupMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[QueryCoursesByCourseGroupMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given queryCoursesByCourseGroupMessageEncoder: Encoder[QueryCoursesByCourseGroupMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given queryCoursesByCourseGroupMessageDecoder: Decoder[QueryCoursesByCourseGroupMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

