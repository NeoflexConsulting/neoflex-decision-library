package ru.neoflex.ndk.dictionary

import ru.neoflex.ndk.strategy.domain.{ ApplicantData, Application, Person }

import java.time.{ LocalDate, LocalDateTime }

object DictionariesApp extends App {
  val application = Application(
    ApplicantData(
      Person(
        1,
        1,
        1,
        LocalDate.ofYearDay(1990, 1),
        None,
        null
      ),
      null
    ),
    null,
    LocalDateTime.now(),
    null
  )

  println(Predictors[Int]("age", "1", "application" -> application))
  println(RegionTable("80") == 3)
  println(RegionTable("Красноярский", "Норильск") == 2)

}
