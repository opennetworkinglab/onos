ONOS Web UI - Topo2Device Documentation
====================================

Returns a new collection of Device Models

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

### icon()
Returns the icon to be used for the device

### showDetails()
Displays the details panel

### displayMastership()
Sets the visibility of the Device for the selected mastership

### setOfflineVisibility()
Hides or Shows according to the value specified in the topo2_prefs['offline_devices']

### onExit()
Runs the exit animate and removal of the device
