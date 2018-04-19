# ONOS GUI 2.0.0

This project is based on __Angular 5__, as an alternative to the 1.0.0 GUI which was based off __AngularJS 1.3.5__

To use the new structure on your system, you need to 
1. Change directory in to onos/web/gui - this is where you will run the `ng` command from.
1. Run `npm install` from this folder to add dependencies
1. Run `npm install -g @angular/cli` to install the `ng` command
1. Run `npm install -g @compodoc/compodoc` to install Compodoc which can generate documentation 
1. Add the following to your PATH environment variable `$ONOS_ROOT/buck-out/gen/web/gui2/node-bin-v8.11.1/node-binaries/bin`

This project was generated with [Angular CLI](https://github.com/angular/angular-cli) version 1.7.4.

## Development server

Run `ng serve` for a dev server. Navigate to `http://localhost:4200/`. The app will automatically reload if you change any of the source files.

## Code scaffolding

Run `ng generate component component-name` to generate a new component. You can also use `ng generate directive|pipe|service|class|guard|interface|enum|module`.

## Build
The build is handled through the web/gui2/BUCK file. This downloads Node, NPM and Angular CLI
It runs ```ng build``` and copies everything over in to WEB-INF/classes/dist (there
is something weird in BUCK resources - if there is a file in the root dir of the
outputted folder this is copied to the sources root directory, where as files
are copied to WEB-INF/classes. To get around this I put all the outputted stuff in to 
```dist``` and it gets copied to /WEB-INF/classes/dist/ )

To start the GUI in a running ONOS at the __onos>__ cli
```
feature:install onos-gui2
```
and the gui will be accessible at [http://localhost:8181/onos/ui2/dist/](http://localhost:8181/onos/ui2/dist/)

## Running unit tests

Run `ng test` to execute the unit tests via [Karma](https://karma-runner.github.io).

## Running end-to-end tests

Run `ng e2e` to execute the end-to-end tests via [Protractor](http://www.protractortest.org/).

## Generating documentation
Run `npm run compodoc` to generate documentation via [Compodoc](https://github.com/compodoc/compodoc)

## Further help

To get more help on the Angular CLI use `ng help` or go check out the [Angular CLI README](https://github.com/angular/angular-cli/blob/master/README.md).
