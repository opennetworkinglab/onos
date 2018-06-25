# Migrating ONOS GUI
Code written for Angular 1.x can be converted to Angular 5, through a line by line migration process (aka a hard slog)

* It is important to know that Angular 5 code is written in TypeScript and all files end in .ts
* In tsconfig.json this app is set to be compiled as ES6 (ES2015)
  * See this [Compatibility Table](http://kangax.github.io/compat-table/es6/) for supported browsers
  * All modern browsers are supported
  * See https://webapplog.com/es6/ for a list of things that ES6 brings
* Each item (Service, Component, Directive, Pipe or Module) gets its own file ending with this type e.g. function.service.ts 
* Each test file is the name of the item with .spec.ts e.g. function.service.spec.ts
    * It is considered best practice to put the Unit test of a component or service
   right next to it in the folder - some new unit tests have been placed in a
   tests folder, but they will have to come back out from there in to the 
   individual folders.
* Modules are used to group together services, components, directives etc
* When starting any new component use the `ng generate ..` This will create the associated test too


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
* The 'detectBrowser' directive becomes __onos/web/gui2/src/main/webapp/app/detectbrowser.directive.ts__
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

command and can be left running as code changes are made. 
Once this is running a browser attached to [http://localhost:4200](http://localhost:4200) 
will display the application and any changes made to the code will 
be visible immediately as the page will refresh

Watch out for any errors thrown in 'ng serve' - they usually point to something fairly 
bad in your code. 

Another place to look is in the browsers own console
`Ctrl-Shift-I` usually brings this up.

# Migrating the code
This is where things get really interesting. A good place to start is on a 
Service which does not have dependencies on other services. 

Two services have been setup in the onos.module.ts that are new to this migration
* LogService - this replaces $log that was inserted in to the old code
* WindowService - this replaces $window and $location in the old code

There is a fair bit of refactoring has to take place. An important thing to understand
is that DOM manipulations from inside JavaScript code is not the Angular 6
way of doing things - there was a lot of this in the old ONOS GUI, using d3.append(..)
and so on.
The Angular 6 way of doing things is to define DOM objects (elements) in the 
html template of a component, and use the Component Java Script code as a base
for logic that can influence the display of these objects in the template.
What this means is that what were previously defined as services (e.g. VeilService or
LoadingService) should now become Components in Angular 6 (e.g. VeilComponent or
LoadingComponent). 

Similarly a directive might be trying to do DOM manipulation and have a CSS - this 
should be made in to a component instead (see IconComponent)

### How do I know whether a Service or Directive should be made a Component in this new GUI?
The general rule to follow is _"if a service in the old GUI has an associated CSS 
file or two then is should be a component in the new GUI"_. 

The implication of this is that all of the d3 DOM Manipulations that happened in 
the old service should now be represented in the template of this new component.
If it's not clear to you what the template should look like, then run the old GUI
and inspect the element and its children to see the structure.

Components (unlike services) have limited scope (that's the magic of them really -
no more DOM is loaded at any time than is necessary). This means that they are
self contained modules, and any CSS associated with them is private to that 
component and not accessible globally.

### Do not inject components in to services
Components are graphical elements and should not be injected in to Services. 
Services should be injected in to components, but not the other way round.
Components can be added in to other components by putting the selector of 
the child component e.g. <onos-icon> in to the html template of the parent.

Take for instance the WebSocketService - this should remain a service, but I want 
to display the LoadingComponent while it's waiting and the VeilComponent if it
disconnects. I should not go injecting these in to WebSocketService - instead
there is a setLoadingDelegate() and a setVeilDelegate() function on WSS that I 
can pass in a reference to these two components. When they need to be displayed
a method call is made on the delegate and the component gets enabled and displays.
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
the __app/fw/remote/wsock.js__ file and is now redefined in 
__onos/web/gui2/src/main/webapp/app/fw/remote/wsock.service.ts__.

First of all this should remain a Service, since it does not do any DOM
manipulation and does not have an associated CSS.

This has one method that's called to establish the WebSocketService

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
located in __onos/web/gui2/src/main/webapp/app/fw/remote/wsock.service.ts__

```typescript
101 import { Injectable } from '@angular/core';
102 import { LogService } from '../../log.service';
103 
104 @Injectable()
105 export class WSock {
106 
107  constructor(
108    private log: LogService,
109  ) {
110    this.log.debug('WSockService constructed');
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
* The module is no longer given in the file - 
    the corresponding RemoteModule in ./remote.module.ts lists this 
    service as one of its providers
* factory is replaced by the @Injectable annotation
* This WSock is expressed as a class rather than a function.
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
* Because if TypeScript we can assign types to the method signature.
    e.g. url is a string, and the function returns a WebSocket. This helps
    a lot with code readability and static checking
* The __let__ keyword (e.g. line 114) is used in TypeScript instead of __var__ (line 8)
 
##Cheatsheet

* angular.extend() can be replaced by Object.assign()
* $timeout can be replaced by setTimeout()
* $timeout.cancel can be replaced by clearTimeout()
* (d3 object).append(..).attr values should be listed individually (see icon.service for example)
* Please try do avoid d3 DOM manipulations in ONOS GUI 2, as this is not the Angular 6 way of 
  doing things
* $interval should be replaced by 
    task = setInterval(() => functionname_or_body, speed);
* To cancel the timer clearInterval(task)


# Progress so far - 18 Jun 2018
The following services are partially migrated:
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


# Devices View
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

# Apps View
This is a Component too, again extending TableBase. Apps view has much more functionality 
though because it has controls for upload and download of applications.


  
