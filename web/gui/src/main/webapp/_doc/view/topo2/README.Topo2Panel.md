ONOS Web UI - Topo2Panel.js Documentation
====================================

Base class for a Panel in Topo2

#Exposed methods
## initialize()
calls `super()`

## setup()
Ensures an empty panel and creates the content regions (header, body, footer)

## appendToHeader(el)
Adds a DOM node to the Header

## appendToBody(el)
Adds a DOM node to the body

## appendToFooter(el)
Adds a DOM node to the footer

## emptyRegions()
Clears the Header, Body and Footer of the Panel

## destroy()
Removes the Panel from the DOM

## isVisible()
Returns the visibility state of the Panel
