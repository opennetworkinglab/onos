# ONOS GUI 2.0.0

This project is based on __[Angular 6](https://angular.io/docs)__, as an alternative to the 1.0.0 GUI which was based 
off __[AngularJS 1.3.5](https://angularjs.org/)__

Building, testing and running lint are all handled by BUCK. See web/gui2/BUCK file.

To use this new GUI you simply have to start the GUI in a running ONOS at the __onos>__ cli:
```
feature:install onos-gui2
```
and the gui will be accessible at [http://localhost:8181/onos/ui2](http://localhost:8181/onos/ui2)

# Development
There are 2 ways to go about development - 
1. rebuild the code and rerun through BUCK OR (much like can be done with any ordinary ONOS app)
2. use Angular 6 CLI (ng command) to rebuild on the fly (must faster for development) 

For 1) if you change the code you can redeploy the application with (requies you to be in ~/onos directory):
```
onos-buck build //web/gui2:onos-web-gui2-oar --show-output|grep /app.oar | cut -d\  -f2 | xargs onos-app localhost reinstall!
```

For 2) it's well worth becoming familiar with Angular CLI.
The project is created with [Angular CLI](https://github.com/angular/angular-cli) v6 to simplify development of the browser side code.

This allows you to develop the Angular 6 Type Script code independent of ONOS in a separate container. 
Since WebSockets have been implemented (Jun 18) there is a requirement to run ONOS in the background.

There is no need to install node, npm or ng again on your system, and indeed if they are already installed, it's best
to use the versions of these that's used by BUCK. To do this add to the __start__ of your PATH environment variable. 
```
~/onos/buck-out/gen/web/gui2/node-bin-v8.11.1/node-binaries/bin

```
On Linux:
```
export PATH=~/onos/buck-out/gen/web/gui2/node-bin-v8.11.1/node-binaries/bin:$PATH
``` 

After this you should be able to run 'ng -v' and see:
```
Angular CLI: 6.0.0
Node: 8.11.1
OS: linux x64
```

To use Angular CLI for development on your system, you need to: 
1. Change directory in to onos/web/gui2 - this is where you will run the `ng` command from.
2. Run `npm install` once from this folder to add dependencies
3. Then run 'ng -v' from onos/web/gui2 and an additional version should be shown __Angular: 6.0.0__

## Development server

Run `ng serve` for a dev server. Navigate to `http://localhost:4200/`. The app will automatically reload if you change any of the source files.

Press Ctrl-Shift-I in Chrome and Firefox to bring up the developer tools and the browser console. 

## Code scaffolding

Run `ng generate component component-name` to generate a new component. You can also use `ng generate directive|pipe|service|class|guard|interface|enum|module`.

## Build
The build is handled through the web/gui2/BUCK file. This downloads Node, NPM and Angular CLI
It runs ```ng build``` and copies everything over in to WEB-INF/classes/dist (there
is something weird in BUCK resources - if there is a file in the root dir of the
outputted folder this is copied to the sources root directory, where as files
are copied to WEB-INF/classes. To get around this I put all the outputted stuff in to 
```dist``` and it gets copied to /WEB-INF/classes/dist/ )

To run it manually in Angular CLI run `ng build`

## Running unit tests
This is automatically done when using "onos-buck test" - see the web/gui2/BUCK file for more details.

To run it manually in Angular CLI run `ng test --watch` to execute the unit tests via [Karma](https://karma-runner.github.io).
Running it directly like this will test with both Firefox and Chrome. To use only one use the __--browsers__ argument

## Running checkstyle
This is automatically done when using "onos-buck test" - see the web/gui2/BUCK file for more details.

To run it manually in Angular CLI run `ng lint` to run codelyzer on your code, according to the rules in __tslint.json__

## Running end-to-end tests

To run it manually in Angular CLI run `ng e2e` to execute the end-to-end tests via [Protractor](http://www.protractortest.org/).

## Generating documentation
This is automatically done when using "onos-buck onos build" - see the web/gui2/BUCK file for more details.

To run it manually in Angular CLI run `npm run compodoc` to generate documentation via [Compodoc](https://github.com/compodoc/compodoc)

## Further help

To get more help on the Angular CLI use `ng help` or go check out the [Angular CLI README](https://github.com/angular/angular-cli/blob/master/README.md).
