"""
 Copyright 2018-present Open Networking Foundation

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
"""

"""
    Rules to build the ONOS GUI 2 FW Lib

    The GUI2 Angular 7 elements are built here with Angular CLI 'ng'
    Some work is being done in the Bazel community to integrate Bazel and
    Angular 7, (Angular Buildtools Convergence -
    https://docs.google.com/document/d/1OlyiUnoTirUj4gecGxJeZBcjHcFr36RvLsvpBl2mxA8/preview)
    but it is in the very early stages (Aug'18) and not yet fit
    for production and at present it works as a replacement for Angular CLI
    (which is not desirable).

    There are plans to extend Bazel it to work with Angular CLI, and if works
    well this Bazel file may be rearchiteced in future.

    Bazel and npm are incompatibe in how they deal with files. npm likes to
    follow links to get back to the original canonical path names, and bazel
    uses links extensively when populating the sandbox. To get around these
    problems, the rules that follow use filegroups to specify the files as
    dependencies and then use a genrule to convert the files into a tar ball.
    Once the tar ball is unrolled into the sandbox, the links are broken, but
    the build is still hermetic since those files are referred to as dependencies in the genrule.
"""

load("//tools/build/bazel:jdk_genrule.bzl", genrule = "jdk_genrule")

COMPILE_DEPS = CORE_DEPS + JACKSON + KRYO + [
    "@javax_ws_rs_api//jar",
    "@servlet_api//jar",
    "@jetty_websocket//jar",
    "@jetty_util//jar",
    "@jersey_media_multipart//jar",
    "@jersey_server//jar",
    "//utils/rest:onlab-rest",
    "//core/store/serializers:onos-core-serializers",
]

TEST_DEPS = TEST + [
    "//core/api:onos-api-tests",
    "//drivers/default:onos-drivers-default",
]

"""
    Files that get put at the top level of the tar ball
"""

filegroup(
    name = "_root_level_files",
    srcs =
        [
            ":angular.json",
            ":package.json",
            ":package-lock.json",
            ":tsconfig.json",
            ":tslint.json",
        ],
)

filegroup(
    name = "_e2e_test_files",
    srcs = [
        ":e2e/protractor.conf.js",
        ":e2e/src/app.e2e-spec.ts",
        ":e2e/src/app.po.ts",
        ":e2e/tsconfig.e2e.json",
    ],
)

"""
    Make a group file of all the webapp files.
"""

filegroup(
    name = "_gui2_fw_lib_src",
    srcs = glob(
        [
            "projects/gui2-fw-lib/**",
            "README.md",
            "LICENSE",
        ],
        exclude = [
            "projects/gui2-fw-lib/**/*.spec.*",  # Don't track tests here
            "projects/gui2-fw-lib/karma.conf.js",
            "projects/gui2-fw-lib/src/test.ts",
        ],
    ),
)

"""
    Make a group of all the webapp qpp files.
"""

filegroup(
    name = "_gui2_app_files",
    srcs = glob(
        [
            "src/**/*",
        ],
    ),
)

"""
    Make a jar file of all the webapp test (*.spec.ts) files.
"""

genrule(
    name = "_gui2_fw_lib_tests",
    srcs = glob(
        [
            "projects/gui2-fw-lib/karma.conf.js",
            "projects/gui2-fw-lib/src/test.ts",
            "projects/gui2-fw-lib/tsconfig.spec.json",
            "projects/gui2-fw-lib/**/*.spec.ts",
        ],
        exclude = [
            "projects/gui2-fw-lib/ng-package.json",
            "projects/gui2-fw-lib/ng-package.prod.json",
            "projects/gui2-fw-lib/package.json",
            "projects/gui2-fw-lib/tsconfig.lib.json",
            "projects/gui2-fw-lib/tslint.json",
            "projects/gui2-fw-lib/src/public_api.ts",
        ],
    ),
    outs = ["gui2_fw_lib_tests.jar"],
    cmd = "cd web/gui2-fw-lib &&" +
          " jar Mcf ../../$@ .",
)

"""
    Make a jar file of all the CSS files we want to reuse in the target.
"""

genrule(
    name = "gui2_fw_lib_ext_css",
    srcs = glob(
        [
            "projects/gui2-fw-lib/src/lib/widget/panel.css",
            "projects/gui2-fw-lib/src/lib/widget/panel-theme.css",
            "projects/gui2-fw-lib/src/lib/widget/table.css",
            "projects/gui2-fw-lib/src/lib/widget/table.theme.css",
            "projects/gui2-fw-lib/src/lib/widget/table.theme.css",
            "projects/gui2-fw-lib/src/lib/layer/loading.service.css",
        ],
    ),
    outs = ["gui2_fw_lib_css.jar"],
    cmd = " ROOT=`pwd` &&" +
          " cd web/gui2-fw-lib/projects/gui2-fw-lib/src/lib &&" +
          " jar Mcf $$ROOT/$@ .",
    visibility = ["//visibility:public"],
)

"""
    Install npm packages listed in package.json in web/gui2
    See bazel-out/k8-fastbuild/bin/web/gui2/onos-gui2-npm-install.log for details of the 'npm install'
"""

genrule(
    name = "onos-gui2-fw-npm-install",
    srcs = [
        "@nodejs//:bin/npm",
        "@nodejs//:bin/node",
        "@nodejs//:bin/nodejs/bin/node",
        "@nodejs//:bin/nodejs/bin/npm",
        ":_root_level_files",
    ],
    outs = [
        "onos-gui2-npm-install.jar",
        "onos-gui2-npm-install.log",
    ],
    cmd = " ROOT=`pwd` &&" +
          " export HOME=. &&" +
          " export XDG_CONFIG_HOME=$(@D)/config &&" +  # npm config cache to the sandbox
          " export BABEL_DISABLE_CACHE=1 &&" +  # turn off babel cache
          ' if [[ ! -z $${HTTP_PROXY-} ]]; then NPM_ARGS="--proxy $$HTTP_PROXY --without-ssl --insecure"; else NPM_ARGS=""; fi &&' +
          " NPM=$(location @nodejs//:bin/npm) &&" +
          " export PATH=$$ROOT/$$(dirname $${NPM}):$$PATH &&" +
          " npm -v > $$ROOT/$(location onos-gui2-npm-install.log) 2>&1 &&" +
          " cd web/gui2-fw-lib && " +
          " npm $$NPM_ARGS install --no-cache >> $$ROOT/$(location onos-gui2-npm-install.log) 2>&1 &&" +
          " jar Mcf $$ROOT/$(location onos-gui2-npm-install.jar) node_modules &&" +
          " touch $$ROOT/$(location onos-gui2-npm-install.log)",  # to get the log always as the 2nd file
    message = "NodeJS npm install",
    visibility = ["//visibility:public"],
)

"""
    Run ng build to create outputs in production mode
    See bazel-out/k8-fastbuild/bin/web/gui2/onos-gui2-ng-build-prod.log for details of the Angular CLI output
"""

genrule(
    name = "onos-gui2-fw-ng-build",
    srcs = [
        "@nodejs//:bin/npm",
        "@nodejs//:bin/node",
        "@nodejs//:bin/nodejs/bin/node",
        "@nodejs//:bin/nodejs/bin/npm",
        ":onos-gui2-fw-npm-install",
        "//web/gui:onos-gui-data-for-gui2",
        ":_root_level_files",
        ":_gui2_fw_lib_src",
    ],
    outs = [
        "gui2-fw-ng-build-prod.log",
        "gui2-fw-lib-ver.tgz",
    ],
    cmd = "ROOT=`pwd` &&" +
          " export HOME=. &&" +
          " export XDG_CONFIG_HOME=$(@D)/config &&" +
          " NODE=$(location @nodejs//:bin/node) &&" +
          " INSTALL_FILES=($(locations :onos-gui2-fw-npm-install)) &&" +  # An array of filenames - sorted by time created
          " cd web/gui2-fw-lib &&" +
          " jar xf $$ROOT/$${INSTALL_FILES[0]} &&" +
          " chmod +x $$ROOT/web/gui2-fw-lib/node_modules/@angular/cli/bin/ng &&" +
          " export PATH=$$ROOT/$$(dirname $${NODE}):$$ROOT/web/gui2-fw-lib/node_modules/@angular/cli/bin:$$PATH &&" +
          " node -v > ../../$(location gui2-fw-ng-build-prod.log) &&" +
          " npm -v >> ../../$(location gui2-fw-ng-build-prod.log) &&" +
          " ng version >> ../../$(location gui2-fw-ng-build-prod.log);" +
          " ng build --prod gui2-fw-lib >> $$ROOT/$(location gui2-fw-ng-build-prod.log) 2>&1 ||" +
          " if [ $$? -eq 0 ]; then echo 'Successfully built GUI FW library';" +
          " else " +
          "   echo 'Error running \'ng build gui2-fw-lib\' on \'//web/gui2-fw-lib:onos-gui2-fw-ng-build\'. \\\n" +
          "   See bazel-out/k8-fastbuild/bin/web/gui2-fw-lib/gui2-fw-ng-build-prod.log for more details' >&2;" +
          "   exit 1;" +
          " fi;" +
          # see https://github.com/angular/angular-cli/issues/11071 - Angular CLI does not yet copy across assets
          " unzip -q $$ROOT/$(location //web/gui:onos-gui-data-for-gui2) web/gui/src/main/webapp/data/img/loading/* &&" +
          " mkdir -p dist/gui2-fw-lib/assets &&" +
          " mv web/gui/src/main/webapp/data/img/loading dist/gui2-fw-lib/assets &&" +
          " cp README.md LICENSE dist/gui2-fw-lib &&" +
          " npm pack ./dist/gui2-fw-lib  >> $$ROOT/$(location gui2-fw-ng-build-prod.log) 2>&1 &&" +
          " mv gui2-fw-lib-*.tgz $$ROOT/$(location gui2-fw-lib-ver.tgz) &&" +
          " touch $$ROOT/$(location gui2-fw-ng-build-prod.log)",  # to get the log always as the 2nd file
    message = "GUI FW Lib build",
    visibility = ["//visibility:public"],
)

"""
    Run 'ng test' to run Angular test and 'ng lint' for checkstyle
    See bazel-out/k8-fastbuild/bin/web/gui2/onos-gui2-fw-ng-lint.log or
    bazel-out/k8-fastbuild/bin/web/gui2/onos-gui2-fw-ng-test.log for details of the Angular CLI output
"""

genrule(
    name = "_onos-gui2-fw-ng-test",
    srcs = [
        "@nodejs//:bin/npm",
        "@nodejs//:bin/node",
        "@nodejs//:bin/nodejs/bin/node",
        "@nodejs//:bin/nodejs/bin/npm",
        ":onos-gui2-fw-npm-install",
        "//web/gui:onos-gui-data-for-gui2",
        ":_root_level_files",
        ":_gui2_fw_lib_src",
        ":_gui2_fw_lib_tests",
        ":_e2e_test_files",
    ],
    outs = [
        "onos-gui2-fw-ng-ver.log",
        "onos-gui2-fw-ng-lint.log",
        "onos-gui2-fw-ng-test.log",
    ],
    cmd = " ROOT=`pwd` &&" +
          " export HOME=. &&" +
          " export XDG_CONFIG_HOME=$(@D)/config &&" +
          " NODE=$(location @nodejs//:bin/node) &&" +
          " INSTALL_FILES=($(locations :onos-gui2-fw-npm-install)) &&" +  # An array of filenames - sorted by time created
          " mkdir -p web/gui2-fw-lib &&" +
          " cd web/gui2-fw-lib &&" +
          " jar xf ../../$(location :_gui2_fw_lib_tests) &&" +
          " jar xf $$ROOT/$${INSTALL_FILES[0]} &&" +
          # next 2 lines needed for png file referenced from mast.component.css
          " unzip -q $$ROOT/$(location //web/gui:onos-gui-data-for-gui2) web/gui/src/main/webapp/data/img/dropdown-icon.png &&" +
          " mv web/gui/src/main/webapp/data projects/gui2-fw-lib/src/lib/mast/mast/ &&" +
          " chmod +x $$ROOT/web/gui2-fw-lib/node_modules/@angular/cli/bin/ng &&" +
          " export PATH=$$ROOT/$$(dirname $${NODE}):$$ROOT/web/gui2-fw-lib/node_modules/@angular/cli/bin:$$PATH &&" +
          " node -v > ../../$(location onos-gui2-fw-ng-ver.log) &&" +
          " npm -v >> ../../$(location onos-gui2-fw-ng-ver.log) &&" +
          " ng version >> ../../$(location onos-gui2-fw-ng-ver.log);" +
          " ng lint gui2-fw-lib > ../../$(location onos-gui2-fw-ng-lint.log) 2>&1 ||" +
          " if [ $$? -eq 0 ]; then echo 'Successfully ran lint';" +
          " else " +
          "   echo 'Error running \'ng lint\' on \'//web/gui2-fw-lib:_onos-gui2-fw-ng-test\'. \\\n" +
          "     See bazel-out/k8-fastbuild/bin/web/gui2-fw-lib/onos-gui2-fw-ng-lint.log for more details' >&2;" +
          "   cat ../../$(location onos-gui2-fw-ng-lint.log) >&2 ||" +
          "   exit 1;" +
          " fi;" +
          " if [ -f /usr/bin/chromium-browser ]; then " +  # Add to this for Mac and Chrome
          "   export CHROME_BIN=/usr/bin/chromium-browser; " +
          " elif [ -f /opt/google/chrome/chrome ]; then " +
          "   export CHROME_BIN=/opt/google/chrome/chrome; " +
          " else " +
          "   MSG='Warning: Step _onos-gui2-fw-ng-test skipped because \\n" +
          "   no binary for ChromeHeadless browser was found at /usr/bin/chromium-browser. \\n" +
          "   Install Google Chrome or Chromium Browser to allow this step to run.';" +
          "   echo -e $$MSG >&2;" +
          "   echo -e $$MSG > ../../$(location onos-gui2-fw-ng-test.log);" +
          "   exit 0;" +
          " fi;" +
          " ng test gui2-fw-lib --preserve-symlinks --code-coverage --browsers=ChromeHeadless" +
          "     --watch=false > ../../$(location onos-gui2-fw-ng-test.log) 2>&1 ||" +
          " if [ $$? -eq 0 ]; then echo 'Successfully ran tests';" +
          " else " +
          "   echo 'Error running \'ng test\' on \'//web/gui2-fw-lib:onos-gui2-fw-ng-test\'. \\\n" +
          "     See bazel-out/k8-fastbuild/bin/web/gui2-fw-lib/onos-gui2-fw-ng-test.log for more details' >&2;" +
          #"   tail -n 100 ../../$(location onos-gui2-fw-ng-test.log) >&2;" +
          "   exit 1;" +
          " fi;",
    message = "GUI FW lib lint and test",
)

"""
    Wrap the genrule for testing in a test
"""

sh_test(
    name = "onos-gui2-ng-tests",
    size = "small",
    srcs = [
        ":ng-test.sh",
    ],
    data = [
        ":_onos-gui2-fw-ng-test",
    ],
    deps = [
        "@bazel_tools//tools/bash/runfiles",
    ],
)
