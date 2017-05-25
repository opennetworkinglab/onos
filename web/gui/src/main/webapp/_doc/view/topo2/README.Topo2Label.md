ONOS Web UI - Topo2Label Documentation
====================================

This is a base class for creating a label

#Exposed methods

## initialize()
## onChange()
By default:
updates the label text if `this.set({label: 'value'})` is called
updates the position if `this.set({x: 'value', y: 'value'})` is called

## applyStyles()
Applies the style property (`this.get('styles')`) to the DOM elements

## renderText()
Creates the default rendering for the text

## renderIcon()
Creates the default rendering for the icon

## render()
Creates the default DOM structure for the entire label.
Calls `this.renderIcon()` and `this.renderText()`

## remove()
Removes the label from the DOM

# Overrides
## setPosition()
Must set properties x and y
```Javascript
this.set({
    x: 10,
    y: 20,
})
```

## setScale()
Called whenever a user scales the topology view

## beforeRender()
## afterRender()