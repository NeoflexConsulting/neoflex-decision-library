package ru.neoflex.ndk.testkit

import org.scalactic.source
import ru.neoflex.ndk.dsl.FlowOp

trait NdkSpecLike extends FlowMatchers {
  implicit class FlowOperatorTestingOps(op: FlowOp) extends FlowTestingRunner {
    def run(after: NdkExecutionContext => Unit)(implicit pos: source.Position): Unit = {
      val ctx = run(op)
      after(ctx)
    }
  }

  implicit class ContextMatcherOps(ctx: NdkExecutionContext) {
    def flow(id: String): OperatorMatcher      = new OperatorMatcher(id, OperatorType.Flow, ctx)
    def action(id: String): OperatorMatcher    = new OperatorMatcher(id, OperatorType.Action, ctx)
    def rule(id: String): OperatorMatcher      = new OperatorMatcher(id, OperatorType.Rule, ctx)
    def gateway(id: String): OperatorMatcher   = new OperatorMatcher(id, OperatorType.Gateway, ctx)
    def table(id: String): OperatorMatcher     = new OperatorMatcher(id, OperatorType.Table, ctx)
    def forEach(id: String): OperatorMatcher   = new OperatorMatcher(id, OperatorType.ForEach, ctx)
    def whileLoop(id: String): OperatorMatcher = new OperatorMatcher(id, OperatorType.While, ctx)
  }

  class OperatorMatcher(id: String, operatorType: OperatorType, ctx: NdkExecutionContext) {
    def has(matcher: Matcher)(implicit pos: source.Position): Unit = matcher.assertMatch(id, operatorType, ctx.tracker)
  }
}
