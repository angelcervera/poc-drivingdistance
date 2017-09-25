package com.simplexportal.spatial.drivingdistance.model

// TODO: Store point as offsets to save space.
// TODO: Extract all tags a create a Lookup to save space.

/**
  * Represent a geoposition.
  *
  * @param latitude
  * @param longitude
  */
case class Location(latitude: Double, longitude: Double)

/**
  * Represent a node in a Way
  *
  * @param id OSM ID
  * @param coords Location
  * @param tags OSM tags
  */
case class Node(id:Long, coords: Location , tags: Map[String, String])

/**
  * Represent one way in the network.
  * Every intersection is represented as entry (Node id -? Seq[Way id])
  *
  * @param id OSM ID
  * @param nodes List of nodes that form the way shape.
  * @param tags OSM tags
  * @param intersections Intersections represented the relation between every intersection node in the way with other ways Map[ nodeId, Seq[wayId] ]
  */
case class Way(id:Long, nodes: Seq[Node], tags: Map[String, String], intersections: Map[Long, Seq[Long]])
