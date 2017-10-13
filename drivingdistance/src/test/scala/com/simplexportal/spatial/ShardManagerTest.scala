package com.simplexportal.spatial

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import akka.util.Timeout
import better.files.File
import com.simplexportal.spatial.ShardManager._
import com.simplexportal.spatial.model.{Location, Node, Way}
import org.scalatest.{BeforeAndAfterAll, GivenWhenThen, Matchers, WordSpecLike}

import scala.concurrent.Await
import scala.concurrent.duration._

class ShardManagerTest extends TestKit(ActorSystem("ShardManagerActorTest"))
  with ImplicitSender
  with WordSpecLike
  with Matchers
  with GivenWhenThen
  with BeforeAndAfterAll
  with TestUtilities {

  override def afterAll = TestKit.shutdownActorSystem(system)

  "Adding a few ways to the shard" must {

    "store all ways in the ways in the shard by id" in {
      Given("an empty shard actor and a set of ways")
      val wayTmp = Way(1, Seq.empty,Map.empty,Map.empty)
      val way1Expected = wayTmp.copy(id=1)
      val way2Expected = wayTmp.copy(id=2)
      val way2bisExpected = way2Expected.copy(tags = Map("key1"->"Values1"))
      val way3Deleted = wayTmp.copy(id=3)

      val shardManager = TestActorRef(ShardManager.props)

      When("add two new ways, the second one two times")
      shardManager ! Put(way1Expected)
      shardManager ! Put(way2Expected)
      shardManager ! Put(way2bisExpected)
      shardManager ! Put(way3Deleted)
      shardManager ! Remove(3)

      Then("two ways will be added")
      implicit val timeout = Timeout(5 seconds)

      val metricsFuture = shardManager ? GetMetrics
      val metrics = Await.result(metricsFuture, timeout.duration).asInstanceOf[Metrics]
      assert(metrics.size == 2)

      val way1Future = shardManager ? Get(1)
      val way1 = Await.result(way1Future, timeout.duration).asInstanceOf[Way]
      assert(way1 == way1Expected)

      val way2Future = shardManager ? Get(2)
      val way2 = Await.result(way2Future, timeout.duration).asInstanceOf[Way]
      assert(way2 == way2bisExpected)

    }

    "work using the bulk" in {
      Given("a set of ways")
      val shardManager = TestActorRef(ShardManager.props)
      val folder = File("assets/osm/faroe-islands-json")

      When("adding two time the list")
      readBulks(folder, ways => shardManager ! PutBulk(ways))
      readBulks(folder, ways => shardManager ! PutBulk(ways))

      Then("only one time has been inserted")
      implicit val timeout = Timeout(5 seconds)
      val metricsFuture = shardManager ? GetMetrics
      val metrics = Await.result(metricsFuture, timeout.duration).asInstanceOf[Metrics]
      assert(metrics.size == 13303)

      And("with the right value")
      val expected = Way(
        44187800L,
        Seq(
          Node(561268530L,Location(61.842537299999954,-6.808299299999992),Map()),
          Node(561268518L,Location(61.84233149999995,-6.809169099999992),Map())
        ),
        Map("name" -> "Í Trøðum", "highway" -> "service", "service" -> "driveway"),
        Map(561268518L -> Seq(497911997L))
      )
      val way = Await.result(shardManager ? Get(44187800L), timeout.duration).asInstanceOf[Way]
      assert(way == expected)
    }

  }
}
