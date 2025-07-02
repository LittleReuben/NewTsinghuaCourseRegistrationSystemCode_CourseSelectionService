package Objects.CourseEvaluationService

import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.{DeserializationContext, JsonDeserializer, JsonSerializer, SerializerProvider}
import io.circe.{Decoder, Encoder}

@JsonSerialize(`using` = classOf[RatingSerializer])
@JsonDeserialize(`using` = classOf[RatingDeserializer])
enum Rating(val desc: String):

  override def toString: String = this.desc

  case One extends Rating("1") // 1
  case Two extends Rating("2") // 2
  case Three extends Rating("3") // 3
  case Four extends Rating("4") // 4
  case Five extends Rating("5") // 5


object Rating:
  given encode: Encoder[Rating] = Encoder.encodeString.contramap[Rating](toString)

  given decode: Decoder[Rating] = Decoder.decodeString.emap(fromStringEither)

  def fromString(s: String):Rating  = s match
    case "1" => One
    case "2" => Two
    case "3" => Three
    case "4" => Four
    case "5" => Five
    case _ => throw Exception(s"Unknown Rating: $s")

  def fromStringEither(s: String):Either[String, Rating]  = s match
    case "1" => Right(One)
    case "2" => Right(Two)
    case "3" => Right(Three)
    case "4" => Right(Four)
    case "5" => Right(Five)
    case _ => Left(s"Unknown Rating: $s")

  def toString(t: Rating): String = t match
    case One => "1"
    case Two => "2"
    case Three => "3"
    case Four => "4"
    case Five => "5"


// Jackson 序列化器
class RatingSerializer extends JsonSerializer[Rating] {
  override def serialize(value: Rating, gen: JsonGenerator, serializers: SerializerProvider): Unit = {
    gen.writeString(Rating.toString(value)) // 直接写出字符串
  }
}

// Jackson 反序列化器
class RatingDeserializer extends JsonDeserializer[Rating] {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): Rating = {
    Rating.fromString(p.getText)
  }
}

