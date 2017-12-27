package com.simplexportal.spatial.data

import akka.actor.{Actor, ActorLogging, Props}
import com.simplexportal.spatial.data.StorageShard._
import com.simplexportal.spatial.model._

object StorageShard {

  /**
    * Create a Props for an empty shard.
    */
  def props: Props = Props(classOf[StorageShard], Map())

  /**
    * Create a Props with a default data set.
    */
  def props(ways: Map[Long, Way]): Props = Props(classOf[StorageShard], ways)





  sealed trait ShardStorageMessages

  /**
    * Message used to add a new Way to the shard
    */
  case class Put(way: Way) extends ShardStorageMessages

  /**
    * Message used to add a list of new Ways to the shard
    */
  case class PutBulk(ways: Seq[Way]) extends ShardStorageMessages

  /**
    * Message used to get a Way from the shard by id
    */
  case class Get(wayId: Long) extends ShardStorageMessages

  /**
    * Remove the way from the shard.
    */
  case class Delete(wayId: Long) extends ShardStorageMessages

  /**
    * Message used to get metrics about the shard
    */
  case object GetMetrics extends ShardStorageMessages

  /**
    * Information about the shard
    *
    * @param size Number of ways stored.
    */
  case class Metrics(size:Long)

}

// TODO: Benchmark with other Map implementations, mutable and immutable https://github.com/angelcervera/poc-drivingdistance/issues/2
class StorageShard(private var ways: Map[Long, Way]) extends Actor with ActorLogging {

  // TODO: Store metrics as Nodes density.

  override def receive = {
    case Get(id) => sender ! ways.get(id)
    case GetMetrics => sender ! Metrics(ways.size)
    case Put(way) => ways = ways + (way.id -> way)
    case Delete(id) => ways = ways - id
  }
}
