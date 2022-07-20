package ru.neoflex.ndk.dsl.dictionary.feel

import scala.reflect.{ classTag, ClassTag }

object NumberValueMapper {
  def convertIfNumber[T: ClassTag](v: Any): Any = {
    if (v != null && classOf[BigDecimal].isInstance(v)) {
      val number = v.asInstanceOf[BigDecimal]
      classTag[T] match {
        case ClassTag.Short                           => number.shortValue
        case ClassTag.Int                             => number.intValue
        case ClassTag.Long                            => number.longValue
        case ClassTag.Float                           => number.floatValue
        case ClassTag.Double                          => number.doubleValue
        case ct if ct.runtimeClass == classOf[BigInt] => number.toBigInt
        case _                                        => v
      }
    }
  }
}
