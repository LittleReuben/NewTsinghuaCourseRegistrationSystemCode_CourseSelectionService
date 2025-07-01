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
 * QueryStudentPreselectedCoursesMessage
 * desc: 用于处理查询预选课程的功能需求。
 * @param studentToken: String (学生登录的唯一标识，用于鉴权。)
 * @return preselectedCourses: CourseInfo (学生预选课程的详细信息列表。)
 */

case class QueryStudentPreselectedCoursesMessage(
  studentToken: String
) extends API[List[CourseInfo]](CourseSelectionServiceCode)



case object QueryStudentPreselectedCoursesMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[QueryStudentPreselectedCoursesMessage] = deriveEncoder
  private val circeDecoder: Decoder[QueryStudentPreselectedCoursesMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[QueryStudentPreselectedCoursesMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[QueryStudentPreselectedCoursesMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[QueryStudentPreselectedCoursesMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given queryStudentPreselectedCoursesMessageEncoder: Encoder[QueryStudentPreselectedCoursesMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given queryStudentPreselectedCoursesMessageDecoder: Decoder[QueryStudentPreselectedCoursesMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

