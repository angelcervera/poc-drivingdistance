package com.simplexportal.spatial.drivingdistance

import akka.actor.{Actor, ActorLogging, ActorRef}
import com.simplexportal.spatial.drivingdistance.CoverageShard.{NodesWeight, WayLocation}
import com.simplexportal.spatial.model.{Node, Way}
import com.vividsolutions.jts.geom.Coordinate

import scala.annotation.tailrec

object CoverageShard {

  sealed trait CoverageShardMessage

  /**
    * Message used to start a coverage calculation in the shard.
    *
    * @param ways Map that represent the network.
    *             Because this call must be in the same process, this data is going o be passed as a reference,
    *             so no de/serialization penalty.
    * @param target Actor where sent the result.
    * @param startPoint Point in the network to start the computation.
    * @param maxWeight Max coverage value.
    */
  case class Start(ways: Map[Long, Way], target:ActorRef, startPoint: WayLocation, maxWeight: Long) extends CoverageShardMessage

  /**
    * Specify the more accurate point int he network.
    */
  case class WayLocation(wayId: Long, nodeId: Long) // TODO: This could be refactored to a LonLat point





  /**
    * Represent the weight of a set of nodes.
    */
  type NodesWeight = Map[Long, Double]

  /**
    * Calculate the coverage for the way. Only, for the way. That means that the result could be different for the shard,
    * because using other way, could arrive to the some node but with lower weight.
    */
  def coverageWay(way: Way, startNodeId: Long, startWeight: Double, maxWeight: Double): NodesWeight = {

    @tailrec
    def recNextNode(currentNode: Node, nodes:Seq[Node], weight: Double, nodesProcessed: NodesWeight): NodesWeight = nodes match {
      case Nil => nodesProcessed
      case node :: tail => {
        weight + calculateWeight(currentNode, node) match { // Continue only if the new weight is lower than maximum.
          case newWeight if newWeight > maxWeight => nodesProcessed
          case newWeight => recNextNode(node, tail, newWeight, nodesProcessed + (node.id -> newWeight) )
        }
      }
    }

    // TODO: Check way direction using tags list.
    // Start the recursive call in both directions.
    way.nodes.zipWithIndex.find(_._1.id == startNodeId) match {
      case Some( (node, idx) ) => recNextNode(
        node,
        way.nodes.drop(idx+1),
        startWeight,
        recNextNode(node, way.nodes.dropRight(way.nodes.length - idx).reverse, startWeight, Map.empty)
      )
      case None => throw new Exception(s"Node ${startNodeId} not found in way [${way.id}]")
    }

  }

  def coverageShard() = {
    //    // Continue with intersections
    //    way.intersections.get(node.id) match {
    //      case Some(ways) => {
    //        ways.foreach( nextWay => sender ! IntersectionFound(processId, way.id, nextWay, node.id, newWeight) )
    //        nodesProcessed :+ (node.id, newWeight)
    //      }
    //      case None => nodesProcessed :+ (node.id, newWeight)
    //    }
  }

  // TODO: Move from distance to time.
  /**
    * Calculate the Weight from one node to other, in the current way.
    *
    * To do it, it is possible to used:
    * - The space from one node to other.
    * - Data in tags as type of via.
    * - Other realtime data.
    *
    * @param from
    * @param to
    * @return
    */
  def calculateWeight(from: Node, to: Node): Double =
    new Coordinate(from.coords.longitude,from.coords.latitude).distance(new Coordinate(to.coords.longitude,to.coords.latitude))

}

class CoverageShard extends Actor with ActorLogging {

  override def receive = ???

}
