# Gui2FwLibApp

This project separates out the Framework part of the ONOS GUI2 project in to a separate Angular library

It is separate to the main ONOS GUI2 project which is in ~/onos/web/gui2

It allows external applications e.g. [onos-gui](https://github.com/onosproject/onos-gui)
to use it, without bringing along the whole of GUI 2

The Bazel build of this library handles the building and packaging of the library
so that other projects and libraries can use it.

## Development server
To build the `npm` library project using Bazel run:
`bazel build //web/gui2-fw-lib:gui2-fw-lib-npm`
inside the `~/onos` folder.

To make the library in to an NPM package use
`bazel run //web/gui2-fw-lib:gui2-fw-lib-npm.pack`

## Code scaffolding

Run `bazel run @npm//:node_modules/@angular/cli/bin/ng generate component component-name --project=gui2-fw-lib`
to generate a new component. You can also use
`bazel run @npm//:node_modules/@angular/cli/bin/ng generate directive|pipe|service|class|guard|interface|enum|module`.

## Running unit tests
Run `bazel test //web/gui2-fw-lib:test-not-coverage` to execute the unit tests via [Karma](https://karma-runner.github.io).
