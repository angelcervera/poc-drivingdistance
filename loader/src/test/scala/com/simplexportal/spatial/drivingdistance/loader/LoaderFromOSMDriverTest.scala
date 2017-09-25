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

package com.simplexportal.spatial.drivingdistance.loader

import com.simplexportal.spatial.drivingdistance.model.{Location, Node, Way}
import org.apache.spark.SparkConf
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

    "extract all ways with coords" in {

      Given("a set of 23 blob files")
      val input = "assets/osm/faroe-islands"
      val output = s"/tmp/poc-drivingdistance/${System.currentTimeMillis()}/loader"

      When("")
      val defaultConfig = new SparkConf().setMaster("local[4]")
      LoaderFromOSMDriver.load(defaultConfig, input, output)

      // TODO: Implement manual test.
      Then("the result must contain 13308 lines")

      And("each way must be unique")

      And("for wayId ")

      val expected = Way(
        44187800L,
        Seq(
          Node(561268530L,Location(61.842537299999954,-6.808299299999992),Map()),
          Node(561268518L,Location(61.84233149999995,-6.809169099999992),Map())
        ),
        Map("name" -> "Í Trøðum", "highway" -> "service", "service" -> "driveway"),
        Map(561268518L -> Seq(497911997L))
      )

    }
  }

}