Topo D3 Service
===============

A collection of rendering functions, typically using the D3 library.


### Function Descriptions

`initD3(api, zoomer)`
* `api`: reverse linkage to topoForce module
  * `node()`: reference to D3 node selection
  * `link()`: reference to D3 link selection
  * `linkLabel()`: reference to D3 link label selection
  * `instVisible()`: returns true if instance panel is visible
  * `posNode(node, forUpdate)`: position node (see topoModel.js) 
  * `showHosts()`: returns true if hosts are to be displayed
  * `restyleLinkElement(ldata, immediate)`: link styling 
  * `updateLinkLabelModel()`: update link labels
  * `linkConfig()`: ref to link configuration data
  * `deviceScale()`: scale value for devices (based on current zoom)
  * `linkWidthScale()`: scale value for links (based on current zoom)
* `zoomer`: zoomer object
* Initializes this module with a function API to other modules

`destroyD3()`
* (currently a no-op)

`incDevLabIndex()`
* Increments the device label index (mod 3) and returns the
  text to show in the "flash" message

`setDevLabIndex(mode)`
* `mode`: label mode index (0, 1, 2)
* Sets the device label index to the given mode
* Stores this choice in user preferences

`hostLabel(d)`
* `d`: host model data
* Returns the label to display for the specified host, given
  the current host label mode

`deviceLabel(d)`
* `d`: device model data
* Returns the label to display for the specified device, given
  the current device label mode

`trimLabel(label)`
* `label`: label to trim
* Returns the specified label trimmed of whitespace

`updateDeviceLabel(d)`
* `d`: device model data
* Updates the rendering of the specified device; specifically
  the label and (if defined) the badge

`updateHostLabel(d)`
* `d`: host model data
* Updates the rendering of the label for the specified host 

`updateDeviceColors(d)`
* `d`: device model data (optional)
* Updates the color of the specified device 
  (showing instance mastership)
* If no parameter is given, updates the colors of _all_ devices

`deviceExisting(d)`
* `d`: device model data
* Updates rendering of an existing (not new) device

`hostExisting(d)`
* `d`: host model data
* Updates rendering of an existing (not new) host

`deviceEnter(d)`
* `d`: device model data
* Sets up and renders a new device

`hostEnter(d)`
* `d`: host model data
* Sets up and renders a new host

`hostExit(d)`
* `d`: host model data
* Animates removal of (and removes) specified host

`deviceExit(d)`
* `d`: device model data
* Animates removal of (and removes) specified device

`linkEntering(d)`
* `d`: link model data
* Sets up and renders a new link

`applyLinklabels()`
* Re-renders labels currently defined on link models

`transformLabel(p, id)`
* `p`: position data
* `id`: link id
* Returns translation string for label positioning

`applyPortLabels(data, portLabelG)`
* `data`: port label data
* `portLabelG`: port label SVG grouping element
* Creates and renders port labels on links

`applyNumLinkLabels(data, lblsG)`
* `data`: link label data
* `lblsG`: link label SVG grouping element
* Renders a "crosshatch link" with count for multiple links
