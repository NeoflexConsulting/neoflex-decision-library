package ru.neoflex.ndk.testkit.func

import ru.neoflex.ndk.engine.tracking.OperatorTrackedEventRoot

final case class RunFlowTraceEvent(runId: String, event: OperatorTrackedEventRoot)
