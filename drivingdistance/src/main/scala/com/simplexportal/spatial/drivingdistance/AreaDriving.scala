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
import GraphNode.GetNextDriving

import scala.collection.mutable

/**
  * Algorithm to calculate the driving distance.
  */

object AreaDriving {

  def props(startVertex: ActorRef, maxWeight:Long, targetActor: ActorRef): Props = Props(new AreaDriving(startVertex, maxWeight, targetActor: ActorRef))

  /**
    * Retrieve the driving coverage.
    */
  case object Calculate

}

class AreaDriving(startVertex: ActorRef, maxWeight:Long, targetActor: ActorRef) extends Actor with ActorLogging {

  import AreaDriving._

  val coveragedArea : mutable.Map[ActorRef, Long] = mutable.Map( startVertex -> 0 )
  var responsesPending = 1

  override def receive: Receive = {
    case Calculate => startVertex ! GetNextDriving(0)
    case next: Seq[(ActorRef, Long)] => {
      responsesPending = responsesPending -1
      next.foreach{case(actorRef, newWeight) => analiseNewVertex(actorRef, newWeight) }

      if( responsesPending == 0) targetActor ! coveragedArea.toMap // If it's the last, response with immutable Map
    }
  }

  private def analiseNewVertex(actorRef: ActorRef, newWeight: Long) = {
    if(newWeight < maxWeight) {
      coveragedArea.get(actorRef) match {
        case None => {
          coveragedArea.put(actorRef, newWeight)
          actorRef ! GetNextDriving(newWeight)

          responsesPending = responsesPending +1
        }
        case Some(oldWeight) => if(newWeight < oldWeight) coveragedArea.put(actorRef, newWeight)
      }
    }
  }
}
