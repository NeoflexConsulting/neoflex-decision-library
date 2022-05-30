package ru.neoflex.ndk.strategy.domain.result

final case class PersonScore(scoreFunction: String, scoreEnd: Double, cuid: Int)
final case class Score(var personScore: PersonScore = null, var details: Seq[ScoringDetails] = Seq.empty)
