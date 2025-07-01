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
 * QueryCourseWaitingListDataMessage
 * desc: 用于查看课程Waiting List数据的功能需求。
 * @param teacherToken: String (教师的身份验证Token，用于进行权限验证。)
 * @param courseID: Int (课程ID，用于指定查询哪一门课程的Waiting List数据。)
 * @return waitingListData: SafeUserInfo (课程的Waiting List数据，返回安全用户信息。)
 */

case class QueryCourseWaitingListDataMessage(
  teacherToken: String,
  courseID: Int
) extends API[List[SafeUserInfo]](CourseSelectionServiceCode)



case object QueryCourseWaitingListDataMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[QueryCourseWaitingListDataMessage] = deriveEncoder
  private val circeDecoder: Decoder[QueryCourseWaitingListDataMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[QueryCourseWaitingListDataMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[QueryCourseWaitingListDataMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[QueryCourseWaitingListDataMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given queryCourseWaitingListDataMessageEncoder: Encoder[QueryCourseWaitingListDataMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given queryCourseWaitingListDataMessageDecoder: Decoder[QueryCourseWaitingListDataMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

