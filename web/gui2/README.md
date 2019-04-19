# ONOS GUI 2.1.0

This project is based on __[Angular 7](https://angular.io/docs)__ 
and __[ES6](http://www.ecma-international.org/ecma-262/6.0/index.html)__ (aka __ES2015__), 
as an alternative to the 1.0.0 GUI which was based 
off __[AngularJS 1.3.5](https://angularjs.org/)__

Building, testing and running lint are all handled by Bazel. See web/gui2/BUILD file.

To use this new GUI you simply have to start the GUI in a running ONOS at the __onos>__ cli:
```
app activate gui2
```
and the gui will be accessible at [http://localhost:8181/onos/ui](http://localhost:8181/onos/ui)

Gui2 can also be loaded every time ONOS starts by adding it to ONOS_APPS
```
export ONOS_APPS=${ONOS_APPS:-drivers,openflow,gui2}
```

The original legacy GUI is also loadable as an app, and is available at the same web resource onos/ui
The legacy GUI should be disabled if GUI2 is active and vice versa
```
app deactivate gui
```

As usual with ONOS if you want to run it in a different language set the __ONOS_LOCALE__ environment variable
to the locale you want before starting onos. e.g.
```
ONOS_LOCALE=fr_FR SHLVL=1 bazel run onos-local -- clean debug
```

# Architecture
The whole application comprises of a number of modules on the client side
* __gui2-fw-lib__ This is a collection of client side component, services and base
classes that are reused among all modules [more details..](../gui2-fw-lib/README.md)
* __gui2-topo-lib__ This is the Topology view only [more details..](../gui2-topo-lib/README.md)
* __fm_gui_lib__ This is the Fault Management table [more details..](../../apps/faultmanagement/fm-gui2-lib/README.md)
* __gui2__ This is the front end entry point application and brings together all
other libraries. It also holds all of the tabular applications such as Applications
view and Devices view, and manages the routing between the different views. 

All of these modules talk to the backend server mainly through a WebSocket connection
passing JSON snippets asynchronously in both directions.
In some places the client side code loads data through a plain HTTP GET call, and
these are served by servlets on the backend. Similarly raster graphics are loaded
through a servlet.

Much of the graphics in the application are made from SVG paths and primitive objects,
and are displayed smoothly by HTML5 compatible browsers. Occasionally the d3
libraries (such as d3-force) are used to help position graphics, but an over
dependence on d3 is mainly avoided.

The main framework used is Angular 7, with a strong emphasis on making resuable
components, directives, services and classes. Angular Routing and animation are
also used.

# Issues
Issues found on the GUI2 should be added to the existing list on the
[ONOS GUI Jira Kanban board](https://jira.onosproject.org/secure/RapidBoard.jspa?rapidView=31)
(requires Jira login)

# Development
There are 2 ways to go about development - 
1. rebuild the code and rerun through Bazel (much like can be done with any ordinary ONOS app)
(recommended for most people) 
 OR
2. use Angular 7 CLI (__ng__ command) to rebuild on the fly (faster for
development). Be aware that if you are just changing the topology view it does not
require all of the steps that __gui2__ does - see its own [README](../gui2-topo-lib/README.md)
for more details

For 1) if you change the code you can redeploy the application without restarting
ONOS with (requires you to be in ~/onos directory):
```
bazel build //web/gui2:onos-web-gui2-oar && onos-app localhost reinstall! bazel-bin/web/gui2/onos-web-gui2-oar.oar
```

For 2) it's well worth becoming familiar with Angular CLI.
> (this has the advantage of debug symbols and the code not is uglified and minimized)

The project is created with [Angular CLI](https://github.com/angular/angular-cli) v7
to simplify development of the browser side code. It is complicated to set up, 
but is worth the effort if you have more than a few days worth of development to do.
Since data is retrieved through WebSockets there is a requirement to run ONOS
in the background.

>There is no need to install node, npm or ng again on your system, and indeed if
>they are already installed, it's best to use the versions of these that's used 
>by Bazel in ONOS.

Add the following 2 entries to the __start__ of 
your PATH environment variable. 
```
~/.cache/bazel/_bazel_scondon/8beba376f58d295cf3282443fe02c21a/external/nodejs/bin/nodejs/bin
```
> (where ~/.cache/bazel/_bazel_scondon/8beba376f58d295cf3282443fe02c21a should be
> replaced by an equivalent folder on your system)
```text
~/onos/web/gui2-fw-lib/node_modules/@angular/cli/bin/
```

The first time you run this you will have to go to the framework folder and run "npm install"
```text
cd ~/onos/web/gui2-fw-lib && \
npm install
```

This will install all the vendor Javascript implementations that are listed in package.json
(including 'ng' - the Angular CLI command) in to ~/onos/web/gui2-fw-lib/node_modules

After this you should be able to cd in to ~/onos/web/gui2-fw-lib and run 'ng version' and see:
```
Angular CLI: 7.0.4
Node: 8.11.1
OS: linux x64
Angular: 7.0.2
```

## GUI FW Lib
The GUI2 __framework__ is in __~/onos/web/gui2-fw-lib__ and the GUI __application__ is in __~/onos/web/gui2__ (and depends on the framework).

The GUI2 framework is a library inside its own mini application (unrelated to the
main GUI application) - every thing of importance is in projects/gui2-fw-lib. The
own application is just a wrapper around the framework library - it has to be
there for Angular CLI to work and can be useful for testing parts of the framework
in isolation. 

For build method 2) above if you make any changes here or are using it for the
first time it will need to be built. From ~/onos/web/gui2 run:
```bash
pushd ~/onos/web/gui2-fw-lib && \
ng build gui2-fw-lib && \
cd dist/gui2-fw-lib && \
npm pack && \
popd && \
npm install gui2-fw-lib
```

This packages the Framework up in to __~/onos/web/gui2-fw-lib/dist/gui2-fw-lib/gui2-fw-lib-2.1.0.tgz__

## GUI2 Topo library
The GUI2 __Topology__ is in __~/onos/web/gui2-topo-lib__ and the GUI __application__
includes this Topology application through the __onos-routing-module__. The 
Topology app has its own README file.

For build method 2) above if you make any changes here or are using it for the
first time it will need to be built. From ~/onos/web/gui2 run:
```text
pushd ~/onos/web/gui2-topo-lib && \
ng build gui2-topo-lib && \
cd dist/gui2-topo-lib && \
npm pack && \
popd && \
npm install gui2-topo-lib
```
This packages the Topo View up in to __onos/web/gui2-topo-lib/dist/gui2-topo-lib/gui2-topo-lib-2.1.0.tgz__

## GUI2 Application
The application contains the ONOS index.html and all of the tabular views and the
topology view. It references the gui2-fw-lib and gui2-topo-lib as just other
dependencies. 

For build method 2) above to use this application in Angular CLI for development
on your system, you need to: 
1. Change directory in to onos/web/gui2 - this is where you will run the `ng` command from.
2. Run `npm install` once from this folder to add dependencies
3. Then run 'ng version' from onos/web/gui2 and an additional version should be
shown __Angular: 7.0.2__
4. Temporarily make a change to disable authentication in UIWebSocket.java
5. Temporarily make a change to disable external routes in onos-routing.module.ts
6. Create symbolic links for some CSS files

### Disable authentication and external routes
> Remember this is only for build method 2) above

Before the server can be run a couple of small adjustments need to be temporarily made
1. The file __~/onos/web/gui/src/main/java/org/onosproject/ui/impl/UiWebSocket.java__ 
needs to be adjusted to remove authentication
2. The file __~/onos/web/gui2/src/main/webapp/app/onos-routing.module.ts__ needs 
to be adjusted to remove references to routes in external applications 

These changes are given in Appendix A at the end of this document - these changes
should not be checked in though - as they are not required (and will break) the
GUI2 embedded in ONOS.

### Create symbolic links for CSS files
Also some files need to be symbolically linked - these should no be checked in
```text
cd ~/onos/web/gui2/src/main/webapp/app && \
mkdir -p fw/widget && mkdir -p fw/layer && \
cd fw/layer && ln -s ~/onos/web/gui2-fw-lib/projects/gui2-fw-lib/src/lib/layer/loading.service.css && \
cd ../widget && \
ln -s ~/onos/web/gui2-fw-lib/projects/gui2-fw-lib/src/lib/widget/panel.css && \
ln -s ~/onos/web/gui2-fw-lib/projects/gui2-fw-lib/src/lib/widget/panel-theme.css && \
ln -s ~/onos/web/gui2-fw-lib/projects/gui2-fw-lib/src/lib/widget/table.css && \
ln -s ~/onos/web/gui2-fw-lib/projects/gui2-fw-lib/src/lib/widget/table.theme.css
```

After this it will be possible to build/test/lint/run the application inside the Angular CLI without errors.
```text
ng build --prod && \
ng lint && \
ng test --watch=false
```

## Development server
Finally the application can be run, and will be available at http://localhost:4200
```text
ng serve --aot
``` 

Run `ng serve --aot` for a dev server (because we are using ES6, we [must use AOT](https://github.com/angular/angular-cli/wiki/build)). 
Navigate to `http://localhost:4200/`. The app will automatically reload if you change any of the source files.

Press Ctrl-Shift-I in Chrome and Firefox to bring up the developer tools and the browser console.

There are certain extra debugging can be turned on by adding the parameter 'debug' 
For example to turn extra logging for WebSockets add on __?debug=txrx__

On the Apps view - icons will appear to be missing - this is because they use a relative path to
source the image, and this path is not available in this 'ng serve' mode. The icons work fine in the
mode where it's run inside ONOS. 

### Navigating
In this development mode navigation is not available, and to to jump to other view, 
replace the 'device' at the end of the URL with the route you want to follow
e.g. 'app' for the Applications view or 'topo' for the Topology view

## Code scaffolding
Change directory in to '~onos/web/gui2/src/main/webapp/app/view'
Run `ng generate component component-name` to generate a new component. You can also use `ng generate directive|pipe|service|class|guard|interface|enum|module`.

## Build
The build is handled through the web/gui2/BUILD file. This downloads Node, NPM and Angular CLI
It runs ```ng build --prod --extract-css``` and copies everything over in to WEB-INF/classes/dist

To run it manually in Angular CLI run `ng build` (and add on --prod --extract-css --watch as necessary to alter its behaviour)

## Running unit tests
This is automatically done when using "bazel test " - see the web/gui2/BUILD file for more details.

To run it manually in Angular CLI run `ng test --watch` to execute the unit tests
via [Karma](https://karma-runner.github.io). Running it directly like this will
test with both Firefox and Chrome. To use only one use the __--browsers__ argument

## Running checkstyle (lint)
This is automatically done when using "bazel test" - see the web/gui2/BUILD file
for more details.

To run it manually in Angular CLI run `ng lint` to run codelyzer on your code,
according to the rules in __tslint.json__

## Running end-to-end tests
To run it manually in Angular CLI run `ng e2e` to execute the end-to-end tests
via [Protractor](http://www.protractortest.org/).

## Generating documentation
To run it manually in Angular CLI run `npm run compodoc` to generate documentation
via [Compodoc](https://github.com/compodoc/compodoc)

## Further help
To get more help on the Angular CLI use `ng help` or go check out the
[Angular CLI README](https://github.com/angular/angular-cli/blob/master/README.md).


# Appendix A - changes needed to run GUI2 application locally
```text
diff --git a/web/gui/src/main/java/org/onosproject/ui/impl/UiWebSocket.java b/web/gui/src/main/java/org/onosproject/ui/impl/UiWebSocket.java
index e53a00756b..63e538a4db 100644
--- a/web/gui/src/main/java/org/onosproject/ui/impl/UiWebSocket.java
+++ b/web/gui/src/main/java/org/onosproject/ui/impl/UiWebSocket.java
@@ -251,9 +251,7 @@ public class UiWebSocket
             ObjectNode message = (ObjectNode) mapper.reader().readTree(data);
             String type = message.path(EVENT).asText(UNKNOWN);
 
-            if (sessionToken == null) {
-                authenticate(type, message);
-            } else {
+
                 UiMessageHandler handler = handlers.get(type);
                 if (handler != null) {
                     log.debug("RX message: {}", message);
@@ -261,7 +259,6 @@ public class UiWebSocket
                 } else {
                     log.warn("No GUI message handler for type {}", type);
                 }
-            }
 
         } catch (Exception e) {
             log.warn("Unable to parse GUI message {} due to {}", data, e);
diff --git a/web/gui2/src/main/webapp/app/onos-routing.module.ts b/web/gui2/src/main/webapp/app/onos-routing.module.ts
index 60ec9d7da6..3abb62376a 100644
--- a/web/gui2/src/main/webapp/app/onos-routing.module.ts
+++ b/web/gui2/src/main/webapp/app/onos-routing.module.ts
@@ -83,10 +83,10 @@ const onosRoutes: Routes = [
         loadChildren: 'app/view/topology/topology.module#TopologyModule'
     },
 /*  Comment out below section for running locally with 'ng serve' when developing */
-    {
+/*    {
         path: 'alarmTable',
         loadChildren: 'fm-gui2-lib#FmGui2LibModule'
-    },
+    },*/
     {
         path: '',
         redirectTo: 'device', // Default to devices view - change to topo in future
```
