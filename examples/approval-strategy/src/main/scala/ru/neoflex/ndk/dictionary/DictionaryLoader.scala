package ru.neoflex.ndk.dictionary

import java.io.InputStreamReader
import scala.util.Using

object DictionaryLoader {
  def load(dictionaryName: String): Map[String, Map[String, Map[String, String]]] = {
    Using(getClass.getResourceAsStream(s"/$dictionaryName.yaml")) { resource =>
      io.circe.yaml.parser.parse(new InputStreamReader(resource))
    }.toEither.joinRight.flatMap { json =>
      json.hcursor.downField("table").as[Map[String, Map[String, Map[String, String]]]]
    }.fold(throw _, j => j)
  }
}
