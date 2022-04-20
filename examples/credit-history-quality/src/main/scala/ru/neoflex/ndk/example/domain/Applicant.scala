package ru.neoflex.ndk.example.domain

final case class Applicant(
  applicantType: String,
  externalCheck: CreditHistoryCheck,
  chEnquiryStatus: CreditHistoryEnquiryStatus)

sealed trait CreditHistoryEnquiryStatus
case object Success extends CreditHistoryEnquiryStatus
case object Failed extends CreditHistoryEnquiryStatus
case object CreditHistoryMissing extends CreditHistoryEnquiryStatus
