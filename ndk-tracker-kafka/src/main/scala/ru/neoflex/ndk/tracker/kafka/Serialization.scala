package ru.neoflex.ndk.tracker.kafka

import io.circe.Printer
import org.apache.kafka.common.serialization.{Serializer, StringSerializer}
import ru.neoflex.ndk.dsl.`type`.OperatorType
import ru.neoflex.ndk.engine.tracking.{ExecutionError, OperatorTrackedEvent, OperatorTrackedEventRoot}
import ru.neoflex.ndk.tracker.kafka.JsonSerialization._

import java.nio.charset.StandardCharsets

object Serialization {
  implicit val string: Serializer[String] = new StringSerializer
  implicit val operatorTrackedEventRootJson: Serializer[OperatorTrackedEventRoot] = {
    (_: String, data: OperatorTrackedEventRoot) =>
      Printer.spaces2.print(operatorTrackedEventRootEncoder(data)).getBytes(StandardCharsets.UTF_8)
  }
}

object JsonSerialization {
  import io.circe.Encoder
  import io.circe.generic.semiauto._

  implicit val operatorTypeEncoder: Encoder[OperatorType] = Encoder.instance[OperatorType] { operatorType =>
    Encoder.encodeString(operatorType.toString)
  }
  implicit val executionErrorEncoder: Encoder[ExecutionError] = deriveEncoder[ExecutionError]
  implicit val operatorTrackedEventEncoder: Encoder[OperatorTrackedEvent] = deriveEncoder[OperatorTrackedEvent]
  implicit val operatorTrackedEventRootEncoder: Encoder[OperatorTrackedEventRoot] =
    deriveEncoder[OperatorTrackedEventRoot]
}
