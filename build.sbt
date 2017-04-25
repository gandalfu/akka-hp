name := "AkkaHoneyPot"

version := "0.1.1"

scalaVersion := "2.12.1"


libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.4.17",
  "com.typesafe.akka" % "akka-slf4j_2.12" % "2.5.0",
  "com.hynnet" % "logback-classic" % "1.1.3"
)

lazy val commonSettings = Seq(
  version := "1.0",
  organization := "com.ncl",
  scalaVersion := "2.12.1"
)

lazy val app = (project in file("app")).
  settings(commonSettings: _*).
  settings(
    // your settings here
  )

scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature")

