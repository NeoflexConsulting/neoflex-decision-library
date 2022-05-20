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
    libraryDependencies += "org.typelevel"  %% "cats-core"      % "2.7.0",
    libraryDependencies += "org.slf4j"      % "slf4j-api"       % "1.7.9",
    libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.11"
  )

lazy val testKit = artifactModule("ndk-test-kit", "ndk-test-kit")
  .settings(
    name := "ndk-test-kit",
    resolvers ++= Repositories.resolvers,
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.2.11",
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

def artifactModule(id: String, baseDir: String) =
  Project(id = id, base = file(baseDir)).settings(Settings.artifactSettings(baseDir))
