# ONOS GUI 2.5.0

This project is based on __[Angular 10](https://angular.io/docs)__ 
and __[ES6](http://www.ecma-international.org/ecma-262/6.0/index.html)__ (aka __ES2015__), 
as an alternative to the 1.0.0 GUI which was based 
off __[AngularJS 1.3.5](https://angularjs.org/)__

Building, testing and running lint are all handled by Bazel. See 

* web/gui2/BUILD
* web/gui2/src/main/webapp/BUILD.bazel
* web/gui2/src/main/webapp/app/BUILD.bazel

To use this new GUI you simply have to ensure it is running on ONOS at the __onos>__ cli:
```
app activate gui2
```
and the gui will be accessible at [http://localhost:8181/onos/ui](http://localhost:8181/onos/ui)

Gui2 is loaded by default as ONOS starts since it is added to ONOS_APPS
```
export ONOS_APPS=${ONOS_APPS:-drivers,openflow,gui2}
```

The original legacy GUI is also loadable as an app, and is available at the same
web resource onos/ui on port 8181
The legacy GUI should be disabled if GUI2 is active and vice versa
```
app deactivate gui
```

As usual with ONOS if you want to run it in a different language set the
__ONOS_LOCALE__ environment variable to the locale you want before starting onos. e.g.
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

The main framework used is Angular 10, with a strong emphasis on making resuable
components, directives, services and classes. Angular Routing and animation are
also used.

# Issues
Issues found on the GUI2 should be added to the existing list on the
[ONOS GUI Jira Kanban board](https://jira.onosproject.org/secure/RapidBoard.jspa?rapidView=31)
(requires Jira login). Assigning the Epic Link `GUI` will ensure the issue appears
 on this board

# Development
Development requires a TypeScript capable IDE (Intellij WebStorm or Idea Ultimate Edition)

Generally the simplest option for development is to rebuild and redeploy on the
command line with:
```
bazel build //web/gui2:onos-web-gui2-oar && onos-app localhost reinstall! bazel-bin/web/gui2/onos-web-gui2-oar.oar
```

> In future support for **devmode** target will be added in conjunction with IBazel.

>There is no need to install node, npm or yarn on your system, and indeed if
>they are already installed, it's best to use the versions used
>by Bazel in ONOS.

All building of GUI artefacts is now done through native Bazel rules from
[https://github.com/bazelbuild/rules_nodejs](https://github.com/bazelbuild/rules_nodejs)

The build sequence is 
* ng_module (compile TypeScript and pull in html and css files for components)
* rollup_bundle (transpose all dependent ng_modules in to Javascript, splitting based on routing modules)
* terser_minified (minifies the output from rollup)
* pkg_web (gather together all assets and the minified bundle in to a single folder - prodapp)
* genrule - onos-web-gui2 (gather the prodapp with the Java files and web.xml to create an Karaf web application) 

## Yarn and NPM
In WORKSPACE targets are defined to install specific versions of Javascript packages
* ```@gui1_npm``` - this uses NPM  to install all the NPM packages in ```//tools/gui:package.json```
* ```@npm``` - this uses Yarn to install all the NPM packages listed in ```//web/gui2:package.json```

> It is not possible to rename this `@npm` to `@gui2_npm` at this time because many
> of the Javascript bazel rules depend on this naming convention. 

To check the Yarn installation you can run
```bash
bazel run @nodejs//:yarn -- versions
```

NPM is also available (but this is mainly used for GUI 1) (and will be phased out eventually).
```bash
bazel run @nodejs//:npm version
```

## The Angular CLI
> The project no longer fully supports Angular CLI (with the **ng** command). Some
> standalone commands can be run like **version**, **help**, **generate**, **config**,
> but actions like **build**, **test**, **serve**, **lint**, **run**
> are just a wrapper for bazel commands, and should be run directly in Bazel as described
> elsewhere.

The Angular CLI (ng) is available inside Bazel as (from `~/onos/web/gui2`):
```bash
bazel run @npm//:node_modules/@angular/cli/bin/ng version
```

> To give an argument to a sub command of bazel like this it must be prefixed with an extra `--`
> `bazel run @npm//:node_modules/@angular/cli/bin/ng config -- version`

When run inside an Angular Project (from `~/onos/web/gui2`) we get:
```bash
Angular CLI: 9.0.0-rc.7
Node: 10.16.0
OS: linux x64

Angular: 9.0.0-rc.7
... animations, bazel, cli, common, compiler, compiler-cli, core
... forms, platform-browser, platform-browser-dynamic, router
Ivy Workspace: <error>

Package                      Version
------------------------------------------------------
@angular-devkit/architect    0.900.0-rc.7
@angular-devkit/core         9.0.0-rc.7
@angular-devkit/schematics   9.0.0-rc.7
@bazel/hide-bazel-files      1.0.0
@bazel/karma                 1.0.0
@bazel/protractor            1.0.0
@bazel/rollup                1.0.0
@bazel/terser                1.0.0
@bazel/typescript            1.0.0
@schematics/angular          9.0.0-rc.7
@schematics/update           0.900.0-rc.7
rxjs                         6.5.4
typescript                   3.6.4
```

## Creating a new GUI module and component
The Angular CLI (**ng generate**) command for generating new modules and components
can still be used. For example to create a new module inside an project
(from ```web/gui2/src/main/webapp/app/view```) do:
```bash
bazel run @npm//:node_modules/@angular/cli/bin/ng generate module mynewmodule
```

Components can then be added to this module:
```bash
cd mynewmodule
bazel run @npm//:node_modules/@angular/cli/bin/ng generate component comp1
```

This module should then be given it's own ```BUILD.Bazel``` file to compile this
```ng_module```, with at least the following:
```bazel
package(default_visibility = ["//:__subpackages__"])

load("@io_bazel_rules_sass//:defs.bzl", "sass_binary")
load("@npm//@angular/bazel:index.bzl", "ng_module")

sass_binary(
    name = "mynewmodule-styles",
    src = ":comp1/comp1.component.scss",
)

ng_module(
    name = "mynewmodule",
    srcs = [
        "comp1/comp1.component.ts",
        "mynewmodule.module.ts",
    ],
    assets = [
        ":mynewmodule-styles",
        ":comp1/comp1.component.html",
    ],
    module_name = "mynewmodule",
    deps = [
        "//web/gui2-fw-lib",
        "@npm//@angular/core",
        "@npm//@angular/platform-browser-dynamic",
        "@npm//@angular/router",
        "@npm//rxjs",
    ],
)
```

This can be built directly with
```bash
bazel build //web/gui2/src/main/webapp/app/view/mynewmodule:mynewmodule
```
To add it to the overall ONOS GUI it can be added to the list of dependencies of
the ```app``` target in ```web/gui2/src/main/webapp/app/BUILD.bazel``` as:
```bazel
"//web/gui2/src/main/webapp/app/view/mynewmodule:mynewmodule",
```

For regular embedded use, this component can be referenced from another component
by its selector (defined in
`web/gui2/src/main/webapp/app/view/mynewmodule/comp1/comp1.component.ts`)
```html
<onos-comp1></onos-comp1>
```
**OR**

this component can be used as a Routable module and accessed from the top level
of onos GUI, as described in the next section.

### Adding the new component as a routable module
To make the module routable, add it to the top level onos router in 
```web/gui2/src/main/webapp/app/onos-routing.module.ts```. 

Add it to the ```onosRoutes``` set as:
```typescript
<Route>{
    path: 'mynewmodule',
    pathMatch: 'full',
    loadChildren: () => import('./view/mynewmodule/mynewmodule.module').then(m => m.MynewmoduleModule),
},
```
and then update the modules own routing table in
`web/gui2/src/main/webapp/app/view/mynewmodule/mynewmodule.module.ts` with a subroute:
```typescript
...
import {RouterModule} from '@angular/router';

@NgModule({
    imports: [
        CommonModule
        RouterModule.forChild([{path: '', component: Comp1Component}]),
    ],
    declarations: [Comp1Component]
})
...
``` 

This can then be accessed at
[http://localhost:8181/onos/ui/#/mynewmodule]

and should display **comp1 works!** from `comp1.component.html`

## GUI FW Lib
The GUI2 __framework__ (in `~/onos/web/gui2-fw-lib`) is at the heart of the GUI2
implementation and contains core items like icon libraries, base classes etc.
The main GUI __application__ is in `~/onos/web/gui2` (and depends on this framework).

When the library is packaged up in
[Angular Package Format](https://docs.google.com/document/d/1CZC2rcpxffTDfRDs6p1cfbmKNLA6x5O-NtkJglDaBVs/preview)
and can be manually uploaded to [https://www.npmjs.com/package/gui2-fw-lib](https://www.npmjs.com/package/gui2-fw-lib)
to be used in other projects (e.g. [onos-gui](https://github.com/onosproject/onos-gui)

## GUI2 Topo library
The GUI2 __Topology__ is in `~/onos/web/gui2-topo-lib` and the GUI __application__
includes this Topology application through the `onos-routing.module`. The 
Topology app has its own README file.

## GUI2 Application
The application is the visible front end and contains the ONOS `index.html` and
all of the tabular views and the topology view. It references the `gui2-fw-lib`
and `gui2-topo-lib` as just other dependencies. 

## Running unit tests
This is automatically done when using:

* for FW lib `bazel test //web/gui2-fw-lib:test-not-coverage`
* for Topo lib `bazel test //web/gui2-topo-lib:test-not-coverage`

> Note: these tests are **not** run by Jenkins, or when you do a unit test build with
> the `ot` or `ob` alias locally. They have been omitted, as they required everyone
> to have ChromeHeadless installed locally. Instead they **must** be run by GUI
> developers before submitting GUI code.
>
> Currently there is no test for the main `gui2` project. They hve been temporarily
> disabled because of Bazel-Karma issues. 

## Running checkstyle (lint)
This is automatically done when using `bazel test //web/gui2:onos-gui2-ng-tests`
> Temporarily disabled.

## Generating documentation
To run it manually in Angular CLI run `npm run compodoc` to generate documentation
via [Compodoc](https://github.com/compodoc/compodoc)
