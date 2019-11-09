# gui2-fw-lib

This project separates out the Framework part of the ONOS GUI2 project into a separate Angular library
that is published on NPM at https://www.npmjs.com/package/gui2-fw-lib

This for reuse outside of ONOS by any project such as [µONOS GUI](https://github.com/onosproject/onos-gui)

This can be published from ONOS Bazel with
```bash
bazel run //web/gui2-fw-lib/projects/gui2-fw-lib:gui2-fw-lib-pkg.publish
```

or created locally with
```bash
bazel build //web/gui2-fw-lib/projects/gui2-fw-lib:onos-gui2-fw-ng-build
```
