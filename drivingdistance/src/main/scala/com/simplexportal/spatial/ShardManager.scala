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

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}
import com.simplexportal.spatial.model._

// TODO: Manage errors, etc.

/**
  * Actor that manages the contentment of the database.
  */
object ShardManager {
  def props: Props = Props(classOf[ShardManager])

  /**
    * Message used to add a new Way to the shard
    * @param way
    */
  case class Put(way:Way)

  /**
    * Message used to get a Way from the shard by id
    * @param wayId
    */
  case class Get(wayId:Long)

  /**
    * Remove the way from the shard.
    * @param wayId
    */
  case class Remove(wayId:Long)

  /**
    * Message used to get metrics about the shard
    */
  case object GetMetrics

  /**
    * Information about the shard
    *
    * @param size Number of ways stored.
    */
  case class Metrics(size:Long)

}

class ShardManager extends Actor with ActorLogging
  with CoverageShardManager {
  import ShardManager._

  val indexByKey = scala.collection.mutable.Map[Long, ActorRef]()

  override def receive = {
    case Put(way) => {
      indexByKey.get(way.id) match {
        case None => indexByKey += way.id -> context.actorOf(WayStorage.props(way), s"way_${way.id}")
        case Some(actorRef) => actorRef ! WayStorage.Update(way)
      }
    }
    case Get(wayId) => {
      indexByKey.get(wayId) match {
        case None => ???
        case Some(actorRef) => actorRef ! WayStorage.Get(sender)
      }
    }
    case Remove(wayId) => {
      indexByKey.get(wayId) match {
        case None => ???
        case Some(actorRef) => {
          actorRef ! PoisonPill
          indexByKey.remove(wayId)
        }
      }
    }
    case GetMetrics => {
      log.debug("Sending metrics to {}", sender)
      sender ! Metrics(indexByKey.size)
    }
  }
}
