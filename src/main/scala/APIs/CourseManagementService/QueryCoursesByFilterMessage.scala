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
import Objects.CourseManagementService.CourseTime
import Objects.CourseManagementService.PairOfGroupAndCourse

/**
 * QueryCoursesByFilterMessage
 * desc: 按复杂条件动态查询课程，支持通过课程组号、课程组名、教师姓名及上课时间条件过滤查询课程信息。
 * @param userToken: String (用于验证用户权限的token。)
 * @param courseGroupID: Int (课程组ID，用于过滤课程信息。)
 * @param courseGroupName: String (课程组名，用于过滤课程信息。)
 * @param teacherName: String (教师姓名，用于过滤课程信息。)
 * @param allowedTimePeriods: CourseTime:1173 (允许的课程时间列表，用于过滤课程信息。)
 * @return filteredCourses: PairOfGroupAndCourse:1196 (符合过滤条件的课程组和课程信息列表。)
 */

case class QueryCoursesByFilterMessage(
  userToken: String,
  courseGroupID: Option[Int] = None,
  courseGroupName: Option[String] = None,
  teacherName: Option[String] = None,
  allowedTimePeriods: List[CourseTime]
) extends API[List[PairOfGroupAndCourse]](CourseManagementServiceCode)



case object QueryCoursesByFilterMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[QueryCoursesByFilterMessage] = deriveEncoder
  private val circeDecoder: Decoder[QueryCoursesByFilterMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[QueryCoursesByFilterMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[QueryCoursesByFilterMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[QueryCoursesByFilterMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given queryCoursesByFilterMessageEncoder: Encoder[QueryCoursesByFilterMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given queryCoursesByFilterMessageDecoder: Decoder[QueryCoursesByFilterMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

