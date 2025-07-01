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
 * QueryCoursePreselectionDataMessage
 * desc: 用于查看课程预选数据的功能需求。
 * @param teacherToken: String (教师Token, 用于鉴权和验证教师身份。)
 * @param courseID: Int (课程ID，用于指定需要查询的课程。)
 * @return preselectionData: SafeUserInfo (预选课程的学生安全信息列表。)
 */

case class QueryCoursePreselectionDataMessage(
  teacherToken: String,
  courseID: Int
) extends API[List[SafeUserInfo]](CourseSelectionServiceCode)



case object QueryCoursePreselectionDataMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[QueryCoursePreselectionDataMessage] = deriveEncoder
  private val circeDecoder: Decoder[QueryCoursePreselectionDataMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[QueryCoursePreselectionDataMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[QueryCoursePreselectionDataMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[QueryCoursePreselectionDataMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given queryCoursePreselectionDataMessageEncoder: Encoder[QueryCoursePreselectionDataMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given queryCoursePreselectionDataMessageDecoder: Decoder[QueryCoursePreselectionDataMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

