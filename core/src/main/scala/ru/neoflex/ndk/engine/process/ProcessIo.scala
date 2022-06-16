package ru.neoflex.ndk.engine.process

import java.io.{BufferedReader, Closeable, InputStreamReader, PrintWriter}
import scala.util.Try

trait ProcessWriter extends Closeable {
  def writeData(data: Seq[String]): Try[Unit]
}

trait ProcessReader extends Closeable {
  def readData(): Try[Seq[String]]
}

sealed trait ProcessIoFactory {
  def createReader(process: Process): ProcessReader
  def createWriter(process: Process): ProcessWriter
}

object BatchedIoFactory extends ProcessIoFactory {
  override def createReader(process: Process): ProcessReader = new BatchedReader(process)
  override def createWriter(process: Process): ProcessWriter = new BatchedWriter(process)
}

class BatchedWriter(process: Process) extends ProcessWriter {
  private val writer = new PrintWriter(process.getOutputStream)

  override def writeData(data: Seq[String]): Try[Unit] = {
    Try {
      writer.println(ProtocolSyntax.BatchStartDirective)
      data.foreach(writer.println)
      writer.println(ProtocolSyntax.BatchEndDirective)
      writer.flush()
    }
  }

  override def close(): Unit = writer.close()
}

class BatchedReader(process: Process) extends ProcessReader {
  private val reader = new BufferedReader(new InputStreamReader(process.getInputStream))

  private def readLine(): Try[String] = Try(reader.readLine())

  private def readUnless(endMark: String): Try[Seq[String]] = {
    var line: String = null
    val rb           = Seq.newBuilder[String]
    Try {
      while ({ line = reader.readLine(); line } != endMark) {
        rb += line
      }
      rb.result()
    }
  }

  override def readData(): Try[Seq[String]] = {
    def errorReadingBatchStart(dataRead: String) =
      new IllegalArgumentException(s"Could not read batch start mark, read data: $dataRead, process: ${process.info()}")

    for {
      firstLine <- readLine()
      _         <- Either.cond(firstLine == ProtocolSyntax.BatchStartDirective, (), errorReadingBatchStart(firstLine)).toTry
      result    <- readUnless(ProtocolSyntax.BatchEndDirective)
    } yield result
  }

  override def close(): Unit = reader.close()
}

object ProtocolSyntax {
  val BatchStartDirective = "__ndk_bs"
  val BatchEndDirective   = "__ndk_be"
}
