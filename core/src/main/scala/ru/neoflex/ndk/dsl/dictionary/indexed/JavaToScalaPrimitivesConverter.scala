package ru.neoflex.ndk.dsl.dictionary.indexed

import scala.reflect.{ classTag, ClassTag }

object JavaToScalaPrimitivesConverter {
  def convert[V: ClassTag](v: Any)(orElse: Any => Any): Any = {
    classTag[V] match {
      case ClassTag.Byte if v.isInstanceOf[java.lang.Byte] =>
        Byte.unbox(v.asInstanceOf[java.lang.Byte])
      case ClassTag.Boolean if v.isInstanceOf[java.lang.Boolean] =>
        Boolean.unbox(v.asInstanceOf[java.lang.Boolean])
      case ClassTag.Char if v.isInstanceOf[java.lang.Character] =>
        Char.unbox(v.asInstanceOf[java.lang.Character])
      case ClassTag.Short if v.isInstanceOf[java.lang.Short] =>
        Short.unbox(v.asInstanceOf[java.lang.Short])
      case ClassTag.Int if v.isInstanceOf[java.lang.Integer] =>
        Int.unbox(v.asInstanceOf[java.lang.Integer])
      case ClassTag.Long if v.isInstanceOf[java.lang.Long] =>
        Long.unbox(v.asInstanceOf[java.lang.Long])
      case ClassTag.Float if v.isInstanceOf[java.lang.Float] =>
        Float.unbox(v.asInstanceOf[java.lang.Float])
      case ClassTag.Double if v.isInstanceOf[java.lang.Double] =>
        Double.unbox(v.asInstanceOf[java.lang.Double])
      case _ => orElse(v)
    }
  }
}
