package com.simplexportal.spatial.loader

import java.util.Properties

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.simplexportal.spatial.loader.LoaderFromOSMDriver.load
import com.simplexportal.spatial.model.Way
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD

/**
  * Generate network and export it to Kafka.
  */
object LoaderFromOSMToKafkaDriver {

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

  /**
    * Execute the process.
    * Three parameters:
    * 1. Folder that contains OSM blocks.
    * 2. Kafka topic. Example: PocDrivingDistance
    * 3. Brokers. Example: localhost:9092
    */
  def main(args: Array[String]): Unit = {
    val Array(input, topic, brokers) = args
    def publisher(ways: RDD[Way]): Unit = ways.foreachPartition(ways => publishToKafka(ways, brokers, topic))
    load(new SparkConf().setAppName("PocDrivingDistance Loader to Kafka"), input, publisher)
  }

}
