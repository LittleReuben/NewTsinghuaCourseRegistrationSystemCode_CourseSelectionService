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
 * DeleteCourseGroupMessage
 * desc: 用于处理删除课程组的功能需求。
 * @param teacherToken: String (教师的身份令牌，用于鉴权)
 * @param courseGroupID: Int (课程组的唯一ID，用于标识需要删除的课程组)
 * @return deleteResult: String (返回删除结果的确认信息，说明操作是否成功)
 */

case class DeleteCourseGroupMessage(
  teacherToken: String,
  courseGroupID: Int
) extends API[String](CourseManagementServiceCode)



case object DeleteCourseGroupMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[DeleteCourseGroupMessage] = deriveEncoder
  private val circeDecoder: Decoder[DeleteCourseGroupMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[DeleteCourseGroupMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[DeleteCourseGroupMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[DeleteCourseGroupMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given deleteCourseGroupMessageEncoder: Encoder[DeleteCourseGroupMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given deleteCourseGroupMessageDecoder: Decoder[DeleteCourseGroupMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

