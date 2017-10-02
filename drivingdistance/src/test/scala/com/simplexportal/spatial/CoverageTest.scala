package com.simplexportal.spatial

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest._

class CoverageTest extends TestKit(ActorSystem("CoverageActorTest"))
  with ImplicitSender
  with WordSpecLike
  with Matchers
  with GivenWhenThen
  with BeforeAndAfterAll {


  def loadNetwork() = ???


  "Calculating the coverage in the network" must {

    "Split the calculus between all actors" in {
      fail("To implement")
    }

    "Return the right coverage node set" in {
      fail("to implement")
    }
  }


}
