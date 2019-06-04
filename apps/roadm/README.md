# UI2 Sample application

This application, created from the ONOS UI2 archetype can be deployed in 2 ways
* As a [standalone application](#Standalone Application outside of ONOS) outside of ONOS OR
* As an [application embedded](#Application embedded within ONOS) within ONOS

There are 2 main parts to it:
* An ONOS OSGi Java bundle that is packaged as an ONOS OAR file, and can be deployed
in to a running ONOS system
* A JavaScript library that may be embedded within an Angular Web project

${symbol_h2} Standalone Application outside of ONOS
In the standalone scenario this library can continue to be built with Maven like:
* mvn clean install

The **app/BUILD.rename** and **web/roadm/BUILD.rename** files can be
discarded or ignored because they are only for building with Bazel.

The ONOS OAR application may be installed with
```bash
onos-app localhost install! app/target/roadm-app-1.0-SNAPSHOT.oar
```

The JavaScript GUI library roadm-gui-lib-1.0.0.tgz may be included in a
[new Angular Project](https://angular.io/guide/quickstart) by adding the line:
```angular2
"roadm-gui-lib": "file:../roadm/web/roadm-gui/roadm-gui-lib-1.0.0.tgz"
```
(ensuring the path is correct) to the **dependencies** section of the
**package.json** of the new Angular application.

Then in the main folder of the new Angular app run
```bash
npm install roadm-gui-lib gui2-fw-lib d3
```

### Add to routing
Finally to use the roadm-gui-lib in your new Angular application, add it to
the Angular Router and make a link to it
* Add a route to it the new app in the file **src/app/app-routing.module.ts** add the following to the "routes" array:
```angular2
  {
    path: 'roadm-gui',
    loadChildren: 'roadm-gui-lib#RoadmGuiLibModule'
  }
```
and
```angular2
 import { RoadmGuiLibModule } from 'roadm-gui-lib';
```
in the imports section at the top of the same file.

* Add a link to the Library in new Application's main page at **src/app/app.component.html**
as **\<a routerLink="/roadm-gui" routerLinkActive="active">roadm page\</a>**
before \<router-outlet>

### Add gui2-fw-lib to module
* In the Angular app's main module **src/app/app.module.ts** add to the providers section:
```angular2
  providers: [
    { provide: LogService, useClass: ConsoleLoggerService },
    { provide: 'Window', useValue: window }
  ],
```
and to the imports section:
```angular2
    Gui2FwLibModule
```
and to the import section:
```angular2
import { Gui2FwLibModule, ConsoleLoggerService, LogService } from 'gui2-fw-lib';
```

### Deploy
At this stage you should be able to build the new application:
```bash
ng build --prod
```
and then copy the files to the html folder of an Apache or Nginx web server
```bash
cp -r dist/<your-app-name>/* /var/www/html
```

And you should be able to open it in your browser at [http://localhost](http://localhost)



${symbol_h2} Application embedded within ONOS
To embed the application inside ONOS (permanently add it to the ONOS code base)
move the application folder in to the ~/onos/apps directory (ensuring its name
does not clash with anything already there).

Run
```bash
cd ~/onos/apps/roadm && \
    mvn -pl web/roadm-gui install && \
    mvn -pl web/roadm-gui clean
```
once inside the folder - this is the easiest way to fetch the dependent node
modules for the Angular application.

To ensure it gets built along with ONOS rename the files **BUILD.rename**,
**app/BUILD.rename** and **web/roadm/BUILD.rename** to **BUILD**, and
remove the pom.xml files, and some extra files left over from the build.
```bash
cd ~/onos/apps/roadm && \
mv BUILD.rename BUILD && \
mv app/BUILD.rename app/BUILD && \
mv web/roadm-gui/BUILD.rename web/roadm-gui/BUILD && \
rm pom.xml && \
rm app/pom.xml && \
rm web/roadm-gui/pom.xml && \
mv web/roadm-gui/gitignore web/roadm-gui/.gitignore && \
chmod +x web/roadm-gui/ng-test.sh && \
rm -rf web/roadm-gui/node_modules/rxjs/src
```

In the file
* ~/onos/apps/roadm/web/roadm-gui/projects/roadm-gui-lib/package.json
change the version to be the current version of ONOS e.g. 2.2.0

Add a reference to the app in **~/onos/tools/build/bazel/modules.bzl** at the
end of the **ONOS_APPS** section as:
```
"//apps/roadm:onos-apps-roadm-oar",
```

In the file **~/onos/web/gui2/BUILD** in the *genrule* section **_onos-gui2-ng-build**
add to the **srcs** section:
```
"//apps/roadm/web/roadm-gui:roadm-gui-lib-build",
```

and in the **cmd** section of the same *genrule* add the following 3 lines
(just before the comment *"# End of add in modules from external packages"*):
```
" ROADM_GUI_LIB_FILES=($(locations //apps/roadm/web/roadm-gui:roadm-gui-lib-build)) &&" +  # An array of filenames - sorted by time created
" tar xf $$ROOT/$${ROADM_GUI_LIB_FILES[0]} &&" +
" mv package/ node_modules/roadm-gui-lib/ &&" +
```

### Add to routing
Finally to use the roadm-gui-lib in the ONOS GUI, add it to the Angular
Router
* Add it as a route to **~/onos/web/gui2/src/main/webapp/app/onos-routing.module.ts**
in the "routes" array as:
```angular2
  {
    path: 'roadm-gui',
    loadChildren: 'roadm-gui-lib#RoadmGuiLibModule'
  },
```
and in the imports section at the top of the same file
```angular2
 import { RoadmGuiLibModule } from 'roadm-gui-lib';
```

### Run the build
Run Bazel build with
```bash
cd ~/onos && \
ob
```
or equivalent.

### Run the application
Start ONOS in the normal way.

Using the ONOS command line, activate the application:
```bash
app activate roadm
```

To turn on ONOS Server side logging, from the same ONOS CLI use:
```bash
log:set DEBUG ${package}
log:set DEBUG org.onosproject.ui.impl
```

On the ONOS GUI the navigation menu should show the new application link. Clicking
on it will navigate to the new app (there is a system of lazy-loading implemented
here so that applications are not loaded until they are first used).

### Rebuild
To rebuild and run the **web** side of the application do:
```bash
bazel build //web/gui2:onos-web-gui2-oar && \
    onos-app localhost reinstall! bazel-bin/web/gui2/onos-web-gui2-oar.oar
```

To rebuild and run the **server** side of the application do:
```bash
bazel build //apps/roadm:onos-apps-roadm-oar && \
    onos-app localhost reinstall! bazel-bin/apps/roadm/onos-apps-roadm-oar.oar
```


### App structure
The application demonstrates some major concepts:
* The use of Angular 7 concepts like Modules, Components, Service

* The top level component - contains the "Fetch Data" button and the connection
to the server backend through a WebSocket
 * Passing a number in the request
 * Receiving a JSON object in reply

* Reuse of items from the **gui2-fw-lib** - with LogService, WebSocketService and
IconComponent

* The use of a child component (WelcomeComponent) in 3 different ways
  * The passing of Inputs to this component
  * The passing of an Event out when the component is clicked

* The embedding of SVG content in to the web page

* The use of TypeScript (as opposed to JavaScript or ECMAScript directly) to ensure
type safety
