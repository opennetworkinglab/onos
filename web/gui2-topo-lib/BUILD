"""
 Copyright 2019-present Open Networking Foundation

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
    Rules to build the Topology app for GUI 2


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
    Run ng build to create Topo GUI 2 library in production mode
    The output file gui2-topo-lib-ver.tgz is in the form that can be uploaded directly to https://www.npmjs.com/
    See bazel-out/k8-fastbuild/bin/web/gui2-topo-lib/gui2-topo-lib.log for details of the Angular CLI output
"""

genrule(
    name = "gui2-topo-lib-build",
    srcs = [
        "@nodejs//:bin/npm",
        "@nodejs//:bin/node",
        "@nodejs//:bin/nodejs/bin/node",
        "@nodejs//:bin/nodejs/bin/npm",
        "//web/gui2-fw-lib:onos-gui2-fw-npm-install",
        "//web/gui2-fw-lib:onos-gui2-fw-ng-build",
        "//web/gui2-fw-lib:gui2_fw_lib_ext_css",
        ":_root_level_files",
        ":_gui2_topo_lib_src",
        ":_gui2_topo_tester_files",
    ],
    outs = [
        "gui2-topo-lib.log",
        "gui2-topo-lib-ver.tgz",
    ],
    cmd = "ROOT=`pwd` &&" +
          " export HOME=. &&" +
          " export XDG_CONFIG_HOME=$(@D)/config &&" +
          " NODE=$(location @nodejs//:bin/node) &&" +
          " INSTALL_FILES=($(locations //web/gui2-fw-lib:onos-gui2-fw-npm-install)) &&" +  # An array of filenames - sorted by time created
          " TOPOLIB_FILES=($(locations //web/gui2-fw-lib:onos-gui2-fw-ng-build)) &&" +  # An array of filenames - sorted by time created
          " mkdir -p web/gui2-topo-lib &&" +
          " cd web/gui2-topo-lib &&" +
          " jar xf $$ROOT/$${INSTALL_FILES[0]} &&" +
          " tar xf $$ROOT/$${TOPOLIB_FILES[0]} &&" +
          " mv package/ node_modules/gui2-fw-lib/ &&" +
          " mkdir -p projects/gui2-topo-lib/fw &&" +
          " (cd projects/gui2-topo-lib/fw &&" +
          "  jar xf $$ROOT/$(location //web/gui2-fw-lib:gui2_fw_lib_ext_css)) &&" +
          " chmod +x node_modules/@angular/cli/bin/ng &&" +
          " export PATH=$$ROOT/$$(dirname $${NODE}):$$ROOT/web/gui2-topo-lib/node_modules/@angular/cli/bin:$$PATH &&" +
          " ng build gui2-topo-lib >> $$ROOT/$(location gui2-topo-lib.log) 2>&1 ||" +
          " if [ $$? -eq 0 ]; then echo 'Successfully ran build';" +
          " else " +
          "   echo 'Error running \'ng build gui2-topo-lib\' on \'//web/gui2-topo-lib:gui2-topo-lib-build\'. \\\n" +
          "     See bazel-out/k8-fastbuild/bin/web/gui2-topo-lib/gui2-topo-lib.log for more details' >&2;" +
          #"   tail -n 100 ../../$(location onos-gui2-ng-test.log) >&2;" +
          "   exit 1;" +
          " fi;" +
          " cp README.md LICENSE dist/gui2-topo-lib && " +
          " npm pack ./dist/gui2-topo-lib >> $$ROOT/$(location gui2-topo-lib.log) 2>&1 &&" +
          " mv gui2-topo-lib-*.tgz $$ROOT/$(location gui2-topo-lib-ver.tgz) &&" +
          " touch $$ROOT/$(location gui2-topo-lib.log)",  # to get the log always as the 2nd file,
    message = "Angular Topo GUI2 build",
    visibility = ["//visibility:public"],
)

"""
    Run 'ng test' to run Angular test and 'ng lint' for checkstyle
    See bazel-out/k8-fastbuild/bin/web/gui2-topo-lib/gui2-topo-lib-lint.log or
    bazel-out/k8-fastbuild/bin/web/gui2-topo-lib/gui2-topo-lib-test.log for details of the Angular CLI output
"""

genrule(
    name = "_gui2-topo-lib-test-genrule",
    srcs = [
        "@nodejs//:bin/npm",
        "@nodejs//:bin/node",
        "@nodejs//:bin/nodejs/bin/node",
        "@nodejs//:bin/nodejs/bin/npm",
        "//web/gui2-fw-lib:onos-gui2-fw-npm-install",
        "//web/gui2-fw-lib:onos-gui2-fw-ng-build",
        "//web/gui2-fw-lib:gui2_fw_lib_ext_css",
        ":_root_level_files",
        ":_gui2_topo_lib_src",
        ":_gui2_topo_lib_src_tests",
    ],
    outs = [
        "gui2-topo-lib-ver.log",
        "gui2-topo-lib-lint.log",
        "gui2-topo-lib-test.log",
    ],
    cmd = " ROOT=`pwd` &&" +
          " export HOME=. &&" +
          " export XDG_CONFIG_HOME=$(@D)/config &&" +
          " NODE=$(location @nodejs//:bin/node) &&" +
          " INSTALL_FILES=($(locations //web/gui2-fw-lib:onos-gui2-fw-npm-install)) &&" +  # An array of filenames - sorted by time created
          " FWLIB_FILES=($(locations //web/gui2-fw-lib:onos-gui2-fw-ng-build)) &&" +  # An array of filenames - sorted by time created
          " mkdir -p web/gui2-topo-lib &&" +
          " cd web/gui2-topo-lib &&" +
          " jar xf ../../$(location :_gui2_topo_lib_src_tests) &&" +
          " jar xf $$ROOT/$${INSTALL_FILES[0]} &&" +
          " tar xf $$ROOT/$${FWLIB_FILES[0]} &&" +
          " mv package/ node_modules/gui2-fw-lib/ &&" +
          " mkdir -p projects/gui2-topo-lib/fw &&" +
          " (cd projects/gui2-topo-lib/fw &&" +
          "  jar xf $$ROOT/$(location //web/gui2-fw-lib:gui2_fw_lib_ext_css)) &&" +
          " chmod +x $$ROOT/web/gui2-topo-lib/node_modules/@angular/cli/bin/ng &&" +
          " export PATH=$$ROOT/$$(dirname $${NODE}):node_modules/@angular/cli/bin:$$PATH &&" +
          " node -v > ../../$(location gui2-topo-lib-ver.log) &&" +
          " npm -v >> ../../$(location gui2-topo-lib-ver.log) &&" +
          " ng version >> ../../$(location gui2-topo-lib-ver.log) &&" +
          " ng lint gui2-topo-lib > ../../$(location gui2-topo-lib-lint.log)  2>&1 ||" +
          " if [ $$? -eq 0 ]; then echo 'Successfully ran lint';" +
          " else " +
          "   echo 'Error running \'ng lint\' on \'//web/gui2-topo-lib:_gui2-topo-lib-test-genrule\'. \\\n" +
          "     See bazel-out/k8-fastbuild/bin/web/gui2-topo-lib/gui2-topo-lib-lint.log for more details' >&2;" +
          "   cat ../../$(location gui2-topo-lib-lint.log) >&2 ||" +
          "   exit 1;" +
          " fi;" +
          " if [ -f /usr/bin/chromium-browser ]; then " +  # Add to this for Mac and Chrome
          "   export CHROME_BIN=/usr/bin/chromium-browser; " +
          " elif [ -f /opt/google/chrome/chrome ]; then " +
          "   export CHROME_BIN=/opt/google/chrome/chrome; " +
          " else " +
          "   MSG='Warning: Step gui2-topo-lib-tests skipped because \\n" +
          "   no binary for ChromeHeadless browser was found at /usr/bin/chromium-browser. \\n" +
          "   Install Google Chrome or Chromium Browser to allow this step to run.';" +
          "   echo -e $$MSG >&2;" +
          "   echo -e $$MSG > ../../$(location gui2-topo-lib-test.log);" +
          "   exit 0;" +
          " fi;" +
          " ng test gui2-topo-lib --preserve-symlinks --code-coverage --browsers=ChromeHeadless" +
          "     --watch=false > ../../$(location gui2-topo-lib-test.log) 2>&1 ||" +
          " if [ $$? -eq 0 ]; then echo 'Successfully ran tests';" +
          " else " +
          "   echo 'Error running \'ng test gui2-topo-lib\' on \'//web/gui2-topo-lib:_gui2-topo-lib-test-genrule\'. \\\n" +
          "     See bazel-out/k8-fastbuild/bin/web/gui2-topo-lib/gui2-topo-lib-test.log for more details' >&2;" +
          "   exit 1;" +
          " fi;",
    message = "Angular Topo GUI2 Lib lint and test",
)

"""
    Make a group of all the webapp files.
"""

filegroup(
    name = "_gui2_topo_lib_src",
    srcs = glob(
        [
            "projects/gui2-topo-lib/**/*",
            "README.md",
            "LICENSE",
        ],
        exclude = [
            "projects/gui2-topo-lib/**/*.spec.*",  # Don't track tests here
            "projects/gui2-topo-lib/karma.conf.js",
            "projects/gui2-topo-lib/src/test.ts",
            "projects/gui2-topo-lib/fw/**/*",
        ],
    ),
)

"""
    Make a group of all the webapp qpp files.
"""

filegroup(
    name = "_gui2_topo_tester_files",
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
    name = "_gui2_topo_lib_src_tests",
    srcs = glob(
        [
            "projects/gui2-topo-lib/karma.conf.js",
            "projects/gui2-topo-lib/src/test.ts",
            "projects/gui2-topo-lib/tsconfig.spec.json",
            "projects/gui2-topo-lib/**/*.spec.ts",
        ],
        exclude = [
            "projects/gui2-topo-lib/ng-package.json",
            "projects/gui2-topo-lib/ng-package.prod.json",
            "projects/gui2-topo-lib/package.json",
            "projects/gui2-topo-lib/tsconfig.lib.json",
            "projects/gui2-topo-lib/tslint.json",
            "projects/gui2-topo-lib/src/public_api.ts",
        ],
    ),
    outs = ["gui2_topo_lib_src_tests.jar"],
    cmd = "cd web/gui2-topo-lib &&" +
          " jar Mcf ../../$@ .",
)

"""
    Make a jar file of all the webapp test (*.spec.ts) files.
"""

genrule(
    name = "_gui2_topo_lib_tests",
    srcs = glob(
        [
            "projects/gui2-topo-lib/**/*.spec.ts",
            "projects/gui2-topo-lib/**/*.spec.json",
        ],
        exclude = [
            "src/main/webapp/tests/**",
            "src/main/webapp/node_modules/**",
            "src/main/webapp/dist/**",
            "src/main/webapp/doc/**",
        ],
    ),
    outs = ["gui2_topo_lib_tests.jar"],
    cmd = "cd web/gui2-topo-lib &&" +
          " find projects/gui2-topo-lib/src/lib -type f -exec touch -t 201808280000 {} \; &&" +
          " jar Mcf ../../$@ projects/gui2-topo-lib/src/lib",
)

"""
    Wrap the genrule for testing in a test
"""

sh_test(
    name = "gui2-topo-lib-tests",
    size = "small",
    srcs = [
        ":ng-test.sh",
    ],
    data = [
        ":_gui2-topo-lib-test-genrule",
    ],
    deps = [
        "@bazel_tools//tools/bash/runfiles",
    ],
)
