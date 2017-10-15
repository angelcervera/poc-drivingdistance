package com.simplexportal.spatial

import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import better.files.File
import com.simplexportal.spatial.model.Way
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.apache.commons.io.{FileUtils, IOUtils}

import scala.util.{Failure, Success, Try}

trait TestUtilities {

  // Functor/Monad/Monoid/WTF!!!

  /**
    *
    * @param folder
    * @param exec
    */
  def execute(folder:File, exec: (Way)=>Unit): Unit =
    folder
      .children
      .foreach(file => executeFromLines(file.lines.toIterator, exec))

  def executeFromXZ(folder:File, exec: (Way)=>Unit): Unit =
    folder
      .children
      .foreach(file => uncompressXZ(file) match {
        case Success(lines) => executeFromLines(lines, exec)
        case Failure(ex) => throw ex
      })

  def executeFromLines(lines: Iterator[String], exec: (Way)=>Unit): Unit =
    lines.foreach(line =>
      decode[Way](line) match {
        case Right(way) => exec(way)
        case Left(error) => throw error
      }
    )

  def uncompressXZ(file:File): Try[Iterator[String]] = Try {
    val is = FileUtils.openInputStream(file.toJava)
    val xzIn = new XZCompressorInputStream(is)
    try {
      IOUtils.toString(xzIn, "UTF8").lines
    } finally {
      IOUtils.closeQuietly(xzIn)
      IOUtils.closeQuietly(is)
    }
  }

}
