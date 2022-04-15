package ru.neoflex.ndk.tools

import org.slf4j.{Logger, LoggerFactory}

trait Logging {
  protected lazy val logger: Logger = LoggerFactory.getLogger(getClass)
}
