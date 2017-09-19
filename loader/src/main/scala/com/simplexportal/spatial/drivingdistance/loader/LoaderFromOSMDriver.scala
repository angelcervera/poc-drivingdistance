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

package com.simplexportal.spatial.drivingdistance.loader

import com.acervera.osm4scala.EntityIterator
import com.acervera.osm4scala.model.{NodeEntity, OSMEntity, OSMTypes, WayEntity}
import org.apache.spark.SparkContext
import org.apache.spark.SparkConf
import org.openstreetmap.osmosis.osmbinary.fileformat.Blob
import org.apache.log4j.LogManager
import org.apache.spark.util.LongAccumulator

object LoaderFromOSMDriver {

  val log = LogManager.getRootLogger

  // Node useful information: (nodeId,  (Latitude, Longitude))
  type NodeInfo = (Long, (Double, Double))

  // Way useful information: (nodeId, (wayId, position))
  type WayInfo = (Long, (Long, Int))


  /**
    * Transform the file into a sequence of OSM entities.
    *
    * @param path
    * @param bin
    * @param errorCounter
    * @return
    */
  def parseBlob(path: String, bin: Array[Byte], errorCounter: LongAccumulator): Seq[OSMEntity] = {
    try {
      EntityIterator.fromBlob(Blob.parseFrom(bin)).toSeq
    } catch {
      case error: Throwable => {
        errorCounter.add(1)
        log.error(s"Error reading blob file ${path}", error)
        Seq()
      }
    }
  }

  def extractUsefulDataFromNode(node: NodeEntity): NodeInfo = (node.id, (node.latitude, node.longitude))

  def extractUsefulDataFromWay(way: WayEntity): Seq[WayInfo] = way.nodes.zipWithIndex.map{case(nodeId, idx) => (nodeId, (way.id, idx))}

  /**
    * Generate useful information from an entity.
    * The result is going to be way smaller, so we can cache the result.
    *
    * @param osmEntity
    * @return
    */
  def extractUsefulData(osmEntity: OSMEntity): Seq[Either[NodeInfo, WayInfo]] = osmEntity.osmModel match {
    case OSMTypes.Node => Seq(Left(extractUsefulDataFromNode(osmEntity.asInstanceOf[NodeEntity])))
    case OSMTypes.Way => extractUsefulDataFromWay(osmEntity.asInstanceOf[WayEntity]).map(Right(_))
    case _ => Seq.empty
  }

  def filterNodeData(usedEntity: Either[NodeInfo, WayInfo]): Option[NodeInfo] = usedEntity match {
    case Left(nodeInfo) => Some(nodeInfo)
    case _ => None
  }

  def filterWayData(usedEntity: Either[NodeInfo, WayInfo]): Option[WayInfo] = usedEntity match {
    case Right(wayInfo) => Some(wayInfo)
    case _ => None
  }

  def generateWay(wayId: Long, coords: Seq[((Double, Double), Int, Long)]): (Long, Seq[(Double, Double)]) = (wayId, coords.sortBy(_._2).map{_._1} )

  def load(defaulConfig: SparkConf, input:String, output:String ): Unit = {

    val conf = defaulConfig.setAppName("PocDrivingDistance Loader")
    conf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")

    val sc = new SparkContext(conf)
    val errorCounter = sc.longAccumulator

    var usefulData = sc.binaryFiles(input)
      .flatMap { case (path, binaryBlob) => parseBlob(path, binaryBlob.toArray(), errorCounter) }
      .flatMap(extractUsefulData)
      .cache()

    val nodesInfo = usefulData.flatMap(filterNodeData)
    val waysInfo = usefulData.flatMap(filterWayData)

    usefulData.unpersist()

    waysInfo.join(nodesInfo)
      .map{case (nodeId, ((wayId,pos), coords)) => (wayId, (coords, pos, nodeId))}
      .groupByKey()
      .map{case ( (wayId, coords) ) => generateWay(wayId, coords.toSeq)}
      .saveAsTextFile(output)

    sc.stop()
  }


  def main(args: Array[String]): Unit = {


  }

}
