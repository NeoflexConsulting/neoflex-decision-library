package ru.neoflex.ndk.tracker.config

sealed trait TrackingEventSendType
case object FireAndForget extends TrackingEventSendType
final case class WaitResponse(ignoreErrors: Boolean = false) extends TrackingEventSendType
