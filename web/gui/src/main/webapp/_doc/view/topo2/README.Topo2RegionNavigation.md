ONOS Web UI - Topo2RegionNavigation.js Documentation
====================================

# Internal methods
## notifyListeners(el)
Adds a DOM node to the body

## navigateToRegion(id: string)
Requests the region nodes via WebSockets
Notifies any listeners that a regionNavigation is about to happen ('region:navigation-start')

## navigateToRegionComplete()
Notifies any listeners that a regionNavigation has completed ('region:navigation-complete')

#Exposed methods
## addListener()
Ensures an empty panel and creates the content regions (header, body, footer)

## destroy()
Resets state and removes the listeners

