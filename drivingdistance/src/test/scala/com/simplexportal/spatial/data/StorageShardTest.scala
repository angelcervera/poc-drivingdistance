package com.simplexportal.spatial.data

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import com.simplexportal.spatial.data.StorageShard._
import com.simplexportal.spatial.TestUtilities
import com.simplexportal.spatial.model.Way
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class StorageShardTest extends TestKit(ActorSystem("ShardManagerActorTest"))
  with ImplicitSender
  with TestUtilities
  with WordSpecLike
  with BeforeAndAfterAll
  with Matchers {

  override def afterAll = TestKit.shutdownActorSystem(system)

  "A ShardStorage actor" should {

    val wayTmp = Way(1, Seq.empty,Map.empty,Map.empty)
    val way1 = wayTmp.copy(id=1)
    val way2 = wayTmp.copy(id=2)
    val way2bis = way2.copy(tags = Map("key1"->"Values1"))
    val way3 = wayTmp.copy(id=3)

    "return Way by id using Get message" in {
      // With data
      val noEmptyShard = TestActorRef(StorageShard.props(Map(1L->way1, 2L->way2)))
      noEmptyShard ! Get(1)
      noEmptyShard ! Get(2)
      noEmptyShard ! Get(3)
      expectMsg(Some(way1))
      expectMsg(Some(way2))
      expectMsg(None)

      // with no data
      val emptyShard = TestActorRef(StorageShard.props)
      emptyShard ! Get(1)
      expectMsg(None)

    }

    "return right metrics using Metrics message" in {
      // With data
      val noEmptyShard = TestActorRef(StorageShard.props(Map(1L->way1, 2L->way2)))
      noEmptyShard ! GetMetrics
      expectMsg(Metrics(2))

      // with no data
      val emptyShard = TestActorRef(StorageShard.props)
      emptyShard ! GetMetrics
      expectMsg(Metrics(0))

    }

    "add or replace ways using Put" in {
      val shard = TestActorRef(StorageShard.props)
      shard ! Put(way1)
      shard ! Put(way2)
      shard ! Put(way3)
      shard ! Put(way2bis)
      shard ! Get(1)
      shard ! Get(2)
      shard ! GetMetrics
      expectMsg(Some(way1))
      expectMsg(Some(way2bis))
      expectMsg(Metrics(3))
    }

    "remove way from the shard using Delete message" in {
      val shard = TestActorRef(StorageShard.props(Map(1L->way1, 2L->way2)))
      shard ! Delete(2L)
      shard ! Get(1)
      shard ! Get(2)
      shard ! GetMetrics
      expectMsg(Some(way1))
      expectMsg(None)
      expectMsg(Metrics(1))
    }

  }
}
