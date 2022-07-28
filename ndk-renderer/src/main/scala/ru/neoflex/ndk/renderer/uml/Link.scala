package ru.neoflex.ndk.renderer.uml

import cats.syntax.either._

sealed trait Link {
  def stringValue: String
}
final case class ClassLink(className: String) extends Link {
  override def stringValue: String = s"c:$className"
}
final case class FileLineNumber(filename: String, lineNumber: Int) extends Link {
  override def stringValue: String = s"f:$filename:$lineNumber"
}
final case class DictionaryLink(filename: String, fileExt: String, keyword: Option[String]) extends Link {
  override def stringValue: String = keyword.map(k => s"d:$filename.$fileExt:$k").getOrElse(s"d:$filename.$fileExt")
}
object DictionaryLink {
  private val dictionaryFileRegex = "d:(.+)\\.([A-z\\d]+)(:(.+))?".r

  def apply(text: String): Either[Throwable, DictionaryLink] = text match {
    case dictionaryFileRegex(filename, fileExt, _, keyword) =>
      Either.right(DictionaryLink(filename, fileExt, Option(keyword)))
    case _ =>
      Either.left(new IllegalArgumentException(s"Pattern for dictionary input string is unknown: $text"))
  }
}

object Link {
  private val fileLineNumberRegex = "f:(.+):(.+)".r

  def apply(text: String): Either[Throwable, Link] = {
    if (text.startsWith("c:")) {
      Right(ClassLink(text.substring(2)))
    } else if (text.startsWith("f:")) {
      text match {
        case fileLineNumberRegex(filename, lineNumber) =>
          Either.right(FileLineNumber(filename, lineNumber.toInt))
        case _ =>
          Either.left(new IllegalArgumentException(s"Pattern for file input string is unknown: $text"))
      }
    } else if (text.startsWith("d:")) {
      DictionaryLink(text)
    } else {
      Either.left(new IllegalArgumentException(s"Pattern for input string is unknown: $text"))
    }
  }
}
