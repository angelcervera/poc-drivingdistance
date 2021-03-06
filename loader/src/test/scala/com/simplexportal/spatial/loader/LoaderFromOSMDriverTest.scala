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

package com.simplexportal.spatial.loader

import java.io.FileOutputStream

import better.files._
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Output
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.simplexportal.spatial.model.{Location, Node, Way}
import org.apache.spark.{SparkConf, SparkContext}
import org.scalatest.{GivenWhenThen, WordSpec}

class LoaderFromOSMDriverTest extends WordSpec with GivenWhenThen {

  "LoaderFromOSMDriver" should {

    "spreadWays" in {
      Given("a set of intersection and ways connected with every intersection")
      val nodeId = 1
      val ways = Seq[Long](1,2,3)

      When("spread to a list of ways that you can forward from the current way an node ")
      val spreaded = LoaderFromOSMDriver.spreadWays(nodeId, ways)

      Then("the result must be correct")
      val expected = Set[(Long, (Long, Seq[Long]))](
        (1, (1, Seq(2, 3))),
        (2, (1, Seq(1, 3))),
        (3, (1, Seq(1, 2)))
      )

      assert(spreaded.toSet == expected)
    }

    "generate the network" in {

      Given("a set of 23 blob files")
      val conf = new SparkConf().setMaster("local[4]").setAppName("PocDrivingDistance Loader")
      conf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      val sc = new SparkContext(conf)
      val input = "assets/osm/faroe-islands"

      When("generate the Network")
      try {
        val network = LoaderFromOSMDriver.generateNetwork(sc, input).collect()

        Then("the result must contain 13308 ways")
        assert(network.size == 13303)

        And("each way must be unique")
        assert(network.groupBy(_.id).forall(_._2.size == 1))

        And("for wayId ")
        network.find(_.id == 44187800L) match {
          case(None) => fail("44187800L not found")
          case(Some(way)) => {
            val expected = Way(
              44187800L,
              Seq(
                Node(561268530L,Location(61.842537299999954,-6.808299299999992),Map()),
                Node(561268518L,Location(61.84233149999995,-6.809169099999992),Map())
              ),
              Map("name" -> "Í Trøðum", "highway" -> "service", "service" -> "driveway"),
              Map(561268518L -> Seq(497911997L))
            )
            assert(way == expected)
          }
        }
      } finally {
        sc.stop()
      }

    }

    "generate the network and serialize to json" in {

      Given("a set of 23 blob files")
      val conf = new SparkConf().setMaster("local[4]").setAppName("PocDrivingDistance Loader Export JSON")
      conf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      val sc = new SparkContext(conf)
      val input = "assets/osm/faroe-islands"
      val outFolder = s"/tmp/poc-drivingdistance/loader-json/${System.currentTimeMillis()}/"

      When("generate the Network")
      try {
        LoaderFromOSMDriver.generateNetwork(sc, input)
          .mapPartitions(ways=> {
            val mapper = new ObjectMapper()
            mapper.registerModule(DefaultScalaModule)
            ways.map(mapper.writeValueAsString)
          })
          .saveAsTextFile(outFolder)

        Then("the result must contain 13308 ways")
        val lines:Int = File(outFolder)
          .children
          .filter(file => file.name != "_SUCCESS" && file.extension.getOrElse("") != ".crc" )
          .map(file=>file.lines.size)
          .reduce(_+_)
        assert(lines == 13303)

      } finally {
        sc.stop()
      }

    }

    "generate the network and serialize to kryo" in {

      Given("a set of 23 blob files")
      val conf = new SparkConf().setMaster("local[4]").setAppName("PocDrivingDistance Loader Export Kryo")
      conf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      val sc = new SparkContext(conf)
      val input = "assets/osm/faroe-islands"
      val outFolder = s"/tmp/poc-drivingdistance/loader-kryo/${System.currentTimeMillis()}/"
      File(outFolder).createDirectories()

      When("generate the Network")
      try {
        LoaderFromOSMDriver.generateNetwork(sc, input)
          .foreachPartition(ways => {
            val kryo = new Kryo()
            ways.foreach(way => {
              val fileOut = new FileOutputStream(outFolder + s"/${way.id}.way")
              val os = new Output(fileOut)
              kryo.writeObject(os, way)
              os.close()
              fileOut.close()
            })
          })

        Then("the result must contain 13308 ways")
        assert(File(outFolder).children.size == 13303)

      } finally {
        sc.stop()
      }

    }

  }

}
