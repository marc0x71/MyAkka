name := "MyAkka"

version := "0.1"

scalaVersion := "2.13.5"

idePackagePrefix := Some("org.example.akka")

val akkaVersion = "2.6.5"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test
)