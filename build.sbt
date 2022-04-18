import sbt.Keys.libraryDependencies

ThisBuild / organization := "ru.neoflex.ndk"
ThisBuild / scalaVersion := "2.13.1"

lazy val root = (project in file("."))
  .settings(
    publish / skip  := true
  )
  .aggregate(core, underwritingExample)

lazy val core = Project(id = "neoflex-decision-kit", base = file("core"))
  .settings(
    name := "neoflex-decision-kit",
    version := "0.0.1-SNAPSHOT",
    libraryDependencies += "org.typelevel" %% "cats-core" % "2.7.0",
    libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.9",
    libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.11"
  )


lazy val underwritingExample = (project in file("examples/underwriting"))
  .settings(
    name := "underwriting-example",
    version := "0.0.1-SNAPSHOT",
    publish / skip  := true
  )
  .dependsOn(core)
