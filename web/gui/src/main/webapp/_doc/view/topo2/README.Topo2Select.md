ONOS Web UI - Topo2Select.js Documentation
====================================

This class also watches mouse move events to highlight the nearest link.
This will eventually be refactored into it's own class.

#Internal methods
## updateDetails
Will update the visible details panel content with selected node(s).

## removeNode
Used to remove a node when multiselect is enabled and device is deselected

#Exposed methods
## init()
Sets up the internal references to the zoomer and SVG layer

## selectObject(node, multiSelectEnabled)
```
Node: The selected node in the topology
MultiSelectEnabled: (true/false) if multi select is enabled for the node type
```
Selects the node passed in to the method and updates internal cache

## clearSelection()
Empties the internal selection cache and updates the details panel accordingly


