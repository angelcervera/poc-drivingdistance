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

package com.simplexportal.spatial.loader

import java.util.Properties

import com.acervera.osm4scala.EntityIterator
import com.acervera.osm4scala.model.{NodeEntity, OSMEntity, OSMTypes, WayEntity}
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.simplexportal.spatial.model.{Location, Node, Way}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.spark.{HashPartitioner, SparkConf, SparkContext}
import org.openstreetmap.osmosis.osmbinary.fileformat.Blob
import org.apache.log4j.Logger
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel
import org.apache.spark.util.LongAccumulator

object LoaderFromOSMDriver {

  val log = Logger.getLogger("com.simplexportal.spatial.loader.LoaderFromOSMDriver")

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

  /**
    * Extract all nodes information from osmNode entities.
    *
    * @param osmEntity
    * @return
    */
  def extractNodes(osmEntity: OSMEntity): Option[(Long, Node)] = osmEntity.osmModel match {
    case OSMTypes.Node => {
      val node = osmEntity.asInstanceOf[NodeEntity]
      Some((node.id, Node(node.id, Location(node.latitude, node.longitude), node.tags)))
    }
    case _ => None
  }

  /**
    * Extract pairs of nodesId and wayId.
    *
    * @param osmEntity
    * @return Pair with the (nodeId, (wayId, node position))
    */
  def extractWayNodes(osmEntity: OSMEntity): Seq[(Long, (Long, Int))] = extractWay(osmEntity) match {
    case None => Seq()
    case Some(way) => way.nodes.zipWithIndex.map { case(nodeId, idx) => (nodeId, (way.id, idx))}
  }

  def extractWay(osmEntity: OSMEntity): Option[WayEntity] = osmEntity.osmModel match {
    case OSMTypes.Way => Some(osmEntity.asInstanceOf[WayEntity])
    case _ => None
  }

  /**
    * From a list of non orders pais of Nodes and position, generate the list of ordered Nodes.
    *
    * @param coords
    * @return
    */
  def generateWayNodeSeq(coords: Seq[(Node, Int)]): Seq[Node] = coords.sortBy(_._2).map{_._1}


  /**
    * Per every wayId, create a ordered list of Nodes that form the way.
    *
    * @param nodeIdsFromWay Set of (nodeIdx, (wayId, position)) that for the list of nodes per way.
    * @param nodes Set of (nodeId, Node) that represent the data set of nodes.
    * @return Set of (wayId, Seq[Node]) that represent the ordered set of nodes that form the way.
    */
  def calculateOrederedNodesPerWay(nodeIdsFromWay: RDD[(Long, (Long, Int))], nodes: RDD[(Long, Node)]) =
    nodeIdsFromWay.join(nodes)
      .map { case (nodeId, ((wayId, position), node)) => (wayId, (node, position)) }
      .aggregateByKey(Seq[(Node, Int)]())(
        (prev: Seq[(Node, Int)], node: (Node, Int)) => prev :+ node,
        (a: Seq[(Node, Int)], b: Seq[(Node, Int)]) => (a ++ b).sortBy(_._2)
      )
      .map { case (wayId, nodes) => (wayId, nodes.map(_._1)) }


  /**
    * Create all combinations of paths using this node.
    *
    * @param nodeId
    * @param ways
    * @return
    */
  def spreadWays(nodeId: Long, ways: Seq[Long] ): Seq[(Long, (Long, Seq[Long]))] = ways.map(sourceWayId => (sourceWayId, (nodeId, ways.filter(_ != sourceWayId))))

  /**
    *
    * @param nodeIdsFromWay
    * @return (wayId, Seq[NodeId, Seq[WayID] ])
    */
  def calculateIntersectionNodesPerWay(nodeIdsFromWay: RDD[(Long, (Long, Int))]): RDD[ (Long, Seq[(Long, Seq[Long])]) ] =
    nodeIdsFromWay
      .map{case( (nodeId, (wayId, _)) ) => (nodeId, Seq(wayId))}
      .reduceByKey( (a:Seq[Long],b:Seq[Long])=>a++b )
      .filter(_._2.size>1) // In this point, we have only intersections in the form (nodeId, Seq[wayId])
      .flatMap{ case(nodeId, ways) => spreadWays(nodeId, ways) } // Data set of (wayId, (nodeId, Seq[wayId]))
      .aggregateByKey(Seq[(Long, Seq[Long])]())(
        (prev:Seq[(Long, Seq[Long])], nodeWaysRelation: (Long, Seq[Long])) => prev :+ nodeWaysRelation,
        (a:Seq[(Long, Seq[Long])], b: Seq[(Long, Seq[Long])] ) => a ++ b
      )


  def buildIntersections(intersOpt: Option[Seq[(Long, Seq[Long])]]): Map[Long, Seq[Long]] = intersOpt match {
    case None => Map()
    case Some(intersections) => intersections.toMap
  }

  def generateNetwork(sc: SparkContext, input:String): RDD[Way] = {

    val errorCounter = sc.longAccumulator

    val osmEntities = sc.binaryFiles(input)
      .flatMap { case (path, binaryBlob) => parseBlob(path, binaryBlob.toArray(), errorCounter) }
      .persist(StorageLevel.DISK_ONLY)

    // (nodeId, (wayId, pos))
    val nodeIdsFromWay: RDD[(Long, (Long, Int))] = osmEntities.flatMap(extractWayNodes).persist()

    val nodes: RDD[(Long, Node)] = osmEntities.flatMap(extractNodes) // (nodeId, node)

    // USe the same partitioner to put all data related with the same way in the same node.
    val partitionByWayId = new HashPartitioner(100)

    // Create a list ordered nodes that form the way line, to fill nodes and bbox.
    val nodesPerWay = calculateOrederedNodesPerWay(nodeIdsFromWay, nodes).partitionBy(partitionByWayId)

    // Create a list with all possible paths from the every intersection in the way.
    val intersectionPerWay = calculateIntersectionNodesPerWay(nodeIdsFromWay).partitionBy(partitionByWayId)

    // Extract direct info from OsmEntity that we are going to use with no manipulation.
    val waysInfo = osmEntities.flatMap(extractWay).map(way=>(way.id, way.tags)).partitionBy(partitionByWayId)

    // join intersections and nodes to be able to complete way information in the next join.
    waysInfo
      .join(nodesPerWay.fullOuterJoin(intersectionPerWay))
      .map{ case ( (wayId, (tags, (nodes, intersections))) ) => Way(
        id = wayId,
        nodes = nodes.getOrElse(Seq()),
        tags = tags,
        intersections = buildIntersections(intersections)
      )}
  }

  /**
    * Publish and iterable sequence of ways into Kafka.
    *
    */
  def publishToKafka(ways: Iterator[Way], brokers: String, topic: String) = {

    // Create Kafka connection.
    val kafkaProps = new Properties()
    kafkaProps.put("bootstrap.servers", brokers)
    kafkaProps.put("client.id", "PocDrivingDistance")
    kafkaProps.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    kafkaProps.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    kafkaProps.put("acks", "1")
    kafkaProps.put("producer.type", "async")

    // Mapper per partition
    val mapper = new ObjectMapper()
    mapper.registerModule(DefaultScalaModule)

    val producer = new KafkaProducer[String, String](kafkaProps)

    // Publish all ways.
    try {
      ways.foreach(way => producer.send(new ProducerRecord[String, String](topic, mapper.writeValueAsString(way))))
    } finally {
      producer.close()
    }
  }

  def load(defaulConfig: SparkConf, input:String, topic:String = "PocDrivingDistance", brokers: String = "localhost:9092" ) = {

    val conf = defaulConfig.setAppName("PocDrivingDistance Loader")
    conf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")

    val sc = new SparkContext(conf)
    try {
      generateNetwork(sc, input)
          .foreachPartition(ways => publishToKafka(ways, brokers, topic))
    } finally {
      sc.stop()
    }

  }

  def main(args: Array[String]): Unit = {
    val Array(input, topic, brokers) = args
    load(new SparkConf().setAppName("PocDrivingDistance Loader"), input, topic, brokers)
  }

}
