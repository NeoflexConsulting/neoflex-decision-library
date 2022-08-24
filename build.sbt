import sbt.Keys.libraryDependencies
import Dependencies._

ThisBuild / credentials += Credentials(Path.userHome / ".sbt" / ".credentials")
ThisBuild / organization := "ru.neoflex.ndk"
ThisBuild / scalaVersion := "2.13.4"

lazy val root = (project in file("."))
  .settings(
    publish / skip := true
  )
  .aggregate(core, testKit, ndkRenderer, underwritingExample, creditHistoryQualityExample)

lazy val core = artifactModule("neoflex-decision-kit", "core")
  .settings(
    name := "neoflex-decision-kit",
    libraryDependencies += CatsCore,
    libraryDependencies += "org.slf4j"      % "slf4j-api"       % "1.7.9",
    libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.11",
    libraryDependencies += Http4s.Dsl,
    libraryDependencies += Http4s.EmberClient,
    libraryDependencies += Http4s.Circe,
    libraryDependencies += CirceGeneric,
    libraryDependencies += Pureconfig,
    libraryDependencies += FeelEngine,
    libraryDependencies += CirceYaml,
    libraryDependencies += Scalatest % Test
  )

lazy val preparePythonVenv     = taskKey[Unit]("Prepare python virtual environment")
lazy val initializePythonVenv  = taskKey[Unit]("Initialize python virtual environment")
lazy val installPythonPackages = taskKey[Unit]("Install python required packages")

import sys.process._

lazy val pythonExtension = artifactModule("ndk-python-extension", "ndk-python-extension")
  .settings(
    name := "ndk-python-extension",
    libraryDependencies += "org.graalvm.sdk" % "graal-sdk" % "22.0.0",
    initializePythonVenv := {
      val log         = streams.value.log
      val javaHome    = sys.env("JAVA_HOME")
      val graalpython = s"$javaHome/bin/graalpython"
      s"$graalpython -m venv venv" ! log
    },
    installPythonPackages := {
      val log = streams.value.log
      s"venv/bin/pip install mlflow" ! log
    },
    preparePythonVenv := Def.sequential(initializePythonVenv, installPythonPackages).value
  )
  .dependsOn(core)

lazy val testKit = artifactModule("ndk-test-kit", "ndk-test-kit")
  .settings(
    name := "ndk-test-kit",
    resolvers ++= Repositories.resolvers,
    libraryDependencies ++= Seq(
      Scalatest,
      Internal.Core,
      CirceParser,
      Akka.Stream,
      Akka.AlpakkaCsv,
      Purecsv,
      Doobie.Core,
      Doobie.Hikari,
      Doobie.Specs2,
      Nanoid,
      "com.h2database" % "h2" % "2.1.214" % Test
    ),
    tpolecatCiModeOptions ~= { options =>
      options.filterNot(Set(ScalacOptions.warnValueDiscard, ScalacOptions.privateWarnValueDiscard))
    }
  )

lazy val ndkRenderer = artifactModule("ndk-renderer", "ndk-renderer")
  .settings(
    name := "ndk-renderer",
    resolvers ++= Repositories.resolvers,
    libraryDependencies ++= Seq(
      "org.scala-lang"         % "scala-reflect" % scalaVersion.value,
      "org.scala-lang.modules" %% "scala-xml"    % "2.1.0",
      Internal.Core
    )
  )

lazy val ndkTrackerKafka = artifactModule("ndk-tracker-kafka", "ndk-tracker-kafka")
  .settings(
    name := "ndk-tracker-kafka",
    resolvers ++= Repositories.resolvers,
    libraryDependencies ++= Seq(
      Internal.Core,
      KafkaClients
    )
  )

lazy val ndkIntegrationRest = artifactModule("ndk-integration-rest", "integration/rest")
  .settings(
    name := "ndk-integration-rest",
    resolvers ++= Repositories.resolvers,
    libraryDependencies ++= Seq(
      Internal.Core,
      Akka.Http,
      Akka.Actor,
      Akka.Stream,
      CirceParser
    )
  )

lazy val ndkIntegrationKafka = artifactModule("ndk-integration-kafka", "integration/kafka")
  .settings(
    name := "ndk-integration-kafka",
    resolvers ++= Repositories.resolvers,
    libraryDependencies ++= Seq(
      Internal.Core,
      Akka.Stream,
      CirceParser,
      Akka.StreamKafka
    )
  )

lazy val integrationExamples = (project in file("integration/examples"))
  .settings(
    name := "integration-examples",
    version := "0.0.1-SNAPSHOT",
    publish / skip := true
  )
  .dependsOn(ndkIntegrationRest, ndkIntegrationKafka, ndkTrackerKafka)

lazy val underwritingExample = (project in file("examples/underwriting"))
  .settings(
    name := "underwriting-example",
    version := "0.0.1-SNAPSHOT",
    publish / skip := true
  )
  .dependsOn(core, ndkRenderer)

lazy val creditHistoryQualityExample = (project in file("examples/credit-history-quality"))
  .settings(
    name := "credit-history-quality-example",
    version := "0.0.1-SNAPSHOT",
    publish / skip := true
  )
  .dependsOn(core, ndkRenderer)

lazy val approvalStrategyExample = (project in file("examples/approval-strategy"))
  .settings(
    name := "approval-strategy-example",
    version := "0.0.1-SNAPSHOT",
    publish / skip := true,
    tpolecatCiModeOptions ~= { options =>
      options.filterNot(Set(ScalacOptions.warnValueDiscard, ScalacOptions.privateWarnValueDiscard))
    }
  )
  .dependsOn(core, ndkRenderer, ndkTrackerKafka)

lazy val restModelExample = (project in file("examples/rest-model"))
  .settings(
    name := "rest-model-example",
    version := "0.0.1-SNAPSHOT",
    publish / skip := true
  )
  .dependsOn(core, ndkRenderer)

lazy val pyModelExample = (project in file("examples/py-model"))
  .settings(
    name := "py-model-example",
    version := "0.0.1-SNAPSHOT",
    publish / skip := true
  )
  .dependsOn(core, ndkRenderer)

def artifactModule(id: String, baseDir: String) =
  Project(id = id, base = file(baseDir)).settings(Settings.artifactSettings(baseDir))
