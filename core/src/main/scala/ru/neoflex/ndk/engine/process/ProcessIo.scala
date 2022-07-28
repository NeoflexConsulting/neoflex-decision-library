package ru.neoflex.ndk.engine.process

import ru.neoflex.ndk.NdkKeywords

import java.io.{BufferedReader, Closeable, InputStreamReader, PrintWriter}
import scala.util.{Failure, Success, Try}

trait ProcessWriter extends Closeable {
  def writeData(data: String): Try[Unit] = writeData(Seq(data))
  def writeData(data: Seq[String]): Try[Unit]
}

trait ProcessReader extends Closeable {
  def readSingleData(): Try[String] = readData().flatMap {
    case Seq(x) => Success(x)
    case _      => Failure(new IllegalStateException("No data or more than one line was read from process"))
  }
  def readData(): Try[Seq[String]]
}

class SimpleReader(process: Process) extends ProcessReader {
  private val reader = new BufferedReader(new InputStreamReader(process.getInputStream))

  override def readData(): Try[Seq[String]] = Try {
    Seq(reader.readLine())
  }

  override def close(): Unit = reader.close()
}

class SimpleWriter(process: Process) extends ProcessWriter {
  private val writer = new PrintWriter(process.getOutputStream)

  override def writeData(data: Seq[String]): Try[Unit] = Try {
    data.foreach(writer.println)
    writer.flush()
  }

  override def close(): Unit = writer.close()
}

class BatchedWriter(process: Process) extends ProcessWriter {
  private val writer = new PrintWriter(process.getOutputStream)

  override def writeData(data: Seq[String]): Try[Unit] = {
    Try {
      writer.println(NdkKeywords.BatchStartDirective)
      data.foreach(writer.println)
      writer.println(NdkKeywords.BatchEndDirective)
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
      new IllegalArgumentException(
        s"Could not read batch start directive, read data: $dataRead, process: ${process.info()}"
      )

    for {
      firstLine <- readLine()
      _         <- Either.cond(firstLine == NdkKeywords.BatchStartDirective, (), errorReadingBatchStart(firstLine)).toTry
      result    <- readUnless(NdkKeywords.BatchEndDirective)
    } yield result
  }

  override def close(): Unit = reader.close()
}

