package ru.neoflex.ndk.dsl.dictionary.indexed

import cats.implicits.catsSyntaxOptionId
import io.circe.JsonNumber

import scala.reflect.{ classTag, ClassTag }

object JsonNumberConverter {
  def convert[V: ClassTag](v: Any)(orElse: Any => Any): Option[Any] = {
    if (v != null && v.isInstanceOf[JsonNumber]) {
      val jn = v.asInstanceOf[JsonNumber]
      classTag[V] match {
        case ClassTag.Byte                                                  => jn.toByte
        case ClassTag.Short                                                 => jn.toShort
        case ClassTag.Int                                                   => jn.toInt
        case ClassTag.Long                                                  => jn.toLong
        case ClassTag.Float                                                 => jn.toFloat.some
        case ClassTag.Double                                                => jn.toDouble.some
        case ClassTag(value) if value.isAssignableFrom(classOf[BigDecimal]) => jn.toBigDecimal
        case ClassTag(value) if value.isAssignableFrom(classOf[BigInt])     => jn.toBigInt
        case _                                                              => jn.toInt
      }
    } else {
      orElse(v).some
    }
  }
}
