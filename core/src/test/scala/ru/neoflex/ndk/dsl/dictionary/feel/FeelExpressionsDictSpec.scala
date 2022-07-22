package ru.neoflex.ndk.dsl.dictionary.feel

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{EitherValues, Inside}
import ru.neoflex.ndk.error.{FeelExpressionDoesNotExist, FeelExpressionError}

import java.time.LocalDate

class FeelExpressionsDictSpec extends AnyFlatSpec with EitherValues with Matchers with Inside {
  private val applicant = ApplicantData(Person(Some("H"), LocalDate.now().minusYears(20)), PreviousApplications(None))

  "hasEducation expression" should "result in true" in {
    val evalResult = FeelDict[Boolean]("hasEducation", "1", "applicant" -> applicant, "desiredEducation" -> "H")
    val result     = evalResult.get.value
    result should be(Some(true))
  }

  "hasEducation expression" should "result in false" in {
    val evalResult = FeelDict[Boolean]("hasEducation", "1", "applicant" -> applicant, "desiredEducation" -> "M")
    val result     = evalResult.get.value
    result should be(Some(false))
  }

  "hasEducation expression" should "result in false when education doesn't exist" in {
    val appl       = ApplicantData(Person(None, LocalDate.now()), PreviousApplications(None))
    val evalResult = FeelDict[Boolean]("hasEducation", "1", "applicant" -> appl, "desiredEducation" -> "M")
    val result     = evalResult.get.value
    result should be(Some(false))
  }

  "missing expression" should "result in error" in {
    val evalResult = FeelDict[Boolean]("nonexistent_exp", "1")
    inside(evalResult.get.left.value) {
      case FeelExpressionDoesNotExist(dictionaryName, expressionName, version) =>
        dictionaryName should be("feel_dict")
        expressionName should be("nonexistent_exp")
        version should be("1")
    }
  }

  "missing expression version" should "result in error" in {
    val evalResult = FeelDict[Boolean]("hasEducation", "2")
    inside(evalResult.get.left.value) {
      case FeelExpressionDoesNotExist(dictionaryName, expressionName, version) =>
        dictionaryName should be("feel_dict")
        expressionName should be("hasEducation")
        version should be("2")
    }
  }

  "hasEducation expression" should "result in error when not enough parameters" in {
    val evalResult = FeelDict[Boolean]("hasEducation", "1")
    inside(evalResult.get.left.value) {
      case FeelExpressionError(dictionaryName, expressionName, version, _) =>
        dictionaryName should be("feel_dict")
        expressionName should be("hasEducation")
        version should be("1")
    }
  }

  "isNewClient expression" should "result in true" in {
    val result = FeelDict[Boolean]("isNewClient", "1", "applicant" -> applicant).get.value
    result should be (Some(true))
  }

  "isNewClient expression" should "result in true with date defined" in {
    val appl = ApplicantData(Person(None, LocalDate.now()), PreviousApplications(Some(LocalDate.now().minusDays(5))))
    val result = FeelDict[Boolean]("isNewClient", "1", "applicant" -> appl).get.value
    result should be (Some(true))
  }

  "age expression" should "result in correct value" in {
    val result = FeelDict[Int]("age", "1", "applicant" -> applicant).get.value
    result should be (Some(20))
  }
}

private object FeelDict extends FeelExpressionsDictionary("feel_dict")

private final case class ApplicantData(person: Person, previousApplications: PreviousApplications)
private final case class PreviousApplications(firstDate: Option[LocalDate])
private final case class Person(education: Option[String], birthDate: LocalDate)
