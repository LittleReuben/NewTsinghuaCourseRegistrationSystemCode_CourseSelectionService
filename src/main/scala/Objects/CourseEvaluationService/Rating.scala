package Objects.CourseEvaluationService

import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.{DeserializationContext, JsonDeserializer, JsonSerializer, SerializerProvider}
import io.circe.{Decoder, Encoder}

@JsonSerialize(`using` = classOf[RatingSerializer])
@JsonDeserialize(`using` = classOf[RatingDeserializer])
enum Rating(val desc: String):

  override def toString: String = this.desc

  case One extends Rating("评分为1分") // 评分为1分
  case Two extends Rating("评分为2分") // 评分为2分
  case Three extends Rating("评分为3分") // 评分为3分
  case Four extends Rating("评分为4分") // 评分为4分
  case Five extends Rating("评分为5分") // 评分为5分


object Rating:
  given encode: Encoder[Rating] = Encoder.encodeString.contramap[Rating](toString)

  given decode: Decoder[Rating] = Decoder.decodeString.emap(fromStringEither)

  def fromString(s: String):Rating  = s match
    case "评分为1分" => One
    case "评分为2分" => Two
    case "评分为3分" => Three
    case "评分为4分" => Four
    case "评分为5分" => Five
    case _ => throw Exception(s"Unknown Rating: $s")

  def fromStringEither(s: String):Either[String, Rating]  = s match
    case "评分为1分" => Right(One)
    case "评分为2分" => Right(Two)
    case "评分为3分" => Right(Three)
    case "评分为4分" => Right(Four)
    case "评分为5分" => Right(Five)
    case _ => Left(s"Unknown Rating: $s")

  def toString(t: Rating): String = t match
    case One => "评分为1分"
    case Two => "评分为2分"
    case Three => "评分为3分"
    case Four => "评分为4分"
    case Five => "评分为5分"


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

