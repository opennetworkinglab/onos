ONOS Web UI - Topo2SpriteLayer.js Documentation
====================================

This class is to be used by Topo2Background only and should not be called directly.

#Internal methods
## init
Sets up internal references when the class is created

## loadLayout
Loads the layout definition from the json payload.
Calls renderLayout when complete.
Returns a promise for consistency with Topo2Map

## createSpriteDefs
Creates and adds the sprite definitions for the layout to the dom

## getWidth
Returns width of the configured sprite layout

## getHeight
Returns height of the configured sprite layout

## renderSprite
Renders a specific sprite to the dom using the sprite definitions
and the configurations (width, height, fill, stroke , etc).

## renderLayout
Renders the SVG layout with the correct viewport dimentions
specified in the configuration

## renderGrid
For Debugging purposes renders a background grid to the DOM