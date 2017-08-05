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

import scala.annotation.tailrec
import scala.collection.mutable

class LoaderDriver {

  val log = LogManager.getRootLogger

//  case class Vertex(id:Long)

  /**
    * Transform the file into a blob.
    *
    * @param path
    * @param bin
    * @param errorCounter
    * @return
    */
  def parseBlob(path: String, bin: Array[Byte], errorCounter: LongAccumulator): Option[Seq[OSMEntity]] = {
    try {
      Some(EntityIterator.fromBlob(Blob.parseFrom(bin)).toSeq)
    } catch {
      case _ => {
        errorCounter.add(1)
        log.error(s"Error reading blob file ${path}", _)
        None
      }
    }
  }

//  /**
//    * Generate an in-memory temporal index with all nodes of the block.
//    * It will be used to generate the Way shape and length (weight)
//    *
//    * @param blob
//    * @return
//    */
//  def extractNodes(blob: Blob): Map[Long, (Double, Double)] =
//    EntityIterator.fromBlob(blob).flatMap {
//      case node if node.osmModel == OSMTypes.Node => {
//        val nodeEntity = node.asInstanceOf[NodeEntity]
//        Some(nodeEntity.id -> (nodeEntity.longitude, nodeEntity.latitude))
//      }
//      case _ => None
//    }.toMap

  /**
    * Transform the blob into a set of vertices. Every node, one vertex.
    *
    * @param entities secuence of tuples (Node id, Way id, Order in the way)
    * @return
    */
  def toNetworkTuples(entities: Seq[OSMEntity]): Seq[(Long, (Long, Int))] = {
    def genTuples(wayEntity: WayEntity): Seq[(Long, (Long, Int))]  = wayEntity.nodes.zipWithIndex.map { case(node, idx) => (node, (wayEntity.id, idx))}
    entities.flatMap {
      case way if way.osmModel == OSMTypes.Way => Some( genTuples(way.asInstanceOf[WayEntity]))
      case other => None
    }.flatten
  }

  def processBlob(path: String, bin: Array[Byte], errorCounter: LongAccumulator): Seq[(Long, (Long, Int))] =
    parseBlob(path, bin, errorCounter).flatMap(toNetworkTuples)(collection.breakOut)


  def loader(defaulConfig: SparkConf, appConfig: AppConfig): Unit = {

    val conf = defaulConfig.setAppName("PocDrivingDistance Loader")
    val sc = new SparkContext(conf)
    val errorCounter = new LongAccumulator

    sc.binaryFiles(appConfig.input)
      .flatMap { case (path, binaryBlob) => processBlob(path, binaryBlob.toArray(), errorCounter) }
      .groupByKey

    sc.stop()
  }


  def main(args: Array[String]): Unit = {


  }

}
