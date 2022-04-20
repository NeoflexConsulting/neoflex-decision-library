package ru.neoflex.ndk.example.domain

case class CheckMarks(
  var isCRECH: Mark = NoMark,
  var curDelCRE: Mark = NoMark,
  var maxDelCRE: Mark = NoMark,
  var totalSumCRE: Mark = NoMark,
  var badDebtCRE: Mark = NoMark) {

  def isAllMarksWhite: Boolean = Set(isCRECH, curDelCRE, maxDelCRE, totalSumCRE, badDebtCRE) == Set(White)
}

sealed trait Mark
case object Yellow extends Mark
case object White  extends Mark
case object Blue   extends Mark
case object NoMark extends Mark
