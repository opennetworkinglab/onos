ONOS Web UI - User Documentation
================================

### Release Notes

This section provides a reverse-chronological summary of changes 
to the GUI for each release.

([Wiki Page][wiki]) 

[wiki]: https://wiki.onosproject.org/display/ONOS/GUI+Release+Notes


----
#### Kingfisher -- 1.10.0 -- May 2017
+ (to be listed)

----
#### Junco -- 1.9.0 -- February 2017
+ Region aware topology view _(Topology 2)_
  + Still WIP
  + Region transition animation added
  + Sprite layers added
+ Partitions View added
+ Support for viewing selected intent via topology overlays added

----
#### Ibis -- 1.8.0 -- November 2016
+ Region aware topology view _(Topology 2)_
  + Network configurations added for _Regions_ and _TopoLayouts_
+ "Resubmit Intent" action added to _Intents View_
+ Topology View icons updated
+ Cluster View details panel added
+ Local filtering added to _Flow_, _Port_, _Group_, and _Meter_ views.

----
#### Hummingbird -- 1.7.0 -- August 2016
+ Region aware topology view _(Topology 2)_
  + Server-side modeling implemented
+ "Brief" mode introduced to _Intent_, _Group_, and _Flow_ views
+ Continued work on re-theming the UI

----
#### Goldeneye -- 1.6.0 -- May 2016
+ Overhaul of the "Look-n-Feel" (re-skinned)
+ Applications view:
  + Drag-n-drop of `.oar` files to install and activate, now supported
  + Auto-prompt user to refresh the UI when components are added / removed
+ Topology view:
  + "Cluster node ready" checkmark indicator added
  + Geo-map selection dialog (`G` keystroke) added
  + Support for topology overlays to provide custom link data
+ Dialog Service:
  + Support for chained dialog operations added
+ Chart Service added
+ User preferences now persisted server side
+ Logged-in user name displayed in masthead

----
#### Falcon -- 1.5.0 -- February 2016
+ Topology View:
  + "Reset node locations" command (`X` keystroke) added
  + Topology overlay selection with `F1`, `F2`, `F3`, ... keystrokes added
+ Applications View:
  + Confirmation dialog added for application activate / deactivate /
    uninstall
  + Application model enhancements supported:
    + columns added for additional attributes
    + details panel displayed when application row selected
  + Note: applications can now define custom icon and URL (link to docs)
+ Dialog Service:
  + `Enter` and `Escape` keys bound to _OK_ and _Cancel_ buttons

----
#### Emu -- 1.4.0 -- November 2015
+ Device View:
  + Friendly name can be set on a device from the device detail panel
+ Intent View:
  + Button added to navigate to _Topology View_ and display the
    selected intent
+ Topology View:
  + _Traffic Overlay_ now selected by default
  + Topology overlays can now highlight devices and hosts with badges
    (small number / text / glyph)
  + Topology overlays can invoke a dialog box to interact with the user
+ ONOS-Branded "Loading..." animation added
+ Sample application (org.onosproject.uiref) featuring UI content
  injection techniques added
+ GUI Archetypes (ui, uitab, uitopo) facilitating rapid development of 
  applications with custom UI content
  
----
#### Drake -- 1.3.0 -- August 2015
+ Authentication enabled by default (login screen; logout action)
+ _Settings_ and _Tunnels_ views added
+ Topology View:
  + Traffic re-implemented as an "overlay"
  + Overlay mechanism now programmable from ONOS apps
    + highlighting / labeling topology links
    + full control over summary panel content
    + full control over details panel content
    
----
#### Cardinal -- 1.2.0 -- May 2015
+ Websocket mechanism promoted to be framework-wide; a shared resource 
  amongst the views
+ More tabular views added:
  + Links
  + Hosts
  + Intents
  + Applications
  + Cluster Nodes
  + Device Flows (hidden view)
  + Device Ports (hidden view)
  + Device Groups (hidden view) 
+ Changes to the Topology View:
  + links are now selectable.
  + toolbar added (note, keystroke commands still available)
    + node layer buttons moved from masthead to toolbar
  + sprite layer added
  + user selection choices persisted across sessions
  + summary and detail panels adjust size to window height
+ Changes to the Device View:
  + slide out details panel appears when a device is clicked on
+ Navigation Menu changes:
  + glyphs next to links for navigation
  + views organized into categories
+ Note that the legacy (Avocet) GUI has been deprecated, and that 
  the (Angular-based) GUI loads by default.

----
#### Blackbird -- 1.1.0 -- February 2015
+ GUI Framework migrated to use AngularJS
  + View-agnostic features refactored as Angular Services
+ Topology View refactored to be an Angular module
  + Topology source code broken out into multiple source files
  + Port Highlighting on links added
+ Device View added
  + Implemented as a simple table for now; one device per row, 
    sortable by column header clicks
+ Sample View added
  + Skeletal example code
+ Light and Dark themes fully implemented
  + Press the 'T' key to toggle theme
+ Beginnings of UIExtension mechanism implemented
  + Over future releases, this will facilitate the ability of 
    Apps to inject their own content into the GUI

> Note that the new (Angular-based) GUI currently co-exists with the 
>   old (Avocet) GUI.
> + By default, the Avocet GUI is launched; 
>   the base URL `http://localhost:8181/onos/ui` is mapped to 
>   `http://localhost:8181/onos/ui/legacy/index.html#topo`
> + The new Angular-based GUI can be launched by manually adjusting 
>   the URL to be:  `http://localhost:8181/onos/ui/index.html#topo`, 
>   (that is, remove "legacy/"). 

----
#### Avocet -- 1.0.0 -- November 2014
+ GUI implemented using a home-grown framework 
+ Single view (_Topology View_) implemented, displaying network topology and
  providing a certain level of interaction to show traffic & flow information
+ Although the `T` key-binding (toggle theme) is present, the "dark" theme 
  has not been implemented
  
----
