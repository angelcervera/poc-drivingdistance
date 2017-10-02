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

import akka.actor.ActorRef
import com.simplexportal.spatial.Coverage.{IntersectionFound, WayDone}
import com.simplexportal.spatial.model.Node

import scala.annotation.tailrec

object Coverage {


  /**
    * Message to send to the [[WayStorage]] from the [[ShardManager]] to start the coverage calculus on the (way, node)
    */
  case class Step(processId: Long, nodeId: Long, weight: Long, maxWeight: Long)

  /**
    * Message to sent to the [[ShardManager]] when the calculation of a way is done.
    *
    * @param processId Id of the process executed.
    * @param wayId Id of way done.
    * @param nodeId Id of the node used as start point.
    * @param nodesProcessed Nodes processed with the weight.
    */
  case class WayDone(processId: Long, wayId:Long, nodeId: Long, nodesProcessed: Seq[(Long, Long)])

  /**
    * Message to sent to the [[ShardManager]] when the weight of the node that is not an intersection has been calculated.
    *
    * @param processId
    * @param wayId
    * @param nodeId
    * @param weight
    */
  case class NodeDone(processId: Long, wayId:Long, nodeId: Long, weight: Long)

  /**
    * Message to send to the [[ShardManager]] when you found an intersection.
    *
    * @param processId
    * @param fromWayId
    * @param toWayId
    * @param nodeId
    * @param weight
    */
  case class IntersectionFound(processId: Long, fromWayId: Long, toWayId:Long, nodeId: Long, weight: Long)

//  /**
//    * Message to calculate the coverage or Driving Distance from one point in a street.
//    *
//    * @param wayId
//    * @param nodeId
//    */
//  case class Coverage(wayId: Long, nodeId:Long) // TODO: Coverage from a spatial location


}

trait CoverageShardManager {
  this: ShardManager =>


  case class Process(sender: ActorRef, nodesVisited: Map[Long, VisitedNodeInfo] )

  /**
    * Information about one node visited.
    *
    * @param minWeightFound The minimum weight found.
    * @param ways Ways used to arrive to this point.
    */
  case class VisitedNodeInfo(minWeightFound:Long, ways: Set[Long])

  /**
    * One actor can handle more than one coverage process.
    * This is an index per process, and every process contains information about all nodes visited.
    */
  var coverageProcesses = Map[Long, Process]()



}

trait CoverageWayStorage {
  this: WayStorage =>


  def coverage(processId: Long, startNodeId: Long, startWeight: Long, maxWeight: Long) = {

    var nodesProcessed = Seq[(Long, Long)]()

    @tailrec
    def recNextNode(currentNode: Node, nodes:Seq[Node], weight: Long, ascDirection: Boolean): Any = nodes match {
      case Nil => sender ! WayDone(processId, way.id, startNodeId, nodesProcessed)
      case node :: tail => {

        // calculate new weight
        val newWeight = weight + calculateWeight(currentNode, node)

        // Continue only if the weight is lower than maximum.
        if(newWeight > maxWeight) {
          sender ! WayDone(processId, way.id, startNodeId, nodesProcessed)
        } else {
          // is this node an intersection?
          way.intersections.get(node.id) match {
            case Some(ways) => ways.foreach( nextWay => sender ! IntersectionFound(processId, way.id, nextWay, node.id, newWeight) )
            case None =>
          }

          // The node has been processed.
          nodesProcessed :+ (node.id, newWeight)

          recNextNode(node, tail, newWeight, ascDirection)
        }
      }
    }

    // Start the recursive call in both directions.
    // TODO: Check way direction using tags list.
    val nodePos = way.nodes.zipWithIndex.find(_._1.id == startNodeId).getOrElse(throw new Exception(s"Node ${startNodeId} not found in way [${way.id}]"))
    recNextNode(nodePos._1, way.nodes.drop(nodePos._2+1), startWeight, true)
    recNextNode(nodePos._1, way.nodes.dropRight(way.nodes.length - nodePos._2).reverse, startWeight, false)

  }



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
  def calculateWeight(from: Node, to: Node): Long = ???

}

