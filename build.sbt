lazy val root = (project in file("."))
  .settings(
    name := "ziverge-challenge",
    scalaVersion := "2.13.6",
    libraryDependencies ++= Seq(
      "co.fs2" %% "fs2-core" % "3.0.6",
      "co.fs2" %% "fs2-io" % "3.0.6",
      "io.circe" %% "circe-core" % "0.14.1",
      "io.circe" %% "circe-parser" % "0.14.1",
      "org.http4s" %% "http4s-dsl" % "0.23.0",
      "org.http4s" %% "http4s-blaze-server" % "0.23.0",
      "org.http4s" %% "http4s-blaze-client" % "0.23.0",
      "org.http4s" %% "http4s-circe" % "0.23.0",
      "ch.qos.logback" %  "logback-classic" % "1.2.5",

    )
  )
