# Migrating ONOS GUI to GUI2

# Introduction
## Why is a migration needed?
Since the introduction of ES6 version of the EcmaScript language many new language
features have been introduced that allow a lot of new capabilities that were very
hard to acheieve with the older version of the ECMAScript language.

Google have taken advantage of this and totally renewed the Angular framework to
take full advantage of these improvements. The new Angular is so different they
have renamed old angular to Angular JS and the new one is known as Angular 7.

Along with this Microsoft have introduced TypeScript which is a strongly typed
superset of ES6. Its big advantage is that type safety can be ensured at compile
time and the code readability and testability has been greatly improved.

In addition to all of this Google has introduced a new framework tool __Angular
CLI__ that bundles together several JS frameworks together that make creation,
building and testing much more consistent and automated.

The reason for this migration of the ONOS GUI code then is to take advantage of
all of these new technologies. As it was in early 2018 based on Angular JS (1.3.5)
the development of the GUI code had come to a halt, and it was at risk of becoming
obsolete and left behind by developers because of its complexity. Anyone wanting
to develop or extend it had a huge learning curve.

The purpose of this migration therefore is to make the code more up to date and
accessible to developers, and to ensure that it provides an open framework that
can be built upon by future developers. The simple goal is that anyone experienced
in the most recent Angular framework should find it very easy to navigate around
the ONOS GUI2 code base.

# Technical challenges
Code written for Angular 1.x can be converted to Angular 7, through a line by line migration process (aka a hard slog)

* It is important to know that Angular 7 code is written in TypeScript and all files end in .ts
* In tsconfig.json this app is set to be compiled as ES6 (ES2015)
  * See this [Compatibility Table](http://kangax.github.io/compat-table/es6/) for supported browsers
  * All modern browsers are supported
  * See https://webapplog.com/es6/ for a list of things that ES6 brings
* Each item (Angular 'schematic' e.g. Service, Component, Directive, Pipe or Module)
gets its own file ending with this type e.g. function.service.ts
* Each test file is the name of the item with .spec.ts e.g. function.service.spec.ts
    * It is considered best practice to put the Unit test of a component or service
   right next to it in the folder.
* Modules are used to group together services, components, directives etc
* When starting any new component use the `ng generate ..` This will create the associated test too
* This migration takes advantage of libraries. The 'fw' section has now been
separated out in to its own library 'gui2-fw-lib'

# Migration approach
Migration the existing ONOS Gui can be done through a process of taking an existing JavaScript file and looking at its 
angular.module statement.
Note:
* The main ONOS GUI page is based on OnosComponent and this is the bootstrap component 
* This is included in index.html as <onos-root>
* Other components like Mast and Nav menu are included inside this using the selectors <onos-mast> and <onos-nav>
* There are 40+ services spread across 8 modules just in the Framework alone
* There is another module per view (e.g. Devices, Topology) and another one per external view (e.g. YangUi)

So taking the onos.js file:
```
    angular.module('onosApp', moduleDependencies)
        .controller('OnosCtrl', [
            '$log', '$scope', '$route', '$routeParams', '$location',
            'LionService',
            'KeyService', 'ThemeService', 'GlyphService', 'VeilService',
            'PanelService', 'FlashService', 'QuickHelpService', 'EeService',
            'WebSocketService', 'SpriteService',
            
            function (_$log_, $scope, $route, $routeParams, $location,
                      lion, ks, ts, gs, vs, ps, flash, qhs, ee, wss, ss) {
            ....
        .directive('detectBrowser', ['$log', 'FnService',
            function ($log, fs) {
                return function (scope) {
```

There is clearly a module (onosApp) here containing a controller (OnosCtrl) and a directive (detectBrowser)
* The 'onosApp' module becomes __onos/web/gui2/src/main/webapp/app/onos.module.ts__
  * It lists the component and the directive as declarations
  * It imports the services and components used within this module by importing __their__ modules
* The 'OnosCtrl' controller becomes the component __onos/web/gui2/src/main/webapp/app/onos.component.ts__
  * The function in the controller becomes the constructor of the component
  * The parameters to the function are injected services to the constructor 
  * It includes a html file and a CSS file through the templateUrl and styleURLs
* The 'detectBrowser' directive becomes __onos/web/gui2-fw-lib/src/main/webapp/app/detectbrowser.directive.ts__
  * It can be referenced in the onos.component.html template by its selector `onosDetectBrowser`

If documentation has been created (using `npm run compodoc`) this module can be inspected at
[OnosModule](./documentation/modules/OnosModule.html)

# Angular CLI
The Angular CLI tool has many handy modes to help you along the way.
From the onos/web/gui folder you should be able to run 

`ng lint`

which will scan all the .ts files in the src folder and check for errors.

The ONOS GUI can be run using the 

`ng serve`

command and can be left running as code changes are made. Please see README.md for notes
on how to run this.
Once this is running a browser attached to [http://localhost:4200](http://localhost:4200) 
will display the application and any changes made to the code will 
be visible immediately as the page will refresh

Watch out for any errors thrown in 'ng serve' - they usually point to something fairly 
bad in your code. 

Another place to look is in the browsers own console
`Ctrl-Shift-I` or `F12` usually brings this up.

# Migrating the code
This is where things get really interesting. A good place to start is on a 
Service which does not have dependencies on other services. 

Two services have been setup in the onos.module.ts that are new to this migration
* LogService - this replaces $log that was inserted in to the old code
* FunctionService - this replaces fn.js in the old code

There is a fair bit of refactoring has to take place. An important thing to understand
is that DOM manipulations from inside JavaScript code is not the Angular 7
way of doing things - there was a lot of this in the old ONOS GUI, using d3.append(..)
and so on.
The Angular 7 way of doing things is to define DOM objects (elements) in the
html template of a component, and use the Component Java Script code as a base
for logic that can influence the display of these objects in the template.
What this means is that what were previously defined as services (e.g. VeilService or
LoadingService) should now become Components in Angular 7 (e.g. VeilComponent or
LoadingComponent).

Similarly a directive might be trying to do DOM manipulation and have a CSS - this 
should be made in to a component instead (see IconComponent)

### How do I know whether a Service or Directive should be made a Component in this new GUI?
The general rule to follow is _"if a service in the old GUI has an associated CSS 
file or two then it should be a component in the new GUI2"_.

The implication of this is that all of the d3 DOM Manipulations that happened in 
the old service should now be represented in the template of this new component.
If it's not clear to you what the template should look like, then run the old GUI
and inspect the element and its children to see the structure.

### Scope of components and services
Components (unlike services) have limited scope (that's the magic of them really -
no more DOM is loaded at any time than is necessary). This means that they are
self contained modules, and any CSS associated with them is private to that 
component and not accessible globally.

Services on the other hand have 2 different options for scope - they can be in the
'root' scope or in the scope of a component that injects them. Most services have
been given root scope - by using __@Injectable ({providedIn: 'root',})__. There
are a few exceptions like TopologyService that is only relevant to TopologyComponent
and so is loaded only in this scope.

### Do not inject components in to services
Components are graphical elements and should not be injected in to Services.
Services should be injected in to components, but not the other way round.
Components can be added in to other components by putting the selector of
the child component e.g. <onos-icon> in to the html template of the parent.

If some function on this child component needs to be referred to in the parent
component code, the child can be given a name tag and then referred to by this
with a @ViewChild declaration in the parent TS file. See TopologyComponent for
an example of this, where a reference to the SummaryComponent is given the tag
__#summary__.

In terms of injecting services, take for instance the WebSocketService - this
should remain a service, but I want to display the LoadingComponent while it's
waiting and the VeilComponent if it disconnects.
I should not go injecting these in to WebSocketService - instead there is a
setLoadingDelegate() and a setVeilDelegate() function on WSS that I can pass in
a reference to these two components. When they need to be displayed a method call
is made on the delegate and the component gets enabled and displays.
Also note inside WSS any time we call a method on this LoadingComponent delegate
we check that it the delegate had actually been set. 

The WSS was passed in to the LoadingComponent and VeilComponent to set the
delegate on it.

Any component that needs to use WSS for data should inject the WSS service __AND__
needs to include the components in its template by adding <onos-loading> and
<onos-veil>.

### Consider if a service is really needs to be a service that runs all the time
Or does it just support a few functions. See the TableBase class. This now
replaces the old TableBuilderService - that was just on function that 
manipulated the scope of a view component. Instead view components now
extend this class.

Also sometimes directive are always used together e.g. icon directive and tooltip
directive and they can be merged in to one

## fw/remote/wsock.service
Taking for a really simple example the fw/remote/WSockService, this was originally defined in
the __/onos/web/gui/src/main/webapp/app/fw/remote/wsock.js__ file and is now redefined in
__onos/web/gui2-fw-lib/projects/gui2-fw-lib/src/lib/remote/wsock.service.ts__.

First of all this should remain a Service, since it does not do any DOM
manipulation and does not have an associated CSS.

This is a wrapper around the ES6 class WebSocket. It has one method newWebSocket()
that's called to establish the WebSocketService

```javascript
 1 (function () {
 2    'use strict';
 3
 4    angular.module('onosRemote')
 5        .factory('WSock', ['$log', function ($log) {
 6
 7            function newWebSocket(url) {
 8                var ws = null;
 9                try {
10                    ws = new WebSocket(url);
11                } catch (e) {
12                    $log.error('Unable to create web socket:', e);
13                }
14                return ws;
15            }
16
17            return {
18                newWebSocket: newWebSocket,
19            };
20        }]);
21 }());

```

Converting this to TypeScript requires a total change in mindset away
from functions and towards more object oriented programming. This file is
located in __onos/web/gui2-fw-lib/projects/gui2-fw-lib/src/lib/remote/wsock.service.ts__

```typescript
101 import { Injectable } from '@angular/core';
102 import { LogService } from '../../log.service';
103 
104 @Injectable({ providedIn: 'root' })
105 export class WSock {
106 
107  constructor(
108      private log: LogService,
109  ) {
110      this.log.debug('WSockService constructed');
111  }
112
113  newWebSocket(url: string): WebSocket {
114      let ws = null;
115      try {
116          ws = new WebSocket(url);
117      } catch (e) {
118          this.log.error('Unable to create web socket:', e);
119      }
120      return ws;
121  }
122 }
```

There are several things worth noting here:
* The module is no longer given in the file - the onosRemote module is now part of
    the corresponding Gui2FwLibModule in ../gui2-fw-lib.module.ts. It could be
    listed in this module file as one of its providers, but it doesn't have to
    since it's already injected as root (line 104)
* factory is replaced by the @Injectable annotation
* This WSock is expressed as a class rather than a function (line 105)
* Note on line 105 - the more usual convention used elsewhere is to call name the class
    WSockService, but for the sake of compatibility with the existing file at line 5
    we keep the same name.  
* WSock now has a constructor function (line 107) - the parameters to this are 
   other Injectables e.g. the LogService
* The class of the LogService has to be imported (line 102) 
    before it can be used
* Anything belonging to the class has to be prefixed with 'this.'
* The calling of the 'debug' service (line 110) on the log service is an example
* The function newWebSocket (line 7) is now replaced by the method newWebSocket()
    - this is automatically public, but could be made protected or private
* Because it's TypeScript, we can assign types to the method signature.
    e.g. url is a string, and the function returns a WebSocket object. This helps
    a lot with code readability and static checking
* The __let__ keyword (e.g. line 114) is used in TypeScript instead of __var__ (line 8)
 
##Cheatsheet

* angular.extend() can be replaced by Object.assign()
* $timeout can be replaced by setTimeout()
* $timeout.cancel can be replaced by clearTimeout()
* (d3 object).append(..).attr values should be listed individually (see icon.service for example)
* Please try do avoid d3 DOM manipulations in ONOS GUI 2, as this is not the
    Angular way of doing things
* $interval should be replaced by 
    task = setInterval(() => functionname_or_body, speed);
* To cancel the timer clearInterval(task)
* If a function is to be called then the format () => {} should be used. This is
  so that the enclosing context can be passed in to the lambda


# Progress so far - Dec 2018
The following services are most migrated:
* fw/util/FnService - full migrated with Unit tests
* fw/svg/GlyphDataService - mostly migrated. Values are stored in maps as constants
* fw/svg/GlyphService - partly implemented - enough to support the icon service
* fw/svg/IconService - mostly implemented - enough to support the IconDirective
* fw/svg/icon/IconComponent - replacement for the IconDirective - decided to make 
   it a component because it has CSS files which are scoped to just that component
   It also incorporates the old fw/widget/tooltip.js which was a directive - combined
   tooltip in to icon because the 2 are always used together in tabular views
* fw/layer/LoadingService - mostly implemented - this should become a component 
    - its CSS is has to be loaded globally in index.html
* fw/layer/flash/FlashComponent - implemented as a Component instead of the old Flash Service
  because it has a CSS file. Replaced all of the D3 Dom manipulations with Template code
  in the Angular 6 style of doing things
* fw/layer/veil/VeilComponent - changed to a component - fully implemented
* fw/remote/urlfn.service - fully implemented with Unit tests
* fw/remote/WebSocketService - fully implemented with Unit tests
* fw/widget/TableBase - previously the TableBuilderService this has now been changed
to a plain interface and class - any table views should extend this
* fw/widget/PanelBase - previously the PanelService - this is an abstract base class
  that both dialogs and details panels are based off
* fw/widget/DetailsPanelBase - previously the DetailsPanelService - this has functions 
  for accessing the WebSocket service for details. If extends PanelBase and is the 
  base for AppsDetailsComponent and DeviceDetailsComponent


## Devices View
This is now a Component, whose class extends the TableBase - this is where it gets
most of its functionality. As before all data comes from the WebSocket.
There is still a bit of work to go on this - scrolling of long lists, device details 
panel etc

The major change in the template (html) is that there used to be 2 tables and these
are now brought together in to a header and body. This simplifies trying to keep 
the widths of both in sync.

For CSS the old device view CSS is included and a link is made across to the
common table CSS

The Details Panel is made visible when a row is selected - it is a component, and is
embedded in to the repeated row. There are base classes for common details panel 
behaviour 

## Apps View
This is a Component too, again extending TableBase. Apps view has much more functionality 
though because it has controls for upload and download of applications.

## All other tabular views
About 20 tabular views have now been migrated

## Faultmanagement Alarms view
Because it is important to be able to integrate external applications in to the
ONOS GUI2, an approach was needed to make them work in the new world of Angular CLI.

To make this happen this GUI view was made in to a library (fm-gui2-lib), in
* onos/apps/faultmanagement/fm-gui2-lib/projects/fm-gui2-lib/src/lib

that
contains 2 components:
* AlarmTableComponent
* AlarmDetailsComponent

These depend on some of the functions defined in the gui2-fw-lib - but that's no
problem  this is imported just like any other NPM library.

The fm-gui2-lib then can just be listed as one of the dependencies of the main
application in onos/web/gui2 and can be navigated to through the Nav menu as 'alarmTable'
See onos/web/gui2/src/main/webapp/app/onos-routing.module.ts line 87.

When the underlying application is stopped in Karaf (ONOS CLI), the option to
navigate to this Alarm GUI disappears from the Nav menu.

## Yang GUI
No migration has been done on this yet. Because it's and external application
the approach will be similar to FM GUI, where it's created as a library.

## Topology View
This is one of the main goals of the migration - bringing together the Topology(1)
and the Topology2 views of the old GUI together in to one new Topology view.

Topology2 project was never really finished and several features from Topology1
are missing from it. Topology2 introduced the Regions and hierarchial navigation.

In the new GUI2 implementation, care has been taken to separate the structure out
in to components that will hopefully be reusable elsewhere. Also the components
have been logically grouped - all of the __panel__ components are together, as
are the __layout__ components. In addition some are SVG components that are
designed to extend an SVG element tree, rather than a HTML one.

Also the icons have got a fresh new look with gradients and drop shadows done in SVG.

All of this will ultimately lead to a framework that can support other paradigms
especially ones like tiles background maps, such as this from Google and other
providers.

This has progressed a lot since the first version in Nov 18. Now traffic flows are 
represented as well as node and host labels and link hovering etc. Zoom and Pan
have been added as well as many of the keyboard shortcuts, familiar from Topo1

The Topology view will eventually be broken out in to its own library, to promote
reuse.