package Objects.CourseManagementService

import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.{DeserializationContext, JsonDeserializer, JsonSerializer, SerializerProvider}
import io.circe.{Decoder, Encoder}

@JsonSerialize(`using` = classOf[TimePeriodSerializer])
@JsonDeserialize(`using` = classOf[TimePeriodDeserializer])
enum TimePeriod(val desc: String):

  override def toString: String = this.desc

  case Morning extends TimePeriod("8:00-9:35") // 8:00-9:35
  case LateMorning extends TimePeriod("9:50-12:15") // 9:50-12:15
  case EarlyAfternoon extends TimePeriod("13:30-15:05") // 13:30-15:05
  case MidAfternoon extends TimePeriod("15:20-16:55") // 15:20-16:55
  case LateAfternoon extends TimePeriod("17:05-18:40") // 17:05-18:40
  case Evening extends TimePeriod("19:20-21:45") // 19:20-21:45


object TimePeriod:
  given encode: Encoder[TimePeriod] = Encoder.encodeString.contramap[TimePeriod](toString)

  given decode: Decoder[TimePeriod] = Decoder.decodeString.emap(fromStringEither)

  def fromString(s: String):TimePeriod  = s match
    case "8:00-9:35" => Morning
    case "9:50-12:15" => LateMorning
    case "13:30-15:05" => EarlyAfternoon
    case "15:20-16:55" => MidAfternoon
    case "17:05-18:40" => LateAfternoon
    case "19:20-21:45" => Evening
    case _ => throw Exception(s"Unknown TimePeriod: $s")

  def fromStringEither(s: String):Either[String, TimePeriod]  = s match
    case "8:00-9:35" => Right(Morning)
    case "9:50-12:15" => Right(LateMorning)
    case "13:30-15:05" => Right(EarlyAfternoon)
    case "15:20-16:55" => Right(MidAfternoon)
    case "17:05-18:40" => Right(LateAfternoon)
    case "19:20-21:45" => Right(Evening)
    case _ => Left(s"Unknown TimePeriod: $s")

  def toString(t: TimePeriod): String = t match
    case Morning => "8:00-9:35"
    case LateMorning => "9:50-12:15"
    case EarlyAfternoon => "13:30-15:05"
    case MidAfternoon => "15:20-16:55"
    case LateAfternoon => "17:05-18:40"
    case Evening => "19:20-21:45"


// Jackson 序列化器
class TimePeriodSerializer extends JsonSerializer[TimePeriod] {
  override def serialize(value: TimePeriod, gen: JsonGenerator, serializers: SerializerProvider): Unit = {
    gen.writeString(TimePeriod.toString(value)) // 直接写出字符串
  }
}

// Jackson 反序列化器
class TimePeriodDeserializer extends JsonDeserializer[TimePeriod] {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): TimePeriod = {
    TimePeriod.fromString(p.getText)
  }
}

