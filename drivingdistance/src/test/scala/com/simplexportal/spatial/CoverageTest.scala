package com.simplexportal.spatial

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.simplexportal.spatial.model.{Location, Node, Way}
import org.scalatest._

class CoverageTest extends TestKit(ActorSystem("CoverageActorTest"))
  with ImplicitSender
  with WordSpecLike
  with Matchers
  with GivenWhenThen
  with BeforeAndAfterAll
  with TestUtilities {

  override def afterAll = TestKit.shutdownActorSystem(system)


  def loadNetwork() = {
    val way1 = Way(
      44187800L,
      Seq(
        Node(561268530L,Location(61.842537299999954,-6.808299299999992),Map()),
        Node(561268518L,Location(61.84233149999995,-6.809169099999992),Map())
      ),
      Map("name" -> "Í Trøðum", "highway" -> "service", "service" -> "driveway"),
      Map(561268518L -> Seq(497911997L))
    )
  }


  "Calculating the coverage in the network" must {

    "Split the calculus between all actors" in {
      fail("To implement")
    }

    "Return the right coverage node set" in {
      fail("to implement")
    }
  }


}
