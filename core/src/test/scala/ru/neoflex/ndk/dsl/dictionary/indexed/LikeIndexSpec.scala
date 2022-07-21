package ru.neoflex.ndk.dsl.dictionary.indexed

import org.scalatest.EitherValues
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import ru.neoflex.ndk.dsl.dictionary.indexed.LikeIndex

import scala.util.Random

class LikeIndexSpec extends AnyFlatSpec with Matchers with EitherValues {
  "like index" should "return single match" in {
    val random = Random
    val records = List(
      SampleRecord("first"),
      SampleRecord("second"),
      SampleRecord("third"),
      SampleRecord("fourth"),
      SampleRecord("fifth")
    )
    val index          = LikeIndex[SampleRecord, SampleRecord]("name", None, records).value
    val randRecordIdx  = random.nextInt(records.length)
    val randRecordName = records(randRecordIdx).name
    val foundRecords   = index.find(randRecordName)
    foundRecords should have size 1
    foundRecords.head.value should be(records(randRecordIdx))
  }
}

private final case class SampleRecord(name: String)
