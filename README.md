# Driving Distance Algorithm implementation.
Remind: This is a *PoC* so it's not for production at all.

This PoC is a reasearch to generate create a service to generate "Areas of coverage" or "Driving Distance".
The first implementation is using AKKA to implement an in-memory database to represent the Graph and the algorithm to generate the map of nodes.

All information used to generate the network is using info from OpenStreetMap and the library [osm4scala](https://github.com/angelcervera/osm4scala) to parse the file in Scala.

## Information
### How to know the way direccion
Basically, all ways with the tag [highway](https://wiki.openstreetmap.org/wiki/Key:highway) are bidirectional, except when the
tag [oneway](https://wiki.openstreetmap.org/wiki/Key:oneway) is present. Depending of the value:
- If the value is "yes" the direction the natural drawn direction.
- If the values is "-1", the direction the opposite to the natural drawn direction.
- If the value is "no" or the tag is not present, it is a bidirectional way.

## Info:
- [osm4scala](https://github.com/angelcervera/osm4scala)
- [oneway tag](https://wiki.openstreetmap.org/wiki/Key:oneway)
- [Forward & backward, left & right](https://wiki.openstreetmap.org/wiki/Forward_%26_backward,_left_%26_right)
- [Identify direction in the editor](https://wiki.openstreetmap.org/wiki/Forward_%26_backward,_left_%26_right#Identifying_the_direction_of_a_.27way.27)
- [JAI](https://github.com/geosolutions-it/jai-ext)
- [GDAL](http://gdal.org/)
- [GeoTools](http://geotools.org/)
- [Temnur's isolines](https://bitbucket.org/temnur/isoline/wiki/Home)

## Metrics.
### Time transferring all blocks of the full planet
```bash
~/apps/hadoop-2.7.4$ time bin/hdfs dfs -put /tmp/blocks osm/planet

real	41m50.689s
user	3m17.480s
sys	2m9.436s
```

## Next steps approachs:
- Store one street per Actor, instead a node, so we save a lot of calls between nodes and it is necessary only check if we passed before by the street and not be the node.
- Store a set of streets (per osm block) per Actor.
