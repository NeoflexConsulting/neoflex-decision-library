package ru.neoflex.ndk.strategy.flow

import ru.neoflex.ndk.dsl.Table
import ru.neoflex.ndk.dsl.ImplicitConversions.stringToOption
import ru.neoflex.ndk.dsl.syntax.expressions
import ru.neoflex.ndk.strategy.domain.result.Trial

final case class HCTable(trial: Trial)
    extends Table("HCTable-t-1", "HCTable", {
      expressions(
        Seq.empty[Table.Expression]: _*
      ) andConditions (
        // TODO What are the conditions?
        Seq.empty[Table.Condition]: _*
      )
    })
