# Gui2TopoLib

This project was generated with [Angular CLI](https://github.com/angular/angular-cli) version 7.0.4.

It is the Topology View of the ONOS GUI. It has been extracted out in to its own
library here to allow it to be used by other projects.

This deliberately has no dependency on the ~/onos/web/gui2 project
It does depend on the ~/onos/web/gui2-fw-lib - the Framework is common to all apps

The view is made up of a hierarchy of Angular components - starting at the top 
with Topology Component

Extensive use is made of SVG to generate the view - especially the layers, nodes
and links. Each of these are components that are meant to be used inside an SVG
element and so have the suffix SvgComponent. Other like panels are plain Html

```
TopologyComponent
|   (layers)
|-- NoDeviceConnectedSvgComponent
|-- BackgroundSvgComponent
|  |-- MapSvgComponent
|-- GridsvgComponent
|-- ForceSvgComponent
|  |-- DeviceNodeSvgComponent
|  |-- HostNodeSvgComponent
|  |-- LinkSvgComponent
|  |-- SubRegionNodeSvgComponent
|
|   (panels)
|-- DetailsComponent
|-- InstanceComponent
|-- SummaryComponent
|-- ToolbarComponent
|-- MapSelectorComponent
```

The purpose of the tester application is to
a) show how the Topology View can be easily reused
b) Allow a developer to run the GUI with Angular CLI tools (ng serve) without
   all of the baggage of the whole ONOS GUI
        
## Development server

Run `ng serve` for a dev server from ~/onos/web/gui2-topo-lib. Navigate to `http://localhost:4200/`.
When a change is made to the library code, it has to be built again
`ng build gui2-topo-lib && ng serve`

This requires the following manual steps
* Make directory `fw/widget` in `~/onos/web/gui2-topo-lib/projects/gui2-topo-lib`
* Copy  ~/onos/web/gui2-fw-lib/lib/widget/panel*.css in to this
* Copy directory `~/onos/web/gui/src/main/webapp/data` in
    to `~/onos/web/gui2-topo-lib/projects/gui2-topo-tester/src/data`
* Change (temporarily) web/gui/src/main/java/org/onosproject/ui/impl/UiWebSocket.java
to remove the check for sessionToken (lines 249 -251)
* Run ONOS `ok -- clean debug` and enable the GUI2 application

## Code scaffolding
From inside ~/onos/web/gui2-topo-lib/projects/gui2-topo-lib
run `ng generate component component-name` to generate a new component.
 
You can also use `ng generate directive|pipe|service|class|guard|interface|enum|module`.

## Build
To build just the library run `ng build gui2-topo-lib` This will always be built
in Prod mode

Run `ng build` to build the project. The build artifacts will be stored in 
the `dist/` directory. Use the `--prod` flag for a production build.

## Running unit tests

Run `bazel test //web/gui2-topo-lib:test-not-coverage` to execute the unit tests via [Karma](https://karma-runner.github.io).
