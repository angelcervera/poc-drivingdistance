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

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

case class Vertex(id: Long, location: LocationXX)
case class LocationXX(lon: Float, lat: Float)
case class Weights(walking: Option[Long], driving: Option[Long])

object GraphNode {

  def props(vertex:Vertex, edges: Seq[(ActorRef, Weights)]): Props = Props(new GraphNode(vertex, edges))

  // Messages

  /**
    * Calculate next routes possible and how expensive they are.
    */
  case class GetNextWalking(weight:Long)
  case class GetNextDriving(weight:Long)

  /**
    * Return information about the vertex.
    */
  case object Get
}

class GraphNode(vertex: Vertex, edges: Seq[(ActorRef, Weights)]) extends Actor with ActorLogging {
  import GraphNode._

  // TODO: Enrich with traffic lights, traffic signal, temporal changes in the route, realtime data like accidents, etc.
  override def receive: Receive =  {
    case Get => sender ! vertex
    case GetNextDriving(totalWeight) => sender ! next(totalWeight, weights => weights.driving)
    case GetNextWalking(totalWeight) => sender ! next(totalWeight, weights => weights.walking)
  }

  private def next(totalWeight: Long, calculateWeight: Weights => Option[Long]): Seq[(ActorRef, Long)] =
    edges.flatMap{ case(actorRef, weights) => calculateWeight(weights).map(weight => (actorRef, totalWeight + weight) )}

}
