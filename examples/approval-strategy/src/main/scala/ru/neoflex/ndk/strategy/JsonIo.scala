package ru.neoflex.ndk.strategy

import io.circe.Printer
import io.circe.generic.auto._
import io.circe.syntax._
import ru.neoflex.ndk.strategy.domain.Application
import ru.neoflex.ndk.strategy.domain.result.ScoringResult

import java.nio.file.{ Files, Paths }
import scala.io.Source
import scala.util.Using
import io.circe.parser.decode

trait JsonIo {
  def readApplication(resourceFilename: String): Application = {
    val is = getClass.getResourceAsStream(s"/$resourceFilename")
    Using(is) { input =>
      val json = Source.fromInputStream(input).mkString
      decode[Application](json).toTry
    }.flatten.fold(throw _, a => a)
  }

  def writeResult(result: ScoringResult, filename: String): Unit = {
    val json = result.asJson.printWith(Printer.spaces2)
    Using(Files.newBufferedWriter(Paths.get(filename))) { w =>
      w.write(json)
    }
  }
}
