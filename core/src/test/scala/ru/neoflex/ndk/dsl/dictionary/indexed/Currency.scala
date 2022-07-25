package ru.neoflex.ndk.dsl.dictionary.indexed

import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder

final case class Currency(alpha: String, numeric: Int, name: String, country: String)

object Currency {
  implicit val decoder: Decoder[Currency] = deriveDecoder[Currency]
}

final case class CurrencyShort(alpha: String, numeric: Int)
object CurrencyShort {
  implicit val decoder: Decoder[CurrencyShort] = deriveDecoder[CurrencyShort]
}
