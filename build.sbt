import sbt.Keys.libraryDependencies

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
    libraryDependencies += "org.typelevel"         %% "cats-core"           % "2.7.0",
    libraryDependencies += "org.slf4j"             % "slf4j-api"            % "1.7.9",
    libraryDependencies += "ch.qos.logback"        % "logback-classic"      % "1.2.11",
    libraryDependencies += "org.http4s"            %% "http4s-dsl"          % "0.23.12",
    libraryDependencies += "org.http4s"            %% "http4s-ember-client" % "0.23.12",
    libraryDependencies += "org.http4s"            %% "http4s-circe"        % "0.23.12",
    libraryDependencies += "io.circe"              %% "circe-generic"       % "0.14.2",
    libraryDependencies += "com.github.pureconfig" %% "pureconfig"          % "0.17.1"
  )

lazy val preparePythonVenv     = taskKey[Unit]("Prepare python virtual environment")
lazy val initializePythonVenv  = taskKey[Unit]("Initialize python virtual environment")
lazy val installPythonPackages = taskKey[Unit]("Install python required packages")

import sys.process._

lazy val pythonExtension = artifactModule("ndk-python-extension", "ndk-python-extension")
  .settings(
    name := "neoflex-decision-kit",
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
      "org.scalatest"  %% "scalatest"            % "3.2.11",
      "ru.neoflex.ndk" %% "neoflex-decision-kit" % "1.2.0"
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
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "ru.neoflex.ndk" %% "neoflex-decision-kit" % "1.2.0"
    )
  )

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
    publish / skip := true
  )
  .dependsOn(core, ndkRenderer)

lazy val restModelExample = (project in file("examples/rest-model"))
  .settings(
    name := "rest-model-example",
    version := "0.0.1-SNAPSHOT",
    publish / skip := true
  )
  .dependsOn(core, ndkRenderer)

def artifactModule(id: String, baseDir: String) =
  Project(id = id, base = file(baseDir)).settings(Settings.artifactSettings(baseDir))
