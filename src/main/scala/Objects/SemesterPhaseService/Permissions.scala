package Objects.SemesterPhaseService


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
 * Permissions
 * desc: 表示权限信息，定义了特定权限是否被允许
 * @param allowTeacherManage: Boolean (是否允许教师管理)
 * @param allowStudentSelect: Boolean (是否允许学生选课)
 * @param allowStudentDrop: Boolean (是否允许学生退课)
 * @param allowStudentEvaluate: Boolean (是否允许学生评价课程)
 */

case class Permissions(
  allowTeacherManage: Boolean,
  allowStudentSelect: Boolean,
  allowStudentDrop: Boolean,
  allowStudentEvaluate: Boolean
){

  //process class code 预留标志位，不要删除


}


case object Permissions{

    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[Permissions] = deriveEncoder
  private val circeDecoder: Decoder[Permissions] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[Permissions] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[Permissions] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[Permissions]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given permissionsEncoder: Encoder[Permissions] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given permissionsDecoder: Decoder[Permissions] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }



  //process object code 预留标志位，不要删除


}

