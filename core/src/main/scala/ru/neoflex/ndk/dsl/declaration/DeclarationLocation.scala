package ru.neoflex.ndk.dsl.declaration

import org.slf4j.LoggerFactory
import ru.neoflex.ndk.dsl._

import java.lang.{ Boolean => JBoolean }
import scala.jdk.OptionConverters.RichOptional

trait DeclarationLocationSupport {
  val declarationLocation: Option[DeclarationLocation] = DeclarationLocation(this)
}

final case class DeclarationLocation(fileName: String, lineNumber: Int)

object DeclarationLocation {
  private val logger                             = LoggerFactory.getLogger(getClass)
  private val collectDeclarationLocationProperty = "ru.neoflex.ndk.collectDeclarationLocation"
  private val collectDeclarationLocation         = JBoolean.getBoolean(collectDeclarationLocationProperty)

  logger.info(s"$collectDeclarationLocationProperty=$collectDeclarationLocation")

  def apply(obj: Any): Option[DeclarationLocation] = obj match {
    case _ if !collectDeclarationLocation => None
    case op: FlowOp if !op.isEmbedded     => None
    case _                                => apply()
  }

  private def apply(): Option[DeclarationLocation] = {
    StackWalker.getInstance().walk { frames =>
      frames.filter { frame =>
        NdkInternals.nonEmbeddedClass(frame.getClassName)
      }.findFirst().toScala.map { frame =>
        DeclarationLocation(frame.getFileName, frame.getLineNumber)
      }
    }
  }
}
