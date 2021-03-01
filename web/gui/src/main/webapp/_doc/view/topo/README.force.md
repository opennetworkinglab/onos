Topo Force Service
==================

Force service API.

### Function Descriptions

`initForce(svg, forceG, uplink, dim, opts)`
* `svg`: D3 selection of _svg_ element for force layout rendering
* `forceG`: D3 selection of force layout _SVG_ group element
* `uplink`: API uplink to main _topo.js_ module
  * `showNoDevs(b)` - show or hide _no connected devices_ message
  * `projection()` - return ref to map projection object
  * `zoomLayer()` - return ref to zoom layer element
  * `zoomer()` - return ref to zoomer object
  * `opacifyMap(b)` - show or hide map layer
  * `topoStartDone()` - callback invoked after topo data
     has been received from server
* `dim`: initial dimensions of _SVG_ ... `[w, h]`
* `opts`: options object
  * can be used to override default settings
  * _gravity_, _friction_, _charge_, _linkDistance_, _linkStrength_

`newDim()`
* `dim`: new dimensions ... `[w, h]`
* Sets new dimensions of force layout

`destroyForce()`
* Frees up all resources, cancels timers, cleans up DOM

`updateDeviceColors()`
* Delegates to _topoD3.js_ function of same name

`toggleHosts(x)`
* `x`: boolean (optional)
* If `x` is not defined, toggles host visibility
* If `x` is defined, sets or clears host visibility

`togglePorts()`
* `x`: boolean (optional)
* If `x` is not defined, toggles port labels visibility
* If `x` is defined, sets or clears port labels visibility

`toggleOffline()`
* `x`: boolean (optional)
* If `x` is not defined, toggles offline devices visibility
* If `x` is defined, sets or clears offline devices visibility

`cycleDeviceLabels()`
* Increments the device label mode and re-renders device labels

`unpin()`
* will _unpin_ a node over which the mouse currently hovers

`showMastership(masterId)`
* `masterId`: ONOS instance identifier (e.g. IP address)
* If `masterId` is defined, will render the display to highlight
  those devices mastered by the given cluster member
* If `masterId` is not defined, restores the display

`showBadLinks()`
* Will briefly highlight links for which there is only a single backing link
* Also writes a summary of bad links to the console

`adjustNodeScale()`
* Rescales the nodes and links (and labels) based on the current zoomer state

`resetAllLocations()`
* Resets all nodes (hosts, devices) to the configured positions

`addDevice(data)`
* `data`: add-device event payload
* Adds a device to the model (and renders it)

`updateDevice(data)`
* `data`: update-device event payload
* Updates information for a device (re-rendering it)

`removeDevice(data)`
* `data`: remove-device event payload
* Removes a device from the model (and animates its removal)

`addHost(data)`
* `data`: add-host event payload
* Adds a host to the model (and renders it if hosts visible)

`updateHost(data)`
* `data`: update-host event payload
* Updates information for a host (re-rendering it)

`moveHost(data)`
* `data`: move-host event payload
* Updates information for a host to move it to a new location
  (re-rendering it if hosts visible)

`removeHost(data)`
* `data`: remove-host event payload
* Removes host from the model (and animates removal if hosts visible)

`addLink(data)`
* `data`: add-link event payload
* Adds a link to the model (and renders it)

`updateLink(data)`
* `data`: update-link event payload
* Updates information for a link (re-rendering it)

`removeLink(data)`
* `data`: remove-link event payload
* Removes link from model (and animates removal)

`topoStartDone()`
* Callback invoked once the server signals that all topology data
  has been transferred
