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
    Rules to build the ONOS GUI 2

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

COMPILE_DEPS = CORE_DEPS + JACKSON + KRYO + CLI + [
    "@javax_ws_rs_api//jar",
    "@servlet_api//jar",
    "@jetty_websocket//jar",
    "@jetty_websocket_api//jar",
    "@jetty_util//jar",
    "@jersey_media_multipart//jar",
    "@jersey_server//jar",
    "@jersey_hk2//jar",
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
            ":karma.conf.js",
            ":package.json",
            ":package-lock.json",
            ":protractor.conf.js",
            ":src/main/tsconfig.json",
            ":src/main/tslint.json",
            ":tsconfig.json",
        ],
)

filegroup(
    name = "_e2e_test_files",
    srcs = [
        ":e2e/app.e2e-spec.ts",
        ":e2e/app.po.ts",
        ":e2e/tsconfig.e2e.json",
    ],
)

"""
    Files that get put into the WEB-INF directory of the tar ball
"""

filegroup(
    name = "_web_inf_classes_files",
    srcs =
        [
            ":src/main/webapp/error.html",
            ":src/main/webapp/login.html",
            ":src/main/webapp/nav.html",
            ":src/main/webapp/not-ready.html",
            ":src/main/webapp/onos.global.css",
        ],
)

"""
    Run ng build to create outputs in production mode
    See bazel-genfiles/web/gui2/onos-gui2-ng-build-prod.log for details of the Angular CLI output

    To avoid the overhead of having several "npm install" invocations, we just do
    it once in the //web/gui2-fw-lib which is really the core for the whole Angular 7
    structure in ONOS. This copies files in to node_modules, but because the gui2-fw-lib
    has not been generated at that time we copy it in separately below with the 'tar' cmd
    and then 'mv'
"""

genrule(
    name = "_onos-gui2-ng-build",
    srcs = [
        "@nodejs//:bin/npm",
        "@nodejs//:bin/node",
        "@nodejs//:bin/nodejs/bin/node",
        "@nodejs//:bin/nodejs/bin/npm",
        "//web/gui2-fw-lib:onos-gui2-fw-npm-install",
        "//web/gui2-fw-lib:onos-gui2-fw-ng-build",
        "//web/gui2-fw-lib:gui2_fw_lib_ext_css",
        "//web/gui2-topo-lib:gui2-topo-lib-build",
        "//apps/faultmanagement/fm-gui2-lib:fm-gui2-lib-build",
        ":_root_level_files",
        ":_web_app_all",
        "//apps/roadm/web/roadm-gui:roadm-gui-lib-build",
    ],
    outs = [
        "onos-gui2-ng-build-prod.log",
        "onos-gui2-ng-build.jar",
    ],
    cmd = "ROOT=`pwd` &&" +
          " export HOME=. &&" +
          " export XDG_CONFIG_HOME=$(@D)/config &&" +
          " NODE=$(location @nodejs//:bin/node) &&" +
          " INSTALL_FILES=($(locations //web/gui2-fw-lib:onos-gui2-fw-npm-install)) &&" +  # An array of filenames - sorted by time created
          " FWLIB_FILES=($(locations //web/gui2-fw-lib:onos-gui2-fw-ng-build)) &&" +  # An array of filenames - sorted by time created
          " mkdir -p web/gui2 && cd web/gui2 &&" +
          " jar xf ../../$(location :_web_app_all) &&" +
          " jar xf $$ROOT/$${INSTALL_FILES[0]} &&" +
          " tar xf $$ROOT/$${FWLIB_FILES[0]} &&" +
          " mv package/ node_modules/gui2-fw-lib/ &&" +
          # Add in modules from external packages
          " GUI2_TOPO_LIB_FILES=($(locations //web/gui2-topo-lib:gui2-topo-lib-build)) &&" +  # An array of filenames - sorted by time created
          " tar xf $$ROOT/$${GUI2_TOPO_LIB_FILES[0]} &&" +
          " mv package/ node_modules/gui2-topo-lib/ &&" +
          " FM_GUI2_LIB_FILES=($(locations //apps/faultmanagement/fm-gui2-lib:fm-gui2-lib-build)) &&" +  # An array of filenames - sorted by time created
          " tar xf $$ROOT/$${FM_GUI2_LIB_FILES[0]} &&" +
          " mv package/ node_modules/fm-gui2-lib/ &&" +
          " ROADM_GUI_LIB_FILES=($(locations //apps/roadm/web/roadm-gui:roadm-gui-lib-build)) &&" +  # An array of filenames - sorted by time created
          " tar xf $$ROOT/$${ROADM_GUI_LIB_FILES[0]} &&" +
          " mv package/ node_modules/roadm-gui-lib/ &&" +
          # End of add in modules from external packages
          " mkdir -p src/main/webapp/app/fw &&" +
          " (cd src/main/webapp/app/fw &&" +
          "  jar xf $$ROOT/$(location //web/gui2-fw-lib:gui2_fw_lib_ext_css)) &&" +
          " chmod +x $$ROOT/web/gui2/node_modules/@angular/cli/bin/ng &&" +
          " export PATH=$$ROOT/$$(dirname $${NODE}):$$ROOT/web/gui2/node_modules/@angular/cli/bin:$$PATH &&" +
          " node -v > ../../$(location onos-gui2-ng-build-prod.log) &&" +
          " npm -v >> ../../$(location onos-gui2-ng-build-prod.log) &&" +
          " ng version >> ../../$(location onos-gui2-ng-build-prod.log) &&" +
          " ng build --extract-css --prod --preserve-symlinks" +
          "   --base-href /onos/ui/ --deploy-url /onos/ui/ >> $$ROOT/$(location onos-gui2-ng-build-prod.log) 2>&1 ||" +
          " if [ $$? -eq 0 ]; then echo 'Successfully ran build';" +
          " else " +
          "   echo 'Error running \'ng build\' on \'//web/gui2:_onos-gui2-ng-build\'. \\\n" +
          "     See bazel-genfiles/web/gui2/onos-gui2-ng-build-prod.log for more details' >&2;" +
          #"   tail -n 100 ../../$(location onos-gui2-ng-test.log) >&2;" +
          "   exit 1;" +
          " fi;" +
          " cp -r node_modules/gui2-fw-lib/assets src/main/webapp/dist &&" +
          " cd src/main/webapp/dist &&" +
          " jar Mcf $$ROOT/$(location onos-gui2-ng-build.jar) .",
    message = "Angular CLI 7 build",
)

"""
    Run 'ng test' to run Angular test and 'ng lint' for checkstyle
    See bazel-genfiles/web/gui2/onos-gui2-ng-lint.log or
    bazel-genfiles/web/gui2/onos-gui2-ng-test.log for details of the Angular CLI output
"""

genrule(
    name = "_onos-gui2-ng-test-genrule",
    srcs = [
        "@nodejs//:bin/npm",
        "@nodejs//:bin/node",
        "@nodejs//:bin/nodejs/bin/node",
        "@nodejs//:bin/nodejs/bin/npm",
        "//web/gui2-fw-lib:onos-gui2-fw-npm-install",
        "//web/gui2-fw-lib:onos-gui2-fw-ng-build",
        "//web/gui2-fw-lib:gui2_fw_lib_ext_css",
        ":_web_app_all",
        ":_web_app_tests",
        ":_angular_all",
    ],
    outs = [
        "onos-gui2-ng-ver.log",
        "onos-gui2-ng-lint.log",
        "onos-gui2-ng-test.log",
    ],
    cmd = " ROOT=`pwd` &&" +
          " export HOME=. &&" +
          " export XDG_CONFIG_HOME=$(@D)/config &&" +
          " NODE=$(location @nodejs//:bin/node) &&" +
          " INSTALL_FILES=($(locations //web/gui2-fw-lib:onos-gui2-fw-npm-install)) &&" +  # An array of filenames - sorted by time created
          " FWLIB_FILES=($(locations //web/gui2-fw-lib:onos-gui2-fw-ng-build)) &&" +  # An array of filenames - sorted by time created
          " mkdir -p web/gui2 &&" +
          " cd web/gui2 &&" +
          " jar xf ../../$(location :_angular_all) &&" +
          " jar xf ../../$(location :_web_app_all) &&" +
          " jar xf ../../$(location :_web_app_tests) &&" +
          " jar xf $$ROOT/$${INSTALL_FILES[0]} &&" +
          " tar xf $$ROOT/$${FWLIB_FILES[0]} &&" +
          " mv package/ node_modules/gui2-fw-lib/ &&" +
          " mkdir -p src/main/webapp/app/fw &&" +
          " (cd src/main/webapp/app/fw &&" +
          "  jar xf $$ROOT/$(location //web/gui2-fw-lib:gui2_fw_lib_ext_css)) &&" +
          " chmod +x $$ROOT/web/gui2/node_modules/@angular/cli/bin/ng &&" +
          " export PATH=$$ROOT/$$(dirname $${NODE}):$$ROOT/web/gui2/node_modules/@angular/cli/bin:$$PATH &&" +
          " node -v > ../../$(location onos-gui2-ng-ver.log) &&" +
          " npm -v >> ../../$(location onos-gui2-ng-ver.log) &&" +
          " ng version >> ../../$(location onos-gui2-ng-ver.log);" +
          " ng lint > ../../$(location onos-gui2-ng-lint.log) 2>&1 ||" +
          " if [ $$? -eq 0 ]; then echo 'Successfully ran lint';" +
          " else " +
          "   echo 'Error running \'ng lint\' on \'//web/gui2:onos-gui2-ng-test\'. \\\n" +
          "     See bazel-genfiles/web/gui2/onos-gui2-ng-lint.log for more details' >&2;" +
          "   exit 1;" +
          " fi;" +
          " if [ -f /usr/bin/chromium-browser ]; then " +  # Add to this for Mac and Chrome
          "   export CHROME_BIN=/usr/bin/chromium-browser; " +
          " elif [ -f /opt/google/chrome/chrome ]; then " +
          "   export CHROME_BIN=/opt/google/chrome/chrome; " +
          " else " +
          "   MSG='Warning: Step onos-gui2-ng-test skipped because \\n" +
          "   no binary for ChromeHeadless browser was found at /usr/bin/chromium-browser. \\n" +
          "   Install Google Chrome or Chromium Browser to allow this step to run.';" +
          "   echo -e $$MSG >&2;" +
          "   echo -e $$MSG > ../../$(location onos-gui2-ng-test.log);" +
          "   exit 0;" +
          " fi;" +
          " ng test --preserve-symlinks --code-coverage --browsers=ChromeHeadless" +
          "     --watch=false > ../../$(location onos-gui2-ng-test.log) 2>&1 ||" +
          " if [ $$? -eq 0 ]; then echo 'Successfully ran tests';" +
          " else " +
          "   echo 'Error running \'ng test\' on \'//web/gui2:onos-gui2-ng-test\'. \\\n" +
          "     See bazel-genfiles/web/gui2/onos-gui2-ng-test.log for more details' >&2;" +
          #"   tail -n 100 ../../$(location onos-gui2-ng-test.log) >&2;" +
          "   exit 1;" +
          " fi;",
    message = "Angular CLI 7 lint and test",
)

"""
    Make a jar file of all the webapp files. Useful for breaking symblic links in the sandbox
"""

genrule(
    name = "_web_app_all",
    srcs = glob(
        [
            "src/main/webapp/**",
        ],
        exclude = [
            "src/main/webapp/**/*.spec.ts",  # Don't track tests here
            "src/main/webapp/tests/**",
            "src/main/webapp/node_modules/**",
            "src/main/webapp/dist/**",
            "src/main/webapp/doc/**",
            "src/main/webapp/app/fw/**",
        ],
    ),
    outs = ["web_app_all.jar"],
    cmd = "cd web/gui2 &&" +
          " find src/main/webapp -type f -exec touch -t 201808280000 {} \; &&" +
          " jar Mcf ../../$@ src/main/webapp",
)

"""
    Make a jar file of all the webapp test (*.spec.ts) files.
"""

genrule(
    name = "_web_app_tests",
    srcs = glob(
        [
            "src/main/webapp/**/*.spec.ts",
        ],
        exclude = [
            "src/main/webapp/tests/**",
            "src/main/webapp/node_modules/**",
            "src/main/webapp/dist/**",
            "src/main/webapp/doc/**",
        ],
    ),
    outs = ["web_app_tests.jar"],
    cmd = "cd web/gui2 &&" +
          " find src/main/webapp -type f -exec touch -t 201808280000 {} \; &&" +
          " jar Mcf ../../$@ src/main/webapp",
)

"""
    Make a jar file of all the supporting files. Useful for breaking symblic links in the sandbox
"""

genrule(
    name = "_angular_all",
    srcs = [
        ":_e2e_test_files",
        ":_root_level_files",
    ],
    outs = ["angular_all.jar"],
    cmd = " cd web/gui2 && jar Mcf ../../$@ .",
)

"""
    Builds the java jar for the java code provided by the GUI2
"""

osgi_jar_with_tests(
    name = "_onos-gui2-base-jar",
    srcs =
        glob([
            "src/main/java/**",
        ]) + [
            "//web/gui:onos-gui-java-for-gui2",
        ],
    exclude_tests = [
        "org.onosproject.ui.impl.AbstractUiImplTest",
        "org.onosproject.ui.impl.topo.model.AbstractTopoModelTest",
    ],
    karaf_command_packages = [
        "org.onosproject.ui.impl.cli",
        "org.onosproject.ui.impl.topo",
    ],
    suppress_checkstyle = True,
    test_deps = TEST_DEPS,
    web_context = "/onos/ui",
    deps = COMPILE_DEPS,
)

"""
    Builds the tar ball for the ONOS GUI2
"""

genrule(
    name = "onos-web-gui2",
    srcs = [
        ":_onos-gui2-ng-build",
        ":_onos-gui2-base-jar",
        ":_web_inf_classes_files",
        "//web/gui:onos-gui-lion-for-gui2",
        "//web/gui:onos-gui-data-for-gui2",
        "src/main/webapp/WEB-INF/web.xml",
    ],
    outs = ["onos-gui2.jar"],
    cmd = " ROOT=`pwd` &&" +
          " mkdir -p web/gui2/WEB-INF/classes &&" +
          " cd web/gui2 &&" +
          " BUILD_FILES=($(locations :_onos-gui2-ng-build)) &&" +  # An array of filenames - sorted by time created
          " for i in $(locations :_web_inf_classes_files); do cp $$ROOT/$$i ./WEB-INF/classes/; done &&" +
          " (cd WEB-INF/classes && jar xf $$ROOT/$${BUILD_FILES[1]}) &&" +
          " jar xf $$ROOT/$(location :_onos-gui2-base-jar) &&" +
          " unzip -q $$ROOT/$(location //web/gui:onos-gui-lion-for-gui2) web/gui/src/main/resources/**/* &&" +
          " mv web/gui/src/main/resources/org/onosproject/ui/lion* WEB-INF/classes/org/onosproject/ui/ &&" +
          " mv web/gui/src/main/resources/core WEB-INF/classes/ &&" +
          " unzip -q $$ROOT/$(location //web/gui:onos-gui-data-for-gui2) web/gui/src/main/webapp/data/**/* &&" +
          " mv web/gui/src/main/webapp/data WEB-INF/classes/ &&" +
          " find . -type f -exec touch -t 201903200000 {} \; &&" +
          " jar cmf META-INF/MANIFEST.MF $$ROOT/$@ WEB-INF/web.xml WEB-INF/classes OSGI-INF/*.xml",
    output_to_bindir = 1,
    visibility = ["//visibility:public"],
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
        ":_onos-gui2-ng-test-genrule",
    ],
    deps = [
        "@bazel_tools//tools/bash/runfiles",
    ],
)

onos_app(
    category = "Graphical User Interface",
    description = "ONOS GUI2 - a reengineered version of the original ONOS GUI " +
                  "based on the latest Angular framework components",
    title = "ONOS GUI2",
    url = "http://onosproject.org",
)
