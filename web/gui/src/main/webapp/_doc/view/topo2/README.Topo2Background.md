ONOS Web UI - Topo2Background Documentation
====================================

Topo2Background is a class that manages which type or background is displayed for
the current region. It receives a JSON object from the Topo2CurrentLayout event determines the
type of background and then renders.

#Exposed methods
##init()
* Adds an svg group for the background
* Initiates the Map and Sprite Service
* Adds a zoomer to the local scope

##addLayout(data: json)
This method determines the type of background to to be shown with the
given `data`

##getBackgroundType()
Returns the current background type

##resetZoom()
Repositions the background pan and zoom to the default values specified in the background json
