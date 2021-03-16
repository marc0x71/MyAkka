name := "MyAkka"

version := "0.1"

scalaVersion := "2.13.5"

idePackagePrefix := Some("org.example.akka")

val akkaVersion = "2.6.5"
val ScalaLogging = "3.9.2"
val LogbackVersion = "1.2.3"

libraryDependencies ++= Seq(
  "com.typesafe.scala-logging" %% "scala-logging" % ScalaLogging,
  "ch.qos.logback" % "logback-classic" % LogbackVersion,
  //"com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test
)