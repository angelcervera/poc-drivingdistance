package com.simplexportal.spatial

import better.files.File
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.simplexportal.spatial.model.Way

trait TestUtilities {
  val mapper = new ObjectMapper() with ScalaObjectMapper
  mapper.registerModule(DefaultScalaModule)

  /**
    *
    * @param folder
    * @param exec
    */
  def readBulks(folder:File, exec: (Seq[Way])=>Any) =
    folder
      .children
      .foreach(file => exec(file.lines.map(mapper.readValue(_, classOf[Way])).toSeq))

}
