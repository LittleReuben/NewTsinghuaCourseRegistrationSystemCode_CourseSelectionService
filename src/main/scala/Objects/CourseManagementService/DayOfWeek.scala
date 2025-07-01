package Objects.CourseManagementService

import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.{DeserializationContext, JsonDeserializer, JsonSerializer, SerializerProvider}
import io.circe.{Decoder, Encoder}

@JsonSerialize(`using` = classOf[DayOfWeekSerializer])
@JsonDeserialize(`using` = classOf[DayOfWeekDeserializer])
enum DayOfWeek(val desc: String):

  override def toString: String = this.desc

  case Monday extends DayOfWeek("星期一") // 星期一
  case Tuesday extends DayOfWeek("星期二") // 星期二
  case Wednesday extends DayOfWeek("星期三") // 星期三
  case Thursday extends DayOfWeek("星期四") // 星期四
  case Friday extends DayOfWeek("星期五") // 星期五
  case Saturday extends DayOfWeek("星期六") // 星期六
  case Sunday extends DayOfWeek("星期日") // 星期日


object DayOfWeek:
  given encode: Encoder[DayOfWeek] = Encoder.encodeString.contramap[DayOfWeek](toString)

  given decode: Decoder[DayOfWeek] = Decoder.decodeString.emap(fromStringEither)

  def fromString(s: String):DayOfWeek  = s match
    case "星期一" => Monday
    case "星期二" => Tuesday
    case "星期三" => Wednesday
    case "星期四" => Thursday
    case "星期五" => Friday
    case "星期六" => Saturday
    case "星期日" => Sunday
    case _ => throw Exception(s"Unknown DayOfWeek: $s")

  def fromStringEither(s: String):Either[String, DayOfWeek]  = s match
    case "星期一" => Right(Monday)
    case "星期二" => Right(Tuesday)
    case "星期三" => Right(Wednesday)
    case "星期四" => Right(Thursday)
    case "星期五" => Right(Friday)
    case "星期六" => Right(Saturday)
    case "星期日" => Right(Sunday)
    case _ => Left(s"Unknown DayOfWeek: $s")

  def toString(t: DayOfWeek): String = t match
    case Monday => "星期一"
    case Tuesday => "星期二"
    case Wednesday => "星期三"
    case Thursday => "星期四"
    case Friday => "星期五"
    case Saturday => "星期六"
    case Sunday => "星期日"


// Jackson 序列化器
class DayOfWeekSerializer extends JsonSerializer[DayOfWeek] {
  override def serialize(value: DayOfWeek, gen: JsonGenerator, serializers: SerializerProvider): Unit = {
    gen.writeString(DayOfWeek.toString(value)) // 直接写出字符串
  }
}

// Jackson 反序列化器
class DayOfWeekDeserializer extends JsonDeserializer[DayOfWeek] {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): DayOfWeek = {
    DayOfWeek.fromString(p.getText)
  }
}

