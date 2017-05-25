ONOS Web UI - Topo2Force Documentation
======================================

#Exposed methods
## init
Setup the required services

## newDim(_dims_: array [w, h])
Updates the width and height of the viewport

## destroy
Unbinds all events and cleans up handlers

## topo2AllInstances(), topo2CurrentLayout(), topo2CurrentRegion(), topo2PeerRegions(), topo2UiModelEvent
WebSocketEvent entry point that passes the data on to the necessary services

## showMastership
Entry Point for the showMastership selection

## updateNodes
Updates all the nodes in the current region after the use of a keyboard shortcut. Subject to change.

## updateLinks
Updates all the links in the current region after the use of a keyboard shortcut. Subject to change.

## resetNodeLocation
Resets a node to its default coordinates. Used as an entry point for the keyboard event. Subject to change.

## unpin
Removes the fixed position of a node. Used as an entry point for the Keyboard shortcut. Subject to change.
