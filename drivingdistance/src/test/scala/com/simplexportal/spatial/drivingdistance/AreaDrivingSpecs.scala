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

package com.simplexportal.spatial.drivingdistance

import akka.actor.{ActorRef, ActorSystem}
import akka.util.Timeout
import com.simplexportal.spatial.drivingdistance.AreaDriving.Calculate
import com.simplexportal.spatial.drivingdistance.Weights

import scala.concurrent.duration._
import akka.testkit.TestProbe

class AreaDrivingSpecs(_system: ActorSystem) extends AreaDrivingTestKit(_system) {

  def this() = this(ActorSystem("AreaDrivingSpecs"))

  override def afterAll: Unit = {
    shutdown(system)
  }

  implicit val timeout = Timeout(30 second)

  def newGraphNode(idx:Long, edges: Seq[(ActorRef, Weights)]): ActorRef =
    system.actorOf(GraphNode.props(Vertex(idx, LocationXX(idx.toFloat, idx.toFloat)), edges), idx.toString)

  def linealGraph(nodes:Int) =
    (0 to (nodes -1)).reverse.foldLeft(newGraphNode(nodes, Seq()))((prev,idx)=>newGraphNode(idx, Seq( (prev, Weights(Some(1),Some(1))) )))

  "AreaDriving" should {

    "return 10 Vertices" in {
      Given(" a linear graph of 10^3 nodes and fixed weight of 1 for driving and 4 for walking")
      val firstNode: ActorRef = linealGraph(Math.pow(10, 3) toInt)

      When("calculate area driving of 10 minutes")
      val probeActor = TestProbe()
      val areaDriving = system.actorOf(AreaDriving.props(firstNode, 10, probeActor.ref), "10_minutes_driving")
      areaDriving ! Calculate

      Then("must return 10 nodes")
      val nodes = probeActor.receiveN(1, 30 second)
      assert(nodes(0).asInstanceOf[Map[ActorRef, Long]].size == 10)

    }
  }
}
