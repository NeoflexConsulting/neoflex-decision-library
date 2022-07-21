package ru.neoflex.ndk.dsl.dictionary.feel

import cats.implicits.{ toBifunctorOps, toTraverseOps }
import io.circe.Decoder
import io.circe.generic.semiauto.deriveDecoder
import org.camunda.feel.FeelEngine
import org.camunda.feel.syntaxtree.ParsedExpression
import ru.neoflex.ndk.dsl.dictionary.{ DictionaryLoader, DictionaryValue }
import ru.neoflex.ndk.error.{ FeelExpressionError, NdkError }

import scala.reflect.ClassTag
import scala.util.Try

abstract class FeelExpressionsDictionary(dictionaryName: String, eagerLoad: Boolean = true) {
  private val engine = new FeelEngine()

  private lazy val _expressions = load()

  if (eagerLoad) {
    _expressions.fold(e => throw e.toThrowable, e => e)
  }

  def apply[T: ClassTag](name: String, version: String, parameters: (String, Any)*): DictionaryValue[T] =
    DictionaryValue[T](dictionaryName) {
      for {
        expressions     <- _expressions
        evaluatedResult <- evalExpression(expressions, name, version, parameters)
        result <- Try(evaluatedResult.map(NumberValueMapper.convertIfNumber[T](_).asInstanceOf[T])).toEither
                   .leftMap(e => FeelExpressionError(dictionaryName, name, version, e.getMessage))
      } yield result
    }

  private def evalExpression(
    expressions: FeelExpressions[ParsedExpression],
    name: String,
    version: String,
    parameters: Seq[(String, Any)]
  ): Either[NdkError, Option[Any]] = {
    expressions.value.get(name).flatMap(_.versions.get(version)) match {
      case Some(expression) =>
        engine
          .eval(expression, parameters.toMap)
          .leftMap(f => FeelExpressionError(dictionaryName, name, version, f.message))
          .map(Option.apply)
      case None => Right(None)
    }
  }

  private def load(): Either[NdkError, FeelExpressions[ParsedExpression]] = {
    for {
      expByName         <- DictionaryLoader.loadDictionary[Map[String, FeelExpressionVersions[String]]](dictionaryName, "table")
      parsedExpressions <- parse(expByName)
    } yield parsedExpressions
  }

  private def parse(
    expByName: Map[String, FeelExpressionVersions[String]]
  ): Either[NdkError, FeelExpressions[ParsedExpression]] = {
    expByName.map {
      case (name, expVersions) =>
        expVersions.versions.map {
          case (version, rawExpression) =>
            engine.parseExpression(rawExpression).map(e => (version, e)).leftMap { f =>
              FeelExpressionError(dictionaryName, name, version, f.message)
            }
        }.toList.sequence
          .map(_.toMap)
          .map(FeelExpressionVersions.apply)
          .map(v => (name, v))
    }.toList.sequence
      .map(_.toMap)
      .map(FeelExpressions.apply)

  }
}

final case class FeelExpressionVersions[E](versions: Map[String, E])
final case class FeelExpressions[E](value: Map[String, FeelExpressionVersions[E]])

object FeelExpressionVersions {
  implicit def decoder[E: Decoder]: Decoder[FeelExpressionVersions[E]] = deriveDecoder[FeelExpressionVersions[E]]
}
