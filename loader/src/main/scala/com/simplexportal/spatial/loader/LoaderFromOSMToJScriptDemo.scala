package com.simplexportal.spatial.loader

import com.simplexportal.spatial.loader.LoaderFromOSMDriver.load
import com.simplexportal.spatial.model.Way
import org.apache.spark.SparkConf
import org.apache.spark.rdd.RDD
import better.files.{File, _}
import java.io.{File => JFile}

/**
  * Generate network and export it to be used as data input in the visualization.
  */
object LoaderFromOSMToJScriptDemoDriver {

  def roundFiveDecimals(v:Double): Double = Math.round(v * 100000d) / 100000d

  def publishToJScriptDemo(ways: Iterator[Way]) = {

    // Publish all ways as json.
    ways
      .map(way => {
        val strNodes = way.nodes.map(node=>s"[${node.id},${roundFiveDecimals(node.coords.latitude)},${roundFiveDecimals(node.coords.longitude)}]").mkString("[", ",","]")
        s"[${way.id},${strNodes}]"
      })
  }

  /**
    * Execute the process.
    * Three parameters:
    * 1. Folder that contains OSM blocks.
    * 2. Folder to store the result.
    */
  def main(args: Array[String]): Unit = {
    val Array(input, tmp, output) = args

    //    topic:String = "PocDrivingDistance", brokers: String = "localhost:9092"
    def publisher(ways: RDD[Way]): Unit = {

      // Generate all data
      ways
        .mapPartitions(ways => publishToJScriptDemo(ways))
        .coalesce(1)
        .saveAsTextFile(tmp)

      val lines = File(s"${tmp}//part-00000").lines
      val h = lines.head
      val out = File(output).append(s"[ ${h}")
      lines.tail.foreach(line=>out.append(s",${line}"))
      out.append("]")

    }

    load(new SparkConf().setAppName("PocDrivingDistance Loader"), input, publisher)
  }

}
