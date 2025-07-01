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
 * CreateCourseGroupMessage
 * desc: 用于处理创建课程组的功能需求。
 * @param teacherToken: String (教师鉴权的Token)
 * @param name: String (课程组名称)
 * @param credit: Int (课程组的学分)
 * @return createdCourseGroup: CourseGroup (新增的课程组信息)
 */

case class CreateCourseGroupMessage(
  teacherToken: String,
  name: String,
  credit: Int
) extends API[CourseGroup](CourseManagementServiceCode)



case object CreateCourseGroupMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[CreateCourseGroupMessage] = deriveEncoder
  private val circeDecoder: Decoder[CreateCourseGroupMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[CreateCourseGroupMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[CreateCourseGroupMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[CreateCourseGroupMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given createCourseGroupMessageEncoder: Encoder[CreateCourseGroupMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given createCourseGroupMessageDecoder: Decoder[CreateCourseGroupMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

