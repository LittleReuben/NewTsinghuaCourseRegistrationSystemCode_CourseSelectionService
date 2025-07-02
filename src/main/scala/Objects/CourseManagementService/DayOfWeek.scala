package Objects.CourseManagementService

import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.{DeserializationContext, JsonDeserializer, JsonSerializer, SerializerProvider}
import io.circe.{Decoder, Encoder}

@JsonSerialize(`using` = classOf[DayOfWeekSerializer])
@JsonDeserialize(`using` = classOf[DayOfWeekDeserializer])
enum DayOfWeek(val desc: String):

  override def toString: String = this.desc

  case Monday extends DayOfWeek("Monday") // Monday
  case Tuesday extends DayOfWeek("Tuesday") // Tuesday
  case Wednesday extends DayOfWeek("Wednesday") // Wednesday
  case Thursday extends DayOfWeek("Thursday") // Thursday
  case Friday extends DayOfWeek("Friday") // Friday
  case Saturday extends DayOfWeek("Saturday") // Saturday
  case Sunday extends DayOfWeek("Sunday") // Sunday


object DayOfWeek:
  given encode: Encoder[DayOfWeek] = Encoder.encodeString.contramap[DayOfWeek](toString)

  given decode: Decoder[DayOfWeek] = Decoder.decodeString.emap(fromStringEither)

  def fromString(s: String):DayOfWeek  = s match
    case "Monday" => Monday
    case "Tuesday" => Tuesday
    case "Wednesday" => Wednesday
    case "Thursday" => Thursday
    case "Friday" => Friday
    case "Saturday" => Saturday
    case "Sunday" => Sunday
    case _ => throw Exception(s"Unknown DayOfWeek: $s")

  def fromStringEither(s: String):Either[String, DayOfWeek]  = s match
    case "Monday" => Right(Monday)
    case "Tuesday" => Right(Tuesday)
    case "Wednesday" => Right(Wednesday)
    case "Thursday" => Right(Thursday)
    case "Friday" => Right(Friday)
    case "Saturday" => Right(Saturday)
    case "Sunday" => Right(Sunday)
    case _ => Left(s"Unknown DayOfWeek: $s")

  def toString(t: DayOfWeek): String = t match
    case Monday => "Monday"
    case Tuesday => "Tuesday"
    case Wednesday => "Wednesday"
    case Thursday => "Thursday"
    case Friday => "Friday"
    case Saturday => "Saturday"
    case Sunday => "Sunday"


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

