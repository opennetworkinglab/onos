ONOS Web UI - Topo2NodePosition.js Documentation
====================================

Topo2Collection is a Class that contains a Collection of Models.

#Exposed methods
## positionNode(node: Model, forUpdate: bool)
Places a node within the Topology, will use `setLongLat` is location is available
or a default location if location is not set

## setLongLat(el: Model)
Positions node in the topology. Either with Lat and Long (geo) values or X and Y (grid)
