name := "business-rules"
version := "0.0.1-SNAPSHOT"

scalaVersion := "2.13.1"

libraryDependencies += "org.typelevel" %% "cats-core" % "2.7.0"

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.13.1" cross CrossVersion.full)
