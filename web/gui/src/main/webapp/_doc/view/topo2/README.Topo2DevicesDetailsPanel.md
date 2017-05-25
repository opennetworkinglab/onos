ONOS Web UI - Topo2DeviceDetailsPanel Documentation
====================================

The details panel for devices

#Exposed methods
## init()
Binds handlers and creates a Panel instance

## updateDetails()
Requests the details via WebSockets and updates the view

## showMulti()
Lists the selected nodes (more than 1) in the details panel

## toggle()
Show or hide the details panel based on it's current visibility

## show()
Shows the details panel

## hide()
Hides the details panel

## destroy()
Removes the details panel and unbinds any websocket events

## isVisible()
returns the visibility state of the details panel

## getInstance()
returns the details panel instance