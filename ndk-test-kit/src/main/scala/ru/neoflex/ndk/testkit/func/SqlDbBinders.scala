package ru.neoflex.ndk.testkit.func

import cats.effect.IO
import cats.effect.IO.asyncForIO
import cats.effect.unsafe.IORuntime
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpecLike

trait SqlDbBinders extends AnyFlatSpecLike with BeforeAndAfterAll {
  protected implicit val ioRuntime: IORuntime = IORuntime.global

  private val (transactor, releaseTransactor) = {
    val dsConfig = datasourceConfig
    for {
      ec <- ExecutionContexts.fixedThreadPool(Math.max(4, Runtime.getRuntime.availableProcessors() * 2))
      t <- HikariTransactor.newHikariTransactor(
            dsConfig.driverClassName,
            dsConfig.url,
            dsConfig.user,
            dsConfig.password,
            ec
          )
    } yield t
  }.allocated.unsafeRunSync()

  protected implicit val xa: HikariTransactor[IO] = transactor

  protected def datasourceConfig: DataSourceConfig

  override def beforeAll(): Unit = super.beforeAll()

  override def afterAll(): Unit = {
    super.afterAll()
    releaseTransactor.unsafeRunSync()
  }
}

final case class DataSourceConfig(driverClassName: String, url: String, user: String = "", password: String = "")
