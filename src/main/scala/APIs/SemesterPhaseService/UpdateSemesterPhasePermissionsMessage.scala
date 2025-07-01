package APIs.SemesterPhaseService

import Common.API.API
import Global.ServiceCenter.SemesterPhaseServiceCode

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
import Objects.SemesterPhaseService.Permissions

/**
 * UpdateSemesterPhasePermissionsMessage
 * desc: 更新学期阶段操作权限设置的接口
 * @param adminToken: String (管理员验证身份的Token)
 * @param allowTeacherManage: Boolean (是否允许教师管理课程组和课程)
 * @param allowStudentSelect: Boolean (是否允许学生选课)
 * @param allowStudentDrop: Boolean (是否允许学生退课)
 * @param allowStudentEvaluate: Boolean (是否允许学生评价课程)
 * @return updatedPermissions: Permissions (更新后的阶段权限设置，包含所有权限字段)
 */

case class UpdateSemesterPhasePermissionsMessage(
  adminToken: String,
  allowTeacherManage: Boolean,
  allowStudentSelect: Boolean,
  allowStudentDrop: Boolean,
  allowStudentEvaluate: Boolean
) extends API[Permissions](SemesterPhaseServiceCode)



case object UpdateSemesterPhasePermissionsMessage{
    
  import Common.Serialize.CustomColumnTypes.{decodeDateTime,encodeDateTime}

  // Circe 默认的 Encoder 和 Decoder
  private val circeEncoder: Encoder[UpdateSemesterPhasePermissionsMessage] = deriveEncoder
  private val circeDecoder: Decoder[UpdateSemesterPhasePermissionsMessage] = deriveDecoder

  // Jackson 对应的 Encoder 和 Decoder
  private val jacksonEncoder: Encoder[UpdateSemesterPhasePermissionsMessage] = Encoder.instance { currentObj =>
    Json.fromString(JacksonSerializeUtils.serialize(currentObj))
  }

  private val jacksonDecoder: Decoder[UpdateSemesterPhasePermissionsMessage] = Decoder.instance { cursor =>
    try { Right(JacksonSerializeUtils.deserialize(cursor.value.noSpaces, new TypeReference[UpdateSemesterPhasePermissionsMessage]() {})) } 
    catch { case e: Throwable => Left(io.circe.DecodingFailure(e.getMessage, cursor.history)) }
  }
  
  // Circe + Jackson 兜底的 Encoder
  given updateSemesterPhasePermissionsMessageEncoder: Encoder[UpdateSemesterPhasePermissionsMessage] = Encoder.instance { config =>
    Try(circeEncoder(config)).getOrElse(jacksonEncoder(config))
  }

  // Circe + Jackson 兜底的 Decoder
  given updateSemesterPhasePermissionsMessageDecoder: Decoder[UpdateSemesterPhasePermissionsMessage] = Decoder.instance { cursor =>
    circeDecoder.tryDecode(cursor).orElse(jacksonDecoder.tryDecode(cursor))
  }


}

