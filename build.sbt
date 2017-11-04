/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Ángel Cervera Claudio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

import sbt.Keys._

lazy val commonSettings = Seq(

  javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint"),
  scalacOptions := Seq("-target:jvm-1.8"),

  organization := "com.simplexportal.spatial.drivingdistance",
  scalaVersion := "2.11.11",
  organizationHomepage := Some(url("http://www.acervera.com")),
  licenses += ("MIT", url("http://opensource.org/licenses/MIT")),

  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "3.0.4" % "test",
    "org.scalactic" %% "scalactic" % "3.0.4" % "test",
    "org.scalacheck" %% "scalacheck" % "1.13.4" % "test",
    "commons-io" % "commons-io" % "2.5" % "test"
  ),
  version := "1.0-SNAPSHOT",
  resolvers += "osm4scala repo" at "http://dl.bintray.com/angelcervera/maven"
)

// SBT BUG: https://github.com/sbt/sbt/issues/1448 / https://stackoverflow.com/questions/27929272/how-to-have-sbt-subproject-with-multiple-scala-versions
lazy val model = Project( id="model", base = file("model")).
  settings(commonSettings: _*).
  settings(
    name := "model",
    description := "Model that represent the network"
  )

val versions = Map(
  "akkaHttp" -> "10.0.10",
  "akka" -> "2.5.6"
)

lazy val drivingDistance = Project(id = "drivingdistance", base = file("drivingdistance")).
  settings(commonSettings: _*).
  settings(
    name := "drivingdistance",
    description := "Driving distance",
    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.1.7",
      "com.vividsolutions" % "jts" % "1.13",
      "com.typesafe.akka" %% "akka-actor"           % versions("akka"),
      "com.typesafe.akka" %% "akka-http"            % versions("akkaHttp"),
      "com.typesafe.akka" %% "akka-http-spray-json" % versions("akkaHttp"),
      "com.typesafe.akka" %% "akka-http-xml"        % versions("akkaHttp"),
      "com.typesafe.akka" %% "akka-stream"          % versions("akka"),

      "io.circe" %% "circe-core" % "0.8.0" % Test,
      "io.circe" %% "circe-generic" % "0.8.0" % Test,
      "io.circe" %% "circe-parser" % "0.8.0" % Test,
      "com.github.pathikrit" %% "better-files" % "2.17.1" % Test,
      "org.apache.commons" % "commons-compress" % "1.14" % Test,
      "org.tukaani" % "xz" % "1.6" % Test,
      "com.typesafe.akka" %% "akka-testkit" % versions("akka") % Test,
      "com.typesafe.akka" %% "akka-stream-testkit" % versions("akka") % Test
    )
  ).dependsOn(model)

lazy val loader = Project(id = "loader", base = file("loader")).
  settings(commonSettings: _*).
  settings(
    name := "loader",
    description := "Read osm blocks an generate the network.",
    libraryDependencies ++= Seq(
      "com.acervera.osm4scala" %% "osm4scala-core" % "1.0.1",
      "com.github.pathikrit" %% "better-files" % "2.17.1" % "test",
      "org.apache.spark" %% "spark-core" % "2.2.0" % "provided",
      "org.apache.kafka" % "kafka-clients" % "1.0.0"
    )
  ).dependsOn(model)
