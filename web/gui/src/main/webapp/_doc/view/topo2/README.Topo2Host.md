ONOS Web UI - Topo2Host Documentation
====================================

Returns a new collection of Host Models

# Collection
## Exposed methods
See README.Topo2Collection

#Model
## Exposed methods

### initialize()
Calls the super method.

### onChange()
Called when the model properties change.
Updates the classNames, visibility and Device Color.

### showDetails()
Displays the details panel

### icon()
Returns the icon to be used for the device

### label()
Returns the label for the host

### setScale()
Determines the scale of the icon

### setVisibility()
Hides or Shows according to the value specified in the topo2_prefs['offline_devices']

### onEnter()
Creates a host node to be placed within the DOM
