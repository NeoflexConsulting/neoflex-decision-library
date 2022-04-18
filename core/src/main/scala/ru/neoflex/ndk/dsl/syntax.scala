package ru.neoflex.ndk.dsl

import ru.neoflex.ndk.error.ErrorSyntax

object syntax
    extends TableSyntax
    with GatewaySyntax
    with RuleSyntax
    with FlowSyntax
    with WhileSyntax
    with ActionSyntax
    with IterateSyntax
    with ErrorSyntax
