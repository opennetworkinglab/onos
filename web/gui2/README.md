# ONOS GUI 2.0.0

This project is based on __[Angular 6](https://angular.io/docs)__ 
and __[ES6](http://www.ecma-international.org/ecma-262/6.0/index.html)__ (aka __ES2015__), 
as an alternative to the 1.0.0 GUI which was based 
off __[AngularJS 1.3.5](https://angularjs.org/)__

Building, testing and running lint are all handled by Bazel. See web/gui2/BUILD file.

To use this new GUI you simply have to start the GUI in a running ONOS at the __onos>__ cli:
```
feature:install onos-gui2
```
and the gui will be accessible at [http://localhost:8181/onos/ui2](http://localhost:8181/onos/ui2)

As usual with ONOS if you want to run it in a different language set the __ONOS_LOCALE__ environment variable
to the locale you want before starting onos. e.g.
```
ONOS_LOCALE=fr_FR SHLVL=1 bazel run onos-local -- clean debug
```

# Development
There are 2 ways to go about development - 
1. rebuild the code and rerun through Bazel (much like can be done with any ordinary ONOS app) 
 (this is not optimal though since in this mode the browser side code is built in '--prod' mode
 and all debug symbols are stripped and debug statements are not logged and the code is uglified and minimized.
 It is useful for testing "prod" mode works though and saves having to set up direct development) OR
2. use Angular 6 CLI (__ng__ command) to rebuild on the fly (must faster for development) 

For 1) (this needs to be updated for Bazel commands) if you change the code you can redeploy the application without restarting ONOS with (requires you to be in ~/onos directory):
```
onos-buck build //web/gui2:onos-web-gui2-oar --show-output|grep /app.oar | cut -d\  -f2 | xargs onos-app localhost reinstall!
```

For 2) it's well worth becoming familiar with Angular CLI.
The project is created with [Angular CLI](https://github.com/angular/angular-cli) v6 to simplify development of the browser side code.
It is complicated to set up, but is worth the effort if you have more than a day's worth of development to do.
This allows you to develop the Angular 6 TypeScript code independent of ONOS in a separate container. 
Since WebSockets have been implemented - there is a requirement to run ONOS in the background.

There is no need to install node, npm or ng again on your system, and indeed if they are already installed, it's best
to use the versions of these that's used by Bazel. To do this add the following 2 entries to the __start__ of your PATH environment variable. 
```
~/.cache/bazel/_bazel_scondon/8beba376f58d295cf3282443fe02c21a/external/nodejs/bin/nodejs/bin
```
(where ~/.cache/bazel/_bazel_scondon/8beba376f58d295cf3282443fe02c21a should be replaced by an equivalent folder on your system)
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

The GUI2 framework is a library inside its own mini application (unrelated to the main GUI application) - every thing of importance 
is in projects/gui2-fw-lib. The own application is just a wrapper around the framework
library - it has to be there for Angular CLI to work. 

If you make any changes here or are using it for the first time it will need to be built
```text
pushd ~/onos/web/gui2-fw-lib && \
ng build gui2-fw-lib && \
cd dist/gui2-fw-lib && \
npm pack && \
popd && \
npm install gui2-fw-lib
```

To test and lint it use
```text
ng lint gui2-fw-lib && \
ng test gui2-fw-lib
```

This packages the Framework up in to __onos/web/gui2-fw-lib/dist/gui2-fw-lib/gui2-fw-lib-2.0.0.tgz__

## GUI2 Topo library
The GUI2 __Topology__ is in __~/onos/web/gui2-topo-lib__ and the GUI __application__
includes this Topology application through the __onos-routing-module__. The 
Topology app has its own README file.

If you make any changes here or are using it for the first time it will need to be built
```text
pushd ~/onos/web/gui2-topo-lib && \
ng build gui2-topo-lib && \
cd dist/gui2-topo-lib && \
npm pack && \
popd && \
npm install gui2-topo-lib
```
This packages the Framework up in to __onos/web/gui2-topo-lib/dist/gui2-topo-lib/gui2-topo-lib-2.0.0.tgz__

## GUI2 Application
The application contains the ONOS index.html and all of the tabular views and the topology view.
It references the gui2-fw-lib, as just another dependency. 

To use this application in Angular CLI for development on your system, you need to: 
1. Change directory in to onos/web/gui2 - this is where you will run the `ng` command from.
2. Run `npm install` once from this folder to add dependencies
3. Then run 'ng -v' from onos/web/gui2 and an additional version should be shown __Angular: 6.0.0__
4. Temporarily make a change to disable authentication in UIWebSocket.java
5. Temporarily make a change to disable external routes in onos-routing.module.ts
6. Create symbolic links for some CSS files

### Disable authentication and external routes
Before the server can be run a couple of small adjustments need to be temporarily made
1. The file __~/onos/web/gui/src/main/java/org/onosproject/ui/impl/UiWebSocket.java__ 
needs to be adjusted to remove authentication
2. The file __~/onos/web/gui2/src/main/webapp/app/onos-routing.module.ts__ needs 
to be adjusted to remove references to routes in external applications 

These changes are given in Appendix A at the end of this document - these changes should not be 
checked in though - as they are not required (and will break) the GUI2 embedded in ONOS.

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

To run it manually in Angular CLI run `ng test --watch` to execute the unit tests via [Karma](https://karma-runner.github.io).
Running it directly like this will test with both Firefox and Chrome. To use only one use the __--browsers__ argument

## Running checkstyle (lint)
This is automatically done when using "bazel test" - see the web/gui2/BUILD file for more details.

To run it manually in Angular CLI run `ng lint` to run codelyzer on your code, according to the rules in __tslint.json__

## Running end-to-end tests

To run it manually in Angular CLI run `ng e2e` to execute the end-to-end tests via [Protractor](http://www.protractortest.org/).

## Generating documentation
This is automatically done when using "ob" - see the web/gui2/BUILD file for more details.

To run it manually in Angular CLI run `npm run compodoc` to generate documentation via [Compodoc](https://github.com/compodoc/compodoc)

## Further help

To get more help on the Angular CLI use `ng help` or go check out the [Angular CLI README](https://github.com/angular/angular-cli/blob/master/README.md).


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
