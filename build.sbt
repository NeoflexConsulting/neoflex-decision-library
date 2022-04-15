name := "neoflex-decision-kit"
version := "0.0.1-SNAPSHOT"

scalaVersion := "2.13.1"

libraryDependencies += "org.typelevel" %% "cats-core" % "2.7.0"
libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.9"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.11"

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.13.1" cross CrossVersion.full)
