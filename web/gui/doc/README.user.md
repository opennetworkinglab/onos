ONOS Web UI - User Documentation
================================

#### Note to Reader

This document provides an overview of the _User Documentation_ for 
the _ONOS Web UI_. 
 
([Wiki Page][wiki], including screenshots) 

 
Further content (subsections) may be found in files named `README.user.*.md`

Placeholders for screenshots / images are marked thusly:

![sample image placeholder][pic]

[wiki]: https://wiki.onosproject.org/display/ONOS/The+ONOS+Web+GUI
[pic]: picture-icon.png 



Introduction
------------

#### Overview

The ONOS GUI is a _single-page web application_, providing a visual
interface to the ONOS controller (or cluster of controllers).

For documentation on the design of the GUI, see [Web UI Architecture][ua] in
the Architecture Guide.

For documentation on how applications running on ONOS can inject content
into the GUI at runtime, see the [Web UI Tutorials][ut] page.

The [Developer Guide][dg] includes documentation on the Web UI framework 
libraries (for both _client-side_ and _server-side_ modules), to help app
developers re-use common coding patterns.


[ua]: https://wiki.onosproject.org/display/ONOS/Web+UI+Architecture
[ut]: https://wiki.onosproject.org/display/ONOS/Web+UI+Tutorials
[dg]: https://wiki.onosproject.org/display/ONOS/Appendix+F%3A+Web+UI+Framework+Libraries


----
#### Configuration Notes

* The **_onos-gui_** feature must be installed in ONOS
* The GUI listens on port **_8181_**
* The base url is `/onos/ui`
  + for example, to access the GUI on _localhost_, use
    `http://localhost:8181/onos/ui`   
* The GUI has been developed to work on _Google Chrome_. 
  The GUI has been tested on _Safari_ and _Firefox_ and minor compatibility 
  adjustments have been made; these and other browsers may work, but have 
  not been extensively tested, and are not actively supported, at this time.
* The key bindings associated with any view will work on any keyboard. 
  The "Cmd" (⌘) key on an Apple keyboard is bound to the same key as 
  the "Windows" or "Alt" keys on Windows or other keyboards.


----
#### Session Notes

Note that the current version of the GUI does not fully support the concept
of individual user accounts, however, login credentials are required.

On launching the GUI you should see the login screen:

![image of login screen][pic]

Default username and password are `onos/rocks`. 

If ONOS was installed via `onos-install` and configured by `onos-secure-ssh`
(developer / test tools), then the _username / password_ may be different; 
examine the `$ONOS_WEB_USER` and `$ONOS_WEB_PASS` environment variables to
discover what they are.

After a successful login, you should see a screen that looks like this:

![image of topology view][pic]

The dark bar at the top is the _Masthead_, which provides a location for
general GUI controls:

- Navigation Menu Button
- ONOS logo and title
- Context help button (click to open web URL specific to view)
- User name (click to access `logout` action)

(In future versions, the masthead may include session controls such as 
user preferences, global search, etc.)

The remainder of the screen is the "view", which defaults to the 
Topology View when the GUI is first loaded – a cluster-wide view of the 
network topology.

* The _ONOS Cluster Node Panel_ indicates the cluster members 
  (controller instances) in the cluster.
* The _Summary Panel_ gives a brief summary of properties of the 
  network topology.
* The _Topology Toolbar_ (initially hidden) provides 
  push-button / toggle-button actions that interact with the topology view.
  
For more detailed information about this view, see the [Topology View][topo] page.


----
#### Navigation

Other views can be "navigated to" by clicking on the _Navigation Menu Button_ 
in the masthead, then selecting an item from the dropdown menu:

![image of navigation menu][pic]

----
#### Views

The GUI is cacapable of supporting multiple views. As new views are added 
to the base release, they will be documented here.

> NOTE:
> The capability of adding views to the GUI dynamically at run-time is also 
> available to developers, allowing, for example, an ONOS App developer to 
> create GUI content that works specifically with their application. 
> The content will be injected dynamically into the GUI when the app is 
> installed, and removed automatically from the GUI when the app is 
> uninstalled. For more details on this feature, see the 
> [Web UI tutorials][ut].

The views currently included in the base release are:

- Platform Category
  + Applications
    + The [Application View][app] provides a listing of installed
      applications, as well as the ability to _install, start, stop
      and uninstall_ them.
  + Settings
    + The [Settings View][set] provides information about all configurable
      settings in the system. (Currently this is a readonly view, but 
      future releases may provide setting adjustments from here).
  + Cluster Nodes
    + The [Cluster Node View][cnode] provides a top level listing of all
      the nodes (ONOS instances) in the cluster.
  + Packet Processors
    + The [Packet Processors View][pkt] shows the currently configured
      components that participate in the processing of packets
      sent to the controller.
  + Partitions
    + The [Partitions View][part] shows the cluster partitions.
  
- Network Category
  + Topology
    + The [Topology View][topo] provides an interactive visualization of the
      network topology, including an indication of which devices (switches)
      are mastered by each ONOS controller instance.
  + Topology 2 (experimental)
    + The [Topology 2 View][topo2] is a "region aware" view of the topology
      which can take advantage of an administrator configuring the network
      into regions.
      * Note that this view is _currently experimental_
  + Devices
    + The [Devices View][dev] provides a top level listing of the 
      devices in the network. Note that when a device in the table is
      selected, additional views (not directly available from the navigation
      menu) become available for that device:
      + [Flows][flow]: shows all flows for the selected device
      + [Ports][port]: shows all ports for the selected device
      + [Groups][group]: shows all groups for the selected device
      + [Meters][meter]: shows all meters for the selected device
  + Links 
    + The [Links View][link] provides a top level listing of all the
      links in the network.
  + Hosts
    + The [Hosts View][host] provides a top level listing of all the
      hosts in the network.
  + Intents
    + The [Intents View][intent] provides a top level listing of all the
      intents in the network.
  + Tunnels
    + The [Tunnels View][tunnel] provides a top level listing of all
      tunnels defined in the network.
    
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

Note that many of the views are table-based, and are similar in look 
and interaction. For a general overview of tabular usage, see the 
[Tabular View][table] page.

[table]: https://wiki.onosproject.org/display/ONOS/GUI+Tabular+View

> See `README.user.views.md` for view specific documentation.

[README-user-views]: README.user.views.md

                                                                               
----
#### Web UI Applications

ONOS applications may contain Web UI components - either custom views
or topology overlay behaviors.
For documentation on application-specific behavior, please see the
[Web UI Application Index][appidx].

> NOTE: For applications that are distributed with core ONOS, it is expected
> that the developer provides a wiki page for documentation, and provides a
> link in the index page.

[appidx]: https://wiki.onosproject.org/display/ONOS/Web+UI+Application+Index

----
#### Release Nodes

Please see `README.user.releases.md` for a summary of UI features introduced
at each release of ONOS.
