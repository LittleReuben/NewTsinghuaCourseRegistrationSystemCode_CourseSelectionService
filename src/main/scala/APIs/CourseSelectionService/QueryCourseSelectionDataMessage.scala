package APIs.CourseSelectionService

import Common.API.API
import Global.ServiceCenter.CourseSelectionServiceCode

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
import Objects.UserAccountService.SafeUserInfo

/**
 * QueryCourseSelectionDataMessage
 * desc: 用于查看课程选上数据的功能需求。
 * @param teacherToken: String (老师验证身份的令牌，用于鉴权。)
 * @param courseID: Int (课程唯一ID，用于查询对应课程选上的数据。)
 * @return selectionData: SafeUserInfo (选上课程的安全用户信息列表，包含用户ID、用户名、账号名和角色权限。)
 */

case class QueryCourseSelectionDataMessage(
  teacherToken: String,
  courseID: Int
) extends API[List[SafeUserInfo]](CourseSelectionServiceCode)



case object QueryCourseSelectionDataMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[QueryCourseSelectionDataMessage] = deriveEncoder
  private val circeDecoder: Decoder[QueryCourseSelectionDataMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[QueryCourseSelectionDataMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[QueryCourseSelectionDataMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[QueryCourseSelectionDataMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given queryCourseSelectionDataMessageEncoder: Encoder[QueryCourseSelectionDataMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given queryCourseSelectionDataMessageDecoder: Decoder[QueryCourseSelectionDataMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

