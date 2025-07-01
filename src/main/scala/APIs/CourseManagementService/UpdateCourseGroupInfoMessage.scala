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
 * UpdateCourseGroupInfoMessage
 * desc: 用于处理修改课程组的功能需求。
 * @param teacherToken: String (教师鉴权所需的令牌，用于验证权限。)
 * @param courseGroupID: Int (课程组ID，用于定位需要修改的课程组。)
 * @param newName: String (新课程组名称，如果需要更新课程组名称则填写。)
 * @param newCredit: Int (新课程组学分，如果需要更新课程组学分则填写。)
 * @return updatedCourseGroup: CourseGroup (更新后的课程组信息，包含课程组ID、名称、学分和创建者等信息。)
 */

case class UpdateCourseGroupInfoMessage(
  teacherToken: String,
  courseGroupID: Int,
  newName: Option[String] = None,
  newCredit: Option[Int] = None
) extends API[CourseGroup](CourseManagementServiceCode)



case object UpdateCourseGroupInfoMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[UpdateCourseGroupInfoMessage] = deriveEncoder
  private val circeDecoder: Decoder[UpdateCourseGroupInfoMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[UpdateCourseGroupInfoMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[UpdateCourseGroupInfoMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[UpdateCourseGroupInfoMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given updateCourseGroupInfoMessageEncoder: Encoder[UpdateCourseGroupInfoMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given updateCourseGroupInfoMessageDecoder: Decoder[UpdateCourseGroupInfoMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

