ONOS Web UI - Topo2Zoom.js Documentation
====================================

API Subject to change soon.

# Methods
## getZoomer
returns the internal reference to the zoomer

## createZoomer
creates a new d3.zoom layer

## addZoomEventListener(callback: function)
Subscribes a callback to the zoom event

## removeZoomEventListener(callback: function)
Removes callback subscription

## scale
Returns the zoomer scale values

## AdjustmentScale(min: int, max: int)
A helper method to limit the visual size of an object.
Cap on how small an object can become and also how large it can be.

## panAndZoom(translate: array, scale: number, transition: int)
Pans and zooms the zoom layer over a given (`transition`) time.
If transition = 0 the pan and zoom is instant, else it is a time in milliseconds.


