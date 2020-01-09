# UI2 Sample application

This application, created from the ONOS UI2 archetype can be deployed as a
[standalone application](#Standalone Application outside of ONOS) outside of ONOS

To create a GUI within ONOS please see [onos/web/gui2/README.md](onos/web/gui2/README.md)

There are 2 main parts to this app:
* An ONOS OSGi Java bundle that is packaged as an ONOS OAR file, and can be deployed
in to a running ONOS system
* An Angular Web project - built with Angular CLI through NPM in Maven

${symbol_h2} Standalone Application outside of ONOS
In the standalone scenario this library can continue to be built with Maven like:
* mvn clean install

> The **app/BUILD**, **web/${artifactId}/BUILD** and any **BUILD.bazel** files can
> be discarded or ignored because they are only for building with Bazel. They do
> allow the project to be converted to Bazel pretty easily though.

The ONOS OAR application may be installed with
```bash
onos-app localhost install! app/target/${artifactId}-app-1.0-SNAPSHOT.oar
```

### Development
The GUI can be built locally with the Angular CLI command 
**ng**[https://cli.angular.io/](https://cli.angular.io/)

> You might already have this installed - see [https://docs.onosproject.org/onos-gui/docs/prerequisites/](https://docs.onosproject.org/onos-gui/docs/prerequisites/)

```bash
ng serve
```

And you should be able to open it in your browser at [http://localhost:4200](http://localhost:4200)

${symbol_h2} Application embedded within ONOS
To embed the application inside ONOS (permanently add it to the ONOS code base)
move the application folder in to the ~/onos/apps directory (ensuring its name
does not clash with anything already there).

For the GUI only the **src** folder should be copied over - all else can be discarded.
The WORKSPACE and other support files will be accessed from the root of ~/onos

### App structure
The application demonstrates some major concepts:
* The use of Angular concepts like Modules, Components, Service

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
