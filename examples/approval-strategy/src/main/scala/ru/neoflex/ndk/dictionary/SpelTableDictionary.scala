package ru.neoflex.ndk.dictionary

import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext

import scala.reflect.ClassTag

abstract class SpelTableDictionary(dictionaryName: String) extends TableDictionary(dictionaryName) {
  override def apply[T](keys: Any*): T = {
    val key  = keys(0)
    val expr = table(key.toString)
    println(expr)
    null.asInstanceOf[T]
  }

  def apply[T: ClassTag](name: String, version: String, parameters: (String, Any)*): T = {
    val versions         = table(name)("versions")
    val expr             = versions(version)
    val parsedExpression = new SpelExpressionParser().parseRaw(expr)
    val ctx              = new StandardEvaluationContext()
    parameters.foreach {
      case (name, value) =>
        ctx.setVariable(name, value)
    }
    val clazz = implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]]
    parsedExpression.getValue(ctx, clazz)
  }
}
