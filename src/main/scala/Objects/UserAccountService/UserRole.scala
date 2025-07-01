package Objects.UserAccountService

import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.{DeserializationContext, JsonDeserializer, JsonSerializer, SerializerProvider}
import io.circe.{Decoder, Encoder}

@JsonSerialize(`using` = classOf[UserRoleSerializer])
@JsonDeserialize(`using` = classOf[UserRoleDeserializer])
enum UserRole(val desc: String):

  override def toString: String = this.desc

  case SuperAdmin extends UserRole("超级管理员角色") // 超级管理员角色
  case Teacher extends UserRole("教师角色") // 教师角色
  case Student extends UserRole("学生角色") // 学生角色


object UserRole:
  given encode: Encoder[UserRole] = Encoder.encodeString.contramap[UserRole](toString)

  given decode: Decoder[UserRole] = Decoder.decodeString.emap(fromStringEither)

  def fromString(s: String):UserRole  = s match
    case "超级管理员角色" => SuperAdmin
    case "教师角色" => Teacher
    case "学生角色" => Student
    case _ => throw Exception(s"Unknown UserRole: $s")

  def fromStringEither(s: String):Either[String, UserRole]  = s match
    case "超级管理员角色" => Right(SuperAdmin)
    case "教师角色" => Right(Teacher)
    case "学生角色" => Right(Student)
    case _ => Left(s"Unknown UserRole: $s")

  def toString(t: UserRole): String = t match
    case SuperAdmin => "超级管理员角色"
    case Teacher => "教师角色"
    case Student => "学生角色"


// Jackson 序列化器
class UserRoleSerializer extends JsonSerializer[UserRole] {
  override def serialize(value: UserRole, gen: JsonGenerator, serializers: SerializerProvider): Unit = {
    gen.writeString(UserRole.toString(value)) // 直接写出字符串
  }
}

// Jackson 反序列化器
class UserRoleDeserializer extends JsonDeserializer[UserRole] {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): UserRole = {
    UserRole.fromString(p.getText)
  }
}

