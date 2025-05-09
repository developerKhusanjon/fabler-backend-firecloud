ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "com.fabler"
ThisBuild / organizationName := "Fabler"
ThisBuild / scalaVersion := "2.13.10"

val Http4sVersion = "0.23.7"
val CirceVersion = "0.14.1"
val PureConfigVersion = "0.17.1"

lazy val root = (project in file("."))
  .settings(
    name := "fabler-firecloud",
    libraryDependencies ++= Seq(
      // Cats Effect
      "org.typelevel" %% "cats-effect" % "3.3.0",

      // Http4s
      "org.http4s" %% "http4s-dsl" % Http4sVersion,
      "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s" %% "http4s-blaze-client" % Http4sVersion,
      "org.http4s" %% "http4s-circe" % Http4sVersion,

      // Circe for JSON handling
      "io.circe" %% "circe-generic" % CirceVersion,
      "io.circe" %% "circe-literal" % CirceVersion,
      "io.circe" %% "circe-parser" % CirceVersion,

      // PureConfig for configuration
      "com.github.pureconfig" %% "pureconfig" % PureConfigVersion,
      "com.github.pureconfig" %% "pureconfig-cats-effect" % PureConfigVersion,

      // Logging
      "org.typelevel" %% "log4cats-slf4j" % "2.3.1",
      "ch.qos.logback" % "logback-classic" % "1.2.10",

      // Firebase Admin Java SDK
      "com.google.firebase" % "firebase-admin" % "9.1.1",

      // Testing
      "org.scalatest" %% "scalatest" % "3.2.11" % Test,
      "org.typelevel" %% "cats-effect-testing-scalatest" % "1.4.0" % Test
    )
  )
