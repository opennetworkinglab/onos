# FmGui2LibApp

This project contains the FM GUI2 web application as an NPM library module

It is a new approach to GUI extensions in GUI2, where each extension is implemented in its own
library module as an Angular module containing one or more Angular components, services or directives.
It benefits from the lazy loading approach of the ONOS GUI2 application, and so does not have injected
js and css files as the old approach had. 

It contains the component AlarmTableComponent and AlarmDetailsComponent, and is built on top of
classes from the GUI2 framework library (gui2-fw-lib).

This is integrated in to the main ONOS GUI2 web application by adding it to the 
"onos-routing.module.ts" in web/gui2/src/main/webapp/app and in to nav.html

```

===============================
== ONOS GUI2 Web Application ==
===============================
           ||
           || Lazy loads
           \/
===================================
== FM GUI2 LIB module (incl ...) ==
==   Alarm Table Component       ==
==   Alarm Details Component     ==
===================================
           ||
           || Depends
           \/
=============================
== GUI2 FW Lib             ==
==  Web Sockets, LION etc  ==
=============================

```


This project was generated with [Angular CLI](https://github.com/angular/angular-cli) version 6.0.0.

A couple of good articles on the creation and use of __libraries__ in Angular 6 is given in:

[The Angular Library Series - Creating a Library with the Angular CLI](https://blog.angularindepth.com/creating-a-library-in-angular-6-87799552e7e5)

and

[The Angular Library Series - Building and Packaging](https://blog.angularindepth.com/creating-a-library-in-angular-6-part-2-6e2bc1e14121)

The Bazel build of this library handles the building and packaging of the library
so that other projects and libraries can use it.

## Development server

To build the library project using Angular CLI run 'ng build --prod fm-gui2-lib'
inside the ~/onos/apps/faultmanagement/fm-gui2-lib folder

To make the library in to an NPM package use 'npm pack' inside the dist/fm-gui2-lib folder

To build the app that surrounds the library run 'ng build'. This app is not
part of the ONOS GUI and is there as a placeholder for testing the library

Run `ng serve` for a dev server. Navigate to `http://localhost:4200/`.
The app will automatically reload if you change any of the source files.
__NOTE__ If you make changes to files in the library, the app will not pick them up until you build the library again

## Code scaffolding

Run `ng generate component <component-name> --project=fm-gui2-lib` to generate a new component. You can also use `ng generate directive|pipe|service|class|guard|interface|enum|module`.

## Build

Run `ng build` to build the project. The build artifacts will be stored in the `dist/` directory. Use the `--prod` flag for a production build.

## Running unit tests

Run `ng test` to execute the unit tests via [Karma](https://karma-runner.github.io).

## Running end-to-end tests

Run `ng e2e` to execute the end-to-end tests via [Protractor](http://www.protractortest.org/).

## Further help

To get more help on the Angular CLI use `ng help` or go check out the [Angular CLI README](https://github.com/angular/angular-cli/blob/master/README.md).
