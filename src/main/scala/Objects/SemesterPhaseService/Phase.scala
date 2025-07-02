package Objects.SemesterPhaseService

import com.fasterxml.jackson.databind.annotation.{JsonDeserialize, JsonSerialize}
import com.fasterxml.jackson.core.{JsonGenerator, JsonParser}
import com.fasterxml.jackson.databind.{DeserializationContext, JsonDeserializer, JsonSerializer, SerializerProvider}
import io.circe.{Decoder, Encoder}

@JsonSerialize(`using` = classOf[PhaseSerializer])
@JsonDeserialize(`using` = classOf[PhaseDeserializer])
enum Phase(val desc: String):

  override def toString: String = this.desc

  case Phase1 extends Phase("phase1") // phase1
  case Phase2 extends Phase("phase2") // phase2


object Phase:
  given encode: Encoder[Phase] = Encoder.encodeString.contramap[Phase](toString)

  given decode: Decoder[Phase] = Decoder.decodeString.emap(fromStringEither)

  def fromString(s: String):Phase  = s match
    case "phase1" => Phase1
    case "phase2" => Phase2
    case _ => throw Exception(s"Unknown Phase: $s")

  def fromStringEither(s: String):Either[String, Phase]  = s match
    case "phase1" => Right(Phase1)
    case "phase2" => Right(Phase2)
    case _ => Left(s"Unknown Phase: $s")

  def toString(t: Phase): String = t match
    case Phase1 => "phase1"
    case Phase2 => "phase2"


// Jackson 序列化器
class PhaseSerializer extends JsonSerializer[Phase] {
  override def serialize(value: Phase, gen: JsonGenerator, serializers: SerializerProvider): Unit = {
    gen.writeString(Phase.toString(value)) // 直接写出字符串
  }
}

// Jackson 反序列化器
class PhaseDeserializer extends JsonDeserializer[Phase] {
  override def deserialize(p: JsonParser, ctxt: DeserializationContext): Phase = {
    Phase.fromString(p.getText)
  }
}

