package ru.neoflex.ndk.strategy.domain.result

final case class Trial(
  name: String,
  wfDetails: WFDetails,
  var strategyName: String = "",
  var strategyFlow: String = "",
  var scoringDetails: ScoringDetails = ScoringDetails(),
  hcDetails: HCDetails = HCDetails(),
  limitDetails: LimitDetails = LimitDetails(),
  rgDetails: RGDetails = RGDetails())

final case class HCDetails(var list: String = "", var lineId: String = "")
final case class ScoringDetails(var scoreFunction: String = "", var scoreValue: Double = 0)
final case class LimitDetails(hit: Boolean = false)
final case class RGDetails(var riskGroup: Int = 0, var cutOffValue: Double = 0, var lineId: String = "")
final case class WFDetails(var decision: String, var rejectReason: String = "", var lineId: String = "")
