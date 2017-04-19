ONOS Web UI - Views
===================

Documentation for the Web UI main views.

> Note that each of these views should have their own wiki page.
> For convenience, we are documenting them all here in a single document.

[table]: https://wiki.onosproject.org/display/ONOS/GUI+Tabular+View
[pic]: picture-icon.png 

[app]: https://wiki.onosproject.org/display/ONOS/GUI+Application+View
[set]: https://wiki.onosproject.org/display/ONOS/GUI+Settings+View
[cnode]: https://wiki.onosproject.org/display/ONOS/GUI+Cluster+Node+View
[pkt]: https://wiki.onosproject.org/display/ONOS/ONOS+Packet+Processors+View
[part]: https://wiki.onosproject.org/display/ONOS/GUI+Partitions+View

[topo]: https://wiki.onosproject.org/display/ONOS/GUI+Topology+View
[topo2]: https://wiki.onosproject.org/display/ONOS/GUI+Topology+2+View
[dev]: https://wiki.onosproject.org/display/ONOS/GUI+Device+View
[flow]: https://wiki.onosproject.org/display/ONOS/GUI+Flow+View
[port]: https://wiki.onosproject.org/display/ONOS/GUI+Port+View
[group]: https://wiki.onosproject.org/display/ONOS/GUI+Group+View
[meter]: https://wiki.onosproject.org/display/ONOS/GUI+Meter+View
[link]: https://wiki.onosproject.org/display/ONOS/GUI+Link+View
[host]: https://wiki.onosproject.org/display/ONOS/GUI+Host+View
[intent]: https://wiki.onosproject.org/display/ONOS/GUI+Intent+View
[tunnel]: https://wiki.onosproject.org/display/ONOS/GUI+Tunnel+View

# Platform Category Views


Application View
----------------

([Wiki Page][app])

### Overview
The application view provides a top level listing of, and basic interaction
with, all installed applications. The applications are displayed in
[tabular form][table], where each row is a single application.

![screenshot of app view][pic]

Selecting a row will display a detail panel, containing more information 
about the selected application, including:
* Basic properties:
  * App ID
  * State
  * Category
  * Version
  * Origin
  * Role
* URL -- to application documentation page 
* Description
* Features, App dependencies, and Permissions (if any)

![Image showing a selected application, and its detail panel][pic]

Note that the first column will contain a checkmark, if the application is
currently active.

As with all table views, clicking on a column header will sort entries
by that column.

### Interacting with applications

An application can be installed simply by dragging and dropping 
an `.oar` file onto the application page. The page border will highlight 
when the "drop target" has been acquired.

Alternatively, pressing the "Install" (`+`) control button will bring up
a file selection dialog, with which you can select on `.oar` file.

The "Activate" (`>`) control button will start the selected application. 

The "Deactivate" (`[]`) control button will stop the selected application.

The "Uninstall" (`trashcan`) button will uninstall the application.

In each case, a confirmation dialog will pop up, asking you to verify the
action.


----

Settings View
-------------

([Wiki Page][set])

### Overview
The settings view lists all the tunable settings by component, showing for each:

* Component name
* Property name
* Property type
* Current value
* Description

> Values that are _not_ currently the _default_ value
> will be shown in **bold type**.

![Image showing settings view][pic]

Selecting a table row will display a detail panel for the corresponding setting. 

Currently, this view is read-only; future versions of the UI may 
support adjusting settings from this view.

> Note: the detail panel is where parameter editing would take place


----

Cluster Node  View
------------------

([Wiki Page][cnode])

### Overview
The cluster node view lists the cluster members, showing basic information 
for each:

* active
* started
* identifier
* IP address
* TCP port
* Last updated

Selecting a table row will display a detail panel for the selected node, 
listing each of the devices for which this node currently holds 
"mastership".

![Image showing selected node, and its detail panel][pic]


----

Packet Processors View
----------------------

([Wiki Page][pkt])

### Overview
The packet processors view lists each component that participates in 
the handling of incoming network packets, in the order that they are 
configured. Each entry shows:

* Priority
* Type _{advisor|director|observer}_
* Implementing class
* Packets processed
* Average processing time per packet _(ms)_

![Image showing packet processors table][pic]

Table row entries are not selectable.

----

Paritions View
--------------

([Wiki Page][part])

### Overview
The partitions view shows how partitions are configured on the cluster, one
table row per partition:

* Partition name
* Term
* Partition leader
* Partition members

![Image showing partition table][pic]

Table row entries are not selectable.

---- 

# Network Category Views

Topology View
-------------

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

See `README.user.topo.md` for details.

----

Topology 2 View
---------------

([Wiki Page][topo2])

### Overview
The topology 2 view ... (to be completed)

----

Devices View
------------

([Wiki Page][dev])

### Overview
The devices view ... (to be completed)

----

Flows View
----------

([Wiki Page][flow])

### Overview
The flows view ... (to be completed)

----

Ports View
----------

([Wiki Page][port])

### Overview
The ports view ... (to be completed)

----

Groups View
-----------

([Wiki Page][group])

### Overview
The groups view ... (to be completed)

----

Meters View
-----------

([Wiki Page][meter])

### Overview
The meters view ... (to be completed)

----

Links View
----------

([Wiki Page][link])

### Overview
The links view ... (to be completed)

----

Hosts View
----------

([Wiki Page][host])

### Overview
The hosts view ... (to be completed)

----

Intents View
------------

([Wiki Page][intent])

### Overview
The intents view ... (to be completed)

----

Tunnels View
------------

([Wiki Page][tunnel])

### Overview
The tunnels view ... (to be completed)

----