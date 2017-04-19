ONOS Web UI - Topology View
===========================

Documentation for the Web UI _Topology View_.

[pic]: picture-icon.png 
[topo]: https://wiki.onosproject.org/display/ONOS/GUI+Topology+View

([Wiki Page][topo])

### Overview
The _topology view_ provides a visual (cluster-wide) overview of the network 
topology controlled by ONOS. When the topology view is instantiated it 
requests topology information from the server; on receipt of that information, 
the view renders a visualization of devices, hosts, and the links between them. 
The view uses the web-socket connection established by the UI framework to
allow the server to drive updates to the view via topology events 
(such as _addHost_, _updateDevice_, etc.)

![Sample image of 3-node cluster][pic]


### Quick help
Pressing slash `/` or backslash `\ ` will bring up the _Quick Help_ panel.
This gives an outline of the keystroke commands and mouse gestures available
to you in the _Topology View_. Pressing either of these keys again (or pressing
`Esc` will dismiss the panel).

![Image of Quick Help panel][pic]

* The top section lists global key-bindings (available on every view in the UI)
* The middle section lists view-specific bindings
  * The first and second columns show general commands for the 
    _Topology View_
  * The third column shows commands for the currently active 
    "topology overlay" (if any)
* The bottom section lists view-specific mouse gestures and other notes

### Toolbar
The key-bindings (listed in _Quick Help_) are also associated with buttons
on the toolbar. (This facilitates using the UI on a smart tablet).
The toolbar is initially hidden, but clicking on the arrow, or pressing
dot (`.`) will toggle its state.

The toolbar has three rows of buttons:

* The first row and half the second row provide basic functions
* The second half of the second row provides a radio-button-set 
  of installed "overlays"
* The third row contains buttons contributed by the currently-active "overlay"

Hovering the mouse over a toolbar button will display a tooltip showing a
description of the button, and listing the key binding, e.g.
`Toggle Summary Panel (O)`.

#### Toolbar First Row

> note: wiki page should format this in a table, and include button icons

* `I` - show/hide ONOS cluster instance panel
* `O` - show/hide ONOS summary panel
* `D` - disable/enable details panel
  + The details panel is enabled by default, and is displayed when one or
    more topology elements are selected. Disabling this panel keeps it 
    hidden even when something is selected.
* `H` - toggle host visibility
  + Shows or hides the hosts (and their links).
* `M` - toggle offline-device visibility
  + Devices that are offline (but that ONOS still knows about) are shown
    by default. This toggle will hide offline devices (and any hosts/links)
    connected to them).
* `P` - Toggle port highlighting
  + Port highlighting displays port numbers on links when the mouse
        hovers over the link. This feature can be disabled with this toggle.
* `B` - Toggle background geo map
  + The background geo-based map (if one is selected) can be shown or hidden.
* `G` - Select background geo map
  + Opens a dialog box which allows selection of a geographic region from
    a pre-defined set.
* `S` - Toggle sprite layer
  + The sprite layer (static shapes / text injected into the view) can be
    shown or hidden with this toggle.

#### Toolbar Second Row

(tbd) 

#### Toolbar Third Row

(tbd)


#### Overlays
The ONOS Web UI comes bundled with the _Traffic Overlay_, which provides
traffic visualization functionality. Other applications running on ONOS
may also register topology overlays, which can be used to provide alternate
visualizations on the topology view.

> `F1` will select "no overlay active"; `F2` will select the traffic overlay.
> `F3`, `F4`, ... will select additional overlays, if they are registered and
> appear in the toolbar.

(WIP --- to be completed)
