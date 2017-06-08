Topo Filter Service
===================

Filter service API.

### Function Descriptions

`initFilter(api)`
* `api`: reverse linkage to topoForce module
  * `node()`: reference to D3 node selection
  * `link()`: reference to D3 link selection
* Initializes this module with a function API to other modules

`clickAction()`
* Increments filter mode index and re-renders nodes and links
  to highlight those in the newly selected layer

`selected()`
* Returns the currently selected "layer"
  * (`all`, `pkt`, or `opt`)

`inLayer(d, layer)`
* `d`: model data (link, host, device)
* `layer`: layer id (e.g. `pkt`)
* Returns true if the given element is "in" the specified layer
