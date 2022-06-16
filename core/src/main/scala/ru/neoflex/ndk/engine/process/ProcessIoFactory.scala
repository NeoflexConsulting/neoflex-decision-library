package ru.neoflex.ndk.engine.process

sealed trait ProcessIoFactory {
  def createReader(process: Process): ProcessReader
  def createWriter(process: Process): ProcessWriter
}

object BatchedIoFactory extends ProcessIoFactory {
  override def createReader(process: Process): ProcessReader = new BatchedReader(process)
  override def createWriter(process: Process): ProcessWriter = new BatchedWriter(process)
}

object SimpleIoFactory extends ProcessIoFactory {
  override def createReader(process: Process): ProcessReader = new SimpleReader(process)
  override def createWriter(process: Process): ProcessWriter = new SimpleWriter(process)
}
