import sbt.Keys._

lazy val commonSettings = Seq(

  organization := "com.simplexportal.spatial.drivingdistance",
  scalaVersion := "2.12.3",
  organizationHomepage := Some(url("http://www.acervera.com")),
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),

  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.0.1" % "test",
    "org.scalacheck" %% "scalacheck" % "1.13.4" % "test",
    "commons-io" % "commons-io" % "2.5" % "test"
  ),
  version := "1.0-SNAPSHOT",
  resolvers += "osm4scala repo" at "http://dl.bintray.com/angelcervera/maven"
)

lazy val drivingdistance = Project(id = "drivingdistance", base = file("drivingdistance")).
  settings(
    commonSettings,
    name := "drivingdistance",
    description := "Driving distance",
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.1.7",
      "com.typesafe.akka" %% "akka-actor" % "2.5.3",
      "com.typesafe.akka" %% "akka-testkit" % "2.5.3"
    )
  )

lazy val blocksextraction = Project(id = "blocksextraction", base = file("blocksextraction")).
  settings(
    commonSettings,
    name := "blockextraction",
    description := "Extract all blocks from an osmfile",
    libraryDependencies ++= Seq(
      "com.acervera.osm4scala" %% "osm4scala-core" % "1.0.1",
      "com.github.scopt" %% "scopt" % "3.5.0"
    )
  )

lazy val loader = Project(id = "loader", base = file("loader")).
  settings(
    commonSettings,
    scalaVersion := "2.11.8",
    name := "loader",
    description := "Read osm blocks an generate the network.",
    libraryDependencies ++= Seq(
      "com.acervera.osm4scala" %% "osm4scala-core" % "1.0.1",
      "org.apache.spark" %% "spark-core" % "2.2.0" % "provided",
      "com.github.scopt" %% "scopt" % "3.5.0"
    )
  )
