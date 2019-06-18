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
    Rules to build the Fault Management (FM) GUI app for GUI 2


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
    Run ng build to create FM GUI 2 library in production mode
    The output file fm-gui2-lib-ver.tgz is in the form that can be uploaded directly to https://www.npmjs.com/
    See bazel-genfiles/apps/faultmanagement/fm-gui2-lib/fm-gui2-lib.log for details of the Angular CLI output
"""

genrule(
    name = "fm-gui2-lib-build",
    srcs = [
        "@nodejs//:bin/npm",
        "@nodejs//:bin/node",
        "@nodejs//:bin/nodejs/bin/node",
        "@nodejs//:bin/nodejs/bin/npm",
        "//web/gui2-fw-lib:onos-gui2-fw-npm-install",
        "//web/gui2-fw-lib:onos-gui2-fw-ng-build",
        "//web/gui2-fw-lib:gui2_fw_lib_ext_css",
        ":_root_level_files",
        ":_fm_gui2_lib_src",
        ":_fm-gui2_app_files",
    ],
    outs = [
        "fm-gui2-lib.log",
        "fm-gui2-lib-ver.tgz",
    ],
    cmd = "ROOT=`pwd` &&" +
          " export HOME=. &&" +
          " export XDG_CONFIG_HOME=$(@D)/config &&" +
          " NODE=$(location @nodejs//:bin/node) &&" +
          " INSTALL_FILES=($(locations //web/gui2-fw-lib:onos-gui2-fw-npm-install)) &&" +  # An array of filenames - sorted by time created
          " FWLIB_FILES=($(locations //web/gui2-fw-lib:onos-gui2-fw-ng-build)) &&" +  # An array of filenames - sorted by time created
          " mkdir -p apps/faultmanagement/fm-gui2-lib &&" +
          " cd apps/faultmanagement/fm-gui2-lib &&" +
          " jar xf $$ROOT/$${INSTALL_FILES[0]} &&" +
          " tar xf $$ROOT/$${FWLIB_FILES[0]} &&" +
          " mv package/ node_modules/gui2-fw-lib/ &&" +
          " mkdir -p projects/fm-gui2-lib/fw &&" +
          " (cd projects/fm-gui2-lib/fw &&" +
          "  jar xf $$ROOT/$(location //web/gui2-fw-lib:gui2_fw_lib_ext_css)) &&" +
          " chmod +x node_modules/@angular/cli/bin/ng &&" +
          " export PATH=$$ROOT/$$(dirname $${NODE}):$$ROOT/apps/faultmanagement/fm-gui2-lib/node_modules/@angular/cli/bin:$$PATH &&" +
          " ng build --prod fm-gui2-lib >> $$ROOT/$(location fm-gui2-lib.log) 2>&1 ||" +
          " if [ $$? -eq 0 ]; then echo 'Successfully ran build';" +
          " else " +
          "   echo 'Error running \'ng build fm-gui2-lib\' on \'//apps/faultmanagement/fm-gui2-lib:fm-gui2-lib-build\'. \\\n" +
          "     See bazel-genfiles/apps/faultmanagement/fm-gui2-lib/fm-gui2-lib.log for more details' >&2;" +
          #"   tail -n 100 ../../$(location onos-gui2-ng-test.log) >&2;" +
          "   exit 1;" +
          " fi;" +
          " cd dist/fm-gui2-lib && " +
          " npm pack >> $$ROOT/$(location fm-gui2-lib.log) 2>&1 &&" +
          " mv fm-gui2-lib-*.tgz $$ROOT/$(location fm-gui2-lib-ver.tgz) &&" +
          " touch $$ROOT/$(location fm-gui2-lib.log)",  # to get the log always as the 2nd file,
    message = "Angular FM GUI2 build",
    visibility = ["//visibility:public"],
)

"""
    Run 'ng test' to run Angular test and 'ng lint' for checkstyle
    See bazel-genfiles/apps/faultmanagement/fm-gui2-lib/fm-gui2-lib-lint.log or
    bazel-genfiles/apps/faultmanagement/fm-gui2-lib/fm-gui2-lib-test.log for details of the Angular CLI output
"""

genrule(
    name = "_fm-gui2-lib-test-genrule",
    srcs = [
        "@nodejs//:bin/npm",
        "@nodejs//:bin/node",
        "@nodejs//:bin/nodejs/bin/node",
        "@nodejs//:bin/nodejs/bin/npm",
        "//web/gui2-fw-lib:onos-gui2-fw-npm-install",
        "//web/gui2-fw-lib:onos-gui2-fw-ng-build",
        "//web/gui2-fw-lib:gui2_fw_lib_ext_css",
        ":_root_level_files",
        ":_fm_gui2_lib_src",
        ":_fm_gui2_lib_src_tests",
    ],
    outs = [
        "fm-gui2-lib-ver.log",
        "fm-gui2-lib-lint.log",
        "fm-gui2-lib-test.log",
    ],
    cmd = " ROOT=`pwd` &&" +
          " export HOME=. &&" +
          " export XDG_CONFIG_HOME=$(@D)/config &&" +
          " NODE=$(location @nodejs//:bin/node) &&" +
          " INSTALL_FILES=($(locations //web/gui2-fw-lib:onos-gui2-fw-npm-install)) &&" +  # An array of filenames - sorted by time created
          " FWLIB_FILES=($(locations //web/gui2-fw-lib:onos-gui2-fw-ng-build)) &&" +  # An array of filenames - sorted by time created
          " mkdir -p apps/faultmanagement/fm-gui2-lib &&" +
          " cd apps/faultmanagement/fm-gui2-lib &&" +
          " jar xf ../../../$(location :_fm_gui2_lib_src_tests) &&" +
          " jar xf $$ROOT/$${INSTALL_FILES[0]} &&" +
          " tar xf $$ROOT/$${FWLIB_FILES[0]} &&" +
          " mv package/ node_modules/gui2-fw-lib/ &&" +
          " mkdir -p projects/fm-gui2-lib/fw &&" +
          " (cd projects/fm-gui2-lib/fw &&" +
          "  jar xf $$ROOT/$(location //web/gui2-fw-lib:gui2_fw_lib_ext_css)) &&" +
          " chmod +x $$ROOT/apps/faultmanagement/fm-gui2-lib/node_modules/@angular/cli/bin/ng &&" +
          " export PATH=$$ROOT/$$(dirname $${NODE}):node_modules/@angular/cli/bin:$$PATH &&" +
          " node -v > ../../../$(location fm-gui2-lib-ver.log) &&" +
          " npm -v >> ../../../$(location fm-gui2-lib-ver.log) &&" +
          " ng -v >> ../../../$(location fm-gui2-lib-ver.log) &&" +
          " ng lint fm-gui2-lib > ../../../$(location fm-gui2-lib-lint.log);" +
          " if [ -f /usr/bin/chromium-browser ]; then " +  # Add to this for Mac and Chrome
          "   export CHROME_BIN=/usr/bin/chromium-browser; " +
          " elif [ -f /opt/google/chrome/chrome ]; then " +
          "   export CHROME_BIN=/opt/google/chrome/chrome; " +
          " else " +
          "   MSG='Warning: Step fm-gui2-lib-tests skipped because \\n" +
          "   no binary for ChromeHeadless browser was found at /usr/bin/chromium-browser. \\n" +
          "   Install Google Chrome or Chromium Browser to allow this step to run.';" +
          "   echo -e $$MSG >&2;" +
          "   echo -e $$MSG > ../../../$(location fm-gui2-lib-test.log);" +
          "   exit 0;" +
          " fi;" +
          " ng test fm-gui2-lib --preserve-symlinks --code-coverage --browsers=ChromeHeadless" +
          "     --watch=false > ../../../$(location fm-gui2-lib-test.log) 2>&1 ||" +
          " if [ $$? -eq 0 ]; then echo 'Successfully ran tests';" +
          " else " +
          "   echo 'Error running \'ng test fm-gui2-lib\' on \'//apps/faultmanagement/fm-gui2-lib:_fm-gui2-lib-test-genrule\'. \\\n" +
          "     See bazel-genfiles/apps/faultmanagement/fm-gui2-lib/fm-gui2-lib-test.log for more details' >&2;" +
          "   exit 1;" +
          " fi;",
    message = "Angular FM GUI2 Lib lint and test",
)

"""
    Make a group of all the webapp files.
"""

filegroup(
    name = "_fm_gui2_lib_src",
    srcs = glob(
        [
            "projects/fm-gui2-lib/**/*",
        ],
        exclude = [
            "projects/fm-gui2-lib/**/*.spec.*",  # Don't track tests here
            "projects/fm-gui2-lib/karma.conf.js",
            "projects/fm-gui2-lib/src/test.ts",
            "projects/fm-gui2-lib/fw/**/*",
        ],
    ),
)

"""
    Make a group of all the webapp qpp files.
"""

filegroup(
    name = "_fm-gui2_app_files",
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
    name = "_fm_gui2_lib_src_tests",
    srcs = glob(
        [
            "projects/fm-gui2-lib/karma.conf.js",
            "projects/fm-gui2-lib/src/test.ts",
            "projects/fm-gui2-lib/tsconfig.spec.json",
            "projects/fm-gui2-lib/**/*.spec.ts",
        ],
        exclude = [
            "projects/fm-gui2-lib/ng-package.json",
            "projects/fm-gui2-lib/ng-package.prod.json",
            "projects/fm-gui2-lib/package.json",
            "projects/fm-gui2-lib/tsconfig.lib.json",
            "projects/fm-gui2-lib/tslint.json",
            "projects/fm-gui2-lib/src/public_api.ts",
        ],
    ),
    outs = ["fm_gui2_lib_src_tests.jar"],
    cmd = "cd apps/faultmanagement/fm-gui2-lib &&" +
          " jar Mcf ../../../$@ .",
)

"""
    Make a jar file of all the webapp test (*.spec.ts) files.
"""

genrule(
    name = "_fm_gui2_lib_tests",
    srcs = glob(
        [
            "projects/fm-gui2-lib/**/*.spec.ts",
            "projects/fm-gui2-lib/**/*.spec.json",
        ],
        exclude = [
            "src/main/webapp/tests/**",
            "src/main/webapp/node_modules/**",
            "src/main/webapp/dist/**",
            "src/main/webapp/doc/**",
        ],
    ),
    outs = ["fm_gui2_lib_tests.jar"],
    cmd = "cd apps/faultmanagement/fm-gui2-lib &&" +
          " find projects/fm-gui2-lib/src/lib -type f -exec touch -t 201808280000 {} \; &&" +
          " jar Mcf ../../../$@ projects/fm-gui2-lib/src/lib",
)

"""
    Wrap the genrule for testing in a test
"""

sh_test(
    name = "fm-gui2-lib-tests",
    size = "small",
    srcs = [
        ":ng-test.sh",
    ],
    data = [
        ":_fm-gui2-lib-test-genrule",
    ],
    deps = [
        "@bazel_tools//tools/bash/runfiles",
    ],
)
