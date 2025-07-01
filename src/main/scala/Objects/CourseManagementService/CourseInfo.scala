package Objects.CourseManagementService


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

/**
 * CourseInfo
 * desc: 课程信息，包括课程ID、容量、上课时间、地点及相关人员信息
 * @param courseID: Int (课程唯一标识ID)
 * @param courseCapacity: Int (课程的容量大小)
 * @param time: CourseTime (课程时间安排信息)
 * @param location: String (课程的地点)
 * @param courseGroupID: Int (课程所属分组ID)
 * @param teacherID: Int (课程负责人教师ID)
 * @param preselectedStudentsSize: Int (提前选课的学生数量)
 * @param selectedStudentsSize: Int (最终选择该课程的学生数量)
 * @param waitingListSize: Int (等待名单上的学生数量)
 */

case class CourseInfo(
  courseID: Int,
  courseCapacity: Int,
  time: List[CourseTime],
  location: String,
  courseGroupID: Int,
  teacherID: Int,
  preselectedStudentsSize: Int,
  selectedStudentsSize: Int,
  waitingListSize: Int
){

  //process class code 预留标志位，不要删除


}


case object CourseInfo{

    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[CourseInfo] = deriveEncoder
  private val circeDecoder: Decoder[CourseInfo] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[CourseInfo] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[CourseInfo] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[CourseInfo]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given courseInfoEncoder: Encoder[CourseInfo] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given courseInfoDecoder: Decoder[CourseInfo] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }



  //process object code 预留标志位，不要删除


}

