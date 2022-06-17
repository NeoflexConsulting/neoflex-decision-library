package ru.neoflex.ndk.engine.process

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

class NamedThreadFactory(poolName: String) extends ThreadFactory {
  private val group =
    Option(System.getSecurityManager).map(_.getThreadGroup).getOrElse(Thread.currentThread.getThreadGroup)
  private val threadNumber = new AtomicInteger(1)

  override def newThread(r: Runnable): Thread = {
    val t = new Thread(group, r, s"$poolName-${threadNumber.getAndIncrement()}", 0)
    t.setDaemon(true)
    t.setPriority(Thread.NORM_PRIORITY)
    t
  }
}
