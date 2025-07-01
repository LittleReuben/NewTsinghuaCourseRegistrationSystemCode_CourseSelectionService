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
import Objects.CourseManagementService.CourseInfo

/**
 * QueryStudentSelectedCoursesMessage
 * desc: 用于处理查询选中课程的功能需求。
 * @param studentToken: String (学生的鉴权token，用于验证身份和权限)
 * @return selectedCourses: CourseInfo (学生选中课程的详细信息列表)
 */

case class QueryStudentSelectedCoursesMessage(
  studentToken: String
) extends API[List[CourseInfo]](CourseSelectionServiceCode)



case object QueryStudentSelectedCoursesMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[QueryStudentSelectedCoursesMessage] = deriveEncoder
  private val circeDecoder: Decoder[QueryStudentSelectedCoursesMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[QueryStudentSelectedCoursesMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[QueryStudentSelectedCoursesMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[QueryStudentSelectedCoursesMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given queryStudentSelectedCoursesMessageEncoder: Encoder[QueryStudentSelectedCoursesMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given queryStudentSelectedCoursesMessageDecoder: Decoder[QueryStudentSelectedCoursesMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

