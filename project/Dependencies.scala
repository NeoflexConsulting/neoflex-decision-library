import sbt._

object Version {
  lazy val Circe  = "0.14.2"
  lazy val Doobie = "1.0.0-RC2"
  lazy val Http4s = "0.23.12"
}

object Dependencies {
  lazy val CirceYaml    = "io.circe"      %% "circe-yaml"    % Version.Circe
  lazy val CirceGeneric = "io.circe"      %% "circe-generic" % Version.Circe
  lazy val CirceParser  = "io.circe"      %% "circe-parser"  % Version.Circe
  lazy val Scalatest    = "org.scalatest" %% "scalatest"     % "3.2.12"
  lazy val Purecsv = "io.kontainers" %% "purecsv" % "1.3.10"
  lazy val Nanoid = "com.aventrix.jnanoid" % "jnanoid" % "2.0.0"
  lazy val CatsCore = "org.typelevel"  %% "cats-core"      % "2.7.0"
  lazy val Pureconfig = "com.github.pureconfig" %% "pureconfig" % "0.17.1"
  lazy val FeelEngine = "org.camunda.feel"      % "feel-engine" % "1.14.2"
  lazy val KafkaClients = "org.apache.kafka" % "kafka-clients" % "3.2.1"

  object Akka {
    lazy val Stream      = "com.typesafe.akka"  %% "akka-stream"             % "2.6.19"
    lazy val StreamKafka = "com.typesafe.akka"  %% "akka-stream-kafka"       % "3.0.0"
    lazy val Actor       = "com.typesafe.akka"  %% "akka-actor"              % "2.6.19"
    lazy val Http        = "com.typesafe.akka"  %% "akka-http"               % "10.2.9"
    lazy val AlpakkaCsv  = "com.lightbend.akka" %% "akka-stream-alpakka-csv" % "3.0.4"
  }

  object Internal {
    lazy val Core = "ru.neoflex.ndk" %% "neoflex-decision-kit" % "1.7.0-SNAPSHOT"
  }

  object Doobie {
    lazy val Core     = "org.tpolecat" %% "doobie-core"     % Version.Doobie
    lazy val Hikari   = "org.tpolecat" %% "doobie-hikari"   % Version.Doobie
    lazy val Specs2   = "org.tpolecat" %% "doobie-specs2"   % Version.Doobie
    lazy val Postgres = "org.tpolecat" %% "doobie-postgres" % Version.Doobie
  }

  object Http4s {
    lazy val Dsl         = "org.http4s" %% "http4s-dsl"          % Version.Http4s
    lazy val EmberClient = "org.http4s" %% "http4s-ember-client" % Version.Http4s
    lazy val Circe       = "org.http4s" %% "http4s-circe"        % Version.Http4s
  }
}
