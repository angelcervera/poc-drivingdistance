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

package com.simplexportal.spatial

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.simplexportal.spatial.Coverage.Step
import com.simplexportal.spatial.model.Way

/**
  * Factory for [[com.simplexportal.spatial.WayStorage]] and container for definition of
  * all messages that it accepts.
  */
object WayStorage {
  def props(way:Way): Props = Props(new WayStorage(way))

  /**
    * Message to update the value of the way stored.
    *
    * @param way
    */
  case class Update(way: Way)

  case class Get(to:ActorRef)

}

/**
  * Store a way and implement parts of algorithms related with the way.
  *
  * @param way Way to store.
  */
class WayStorage(var way: Way) extends Actor with ActorLogging
  with CoverageWayStorage {

  import WayStorage._

  log.debug("Storing way [{}]", way.id)

  override def receive = {
    case Update(newWay) => {
      log.debug("Updating way [{}]", way.id)
      way = newWay
    }
    case Get(to) => {
      log.debug("Sending way to [{}]", to)
      to ! way
    }
    case Step(processId, nodeId, weight, maxWeight) => {
      log.debug(
        "Starting process [{}] coverage in the way,node [{},{}] with the current weight [{}] and a maximum weight of [{}]",
        Array(processId, way.id, nodeId, weight, maxWeight)
      )
      coverage(processId, nodeId, weight, maxWeight)
    }
  }

}
