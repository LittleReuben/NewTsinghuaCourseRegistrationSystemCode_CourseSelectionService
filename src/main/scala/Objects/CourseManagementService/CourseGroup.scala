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


/**
 * CourseGroup
 * desc: 课程分组实体信息，为课程管理服务的一部分
 * @param courseGroupID: Int (课程分组的唯一ID)
 * @param name: String (课程分组的名称)
 * @param credit: Int (课程分组的学分)
 * @param ownerTeacherID: Int (课程分组的负责人ID)
 * @param authorizedTeachers: Int (获授权管理该课程分组的教师ID列表)
 */

case class CourseGroup(
  courseGroupID: Int,
  name: String,
  credit: Int,
  ownerTeacherID: Int,
  authorizedTeachers: List[Int]
){

  //process class code 预留标志位，不要删除


}


case object CourseGroup{

    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[CourseGroup] = deriveEncoder
  private val circeDecoder: Decoder[CourseGroup] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[CourseGroup] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[CourseGroup] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[CourseGroup]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given courseGroupEncoder: Encoder[CourseGroup] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given courseGroupDecoder: Decoder[CourseGroup] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }



  //process object code 预留标志位，不要删除


}

