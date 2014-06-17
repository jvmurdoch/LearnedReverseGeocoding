name := "LearnedReverseGeocoding"

organization := "com.example"

version := "0.1"

scalaVersion := "2.10.3"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= {
  val akkaV = "2.3.2"
  val sprayV = "1.3.1"
  Seq(
    "com.datastax.cassandra" % "cassandra-driver-core" % "2.0.2" exclude("org.xerial.snappy", "snappy-java"),
    "com.typesafe.akka" %% "akka-actor" % akkaV,
    "io.spray" % "spray-can" % sprayV,
    "io.spray" % "spray-client" % sprayV,
    "io.spray" % "spray-routing" % sprayV,
    "io.spray" %% "spray-json" % "1.2.5",
    "nz.ac.waikato.cms.weka" % "weka-stable" % "3.6.11",
    "org.xerial.snappy" % "snappy-java" % "1.0.5",
    //"org.specs2" %% "specs2" % "2.2.3" % "test",
    //"io.spray" % "spray-testkit" % sprayV % "test",
    //"com.typesafe.akka" %% "akka-testkit" % akkaV % "test",
    "org.slf4j" % "slf4j-nop" % "1.7.5"
  )
}

parallelExecution in Test := false

