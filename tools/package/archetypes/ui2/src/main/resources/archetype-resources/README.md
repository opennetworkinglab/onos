# UI2 Sample application

This application, created from the ONOS UI2 archetype can be deployed in 2 ways
* As a [standalone application](#Standalone Application outside of ONOS) outside of ONOS OR
* As an [application embedded](#Application embedded within ONOS) within ONOS

There are 2 main parts to it:
* An ONOS OSGi Java bundle that is packaged as an ONOS OAR file, and can be deployed
in to a running ONOS system
* A JavaScript library that may be embedded within an Angular Web project

${symbol_h2} web/gitignore
The file **web/gitignore** should be renamed to **web/.gitignore** before the app is added
to any git repository

${symbol_h2} Standalone Application outside of ONOS
In the standalone scenario this library can continue to be built with Maven like:
* mvn clean install

The **app/BUILD.rename** and **web/${artifactId}/BUILD.rename** files can be
discarded or ignored because they are only for building with Bazel.

The ONOS OAR application may be installed with
```bash
onos-app localhost install! app/target/${artifactId}-app-1.0-SNAPSHOT.oar
```

The JavaScript GUI library ${artifactId}-gui-lib-1.0.0.tgz may be included in a
[new Angular Project](https://angular.io/guide/quickstart) by adding the line:
```angular2
"${artifactId}-gui-lib": "file:../${artifactId}/web/${artifactId}-gui/${artifactId}-gui-lib-1.0.0.tgz"
```
(ensuring the path is correct) to the **dependencies** section of the
**package.json** of the new Angular application.

Then in the main folder of the new Angular app run
```bash
npm install ${artifactId}-gui-lib gui2-fw-lib d3
```

### Add to routing
Finally to use the ${artifactId}-gui-lib in your new Angular application, add it to
the Angular Router and make a link to it
* Add a route to it the new app in the file **src/app/app-routing.module.ts** add the following to the "routes" array:
```angular2
  {
    path: '${artifactId}-gui',
    loadChildren: '${artifactId}-gui-lib#${appNameCap}${appNameEnd}GuiLibModule'
  }
```
and
```angular2
 import { ${appNameCap}${appNameEnd}GuiLibModule } from '${artifactId}-gui-lib';
```
in the imports section at the top of the same file.

* Add a link to the Library in new Application's main page at **src/app/app.component.html**
as **\<a routerLink="/${artifactId}-gui" routerLinkActive="active">${artifactId} page\</a>**
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
cd ${artifactId} && mvn install && mvn clean
```
once inside the folder - this will fetch the dependent node modules for the
Angular application.

To ensure it gets built along with ONOS rename the files **BUILD.rename**,
**app/BUILD.rename** and **web/${artifactId}/BUILD.rename** to **BUILD**, and
remove the pom.xml files, and some extra files left over from the build.
```bash
cd ~/onos/apps/${artifactId} && \
mv BUILD.rename BUILD && \
mv app/BUILD.rename app/BUILD && \
mv web/${artifactId}-gui/BUILD.rename web/${artifactId}-gui/BUILD && \
rm pom.xml && \
rm app/pom.xml && \
rm web/${artifactId}-gui/pom.xml && \
mv web/${artifactId}-gui/gitignore web/${artifactId}-gui/.gitignore && \
chmod +x web/${artifactId}-gui/ng-test.sh && \
rm -rf web/${artifactId}-gui/node_modules/rxjs/src
```


In the file
* ~/onos/apps/${artifactId}/web/${artifactId}-gui/projects/${artifactId}-gui-lib/package.json
change the version to be the current version of ONOS e.g. 2.2.0

Add a reference to the app in **~/onos/tools/build/bazel/modules.bzl** at the
end of the **ONOS_APPS** section as:
```
"//apps/${artifactId}:onos-apps-${artifactId}-oar",
```

In the file **~/onos/web/gui2/BUILD** in the *genrule* section **_onos-gui2-ng-build**
add to the **srcs** section:
```
"//apps/${artifactId}/web/${artifactId}-gui:${artifactId}-gui-lib-build",
```

and in the **cmd** section of the same *genrule* add the following 3 lines
(just before the comment *"# End of add in modules from external packages"*):
```
" ${appNameAllCaps}_GUI_LIB_FILES=($(locations //apps/${artifactId}/web/${artifactId}-gui:${artifactId}-gui-lib-build)) &&" +  # An array of filenames - sorted by time created
" tar xf $$ROOT/$${${appNameAllCaps}_GUI_LIB_FILES[0]} &&" +
" mv package/ node_modules/${artifactId}-gui-lib/ &&" +
```

### Add to routing
Finally to use the ${artifactId}-gui-lib in the ONOS GUI, add it to the Angular
Router
* Add it as a route to **~/onos/web/gui2/src/main/webapp/app/onos-routing.module.ts**
in the "routes" array as:
```angular2
  {
    path: '${artifactId}-gui',
    loadChildren: '${artifactId}-gui-lib#${appNameCap}${appNameEnd}GuiLibModule'
  }
```
and in the imports section at the top of the same file
```angular2
 import { ${appNameCap}${appNameEnd}GuiLibModule } from '${artifactId}-gui-lib';
```

### Run the build
Run Bazel build with
```bash
cd ~/onos && \
ob
```
or equivalent.
