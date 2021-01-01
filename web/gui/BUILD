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
    Rules to build the ONOS GUI

    Bazel and npm are incompatibe in how they deal with files. npm likes to follow links
    to get back to the original canonical path names, and bazel uses links extensively when
    populating the sandbox. To get around these problems, the rules that follow use filegroups
    to specify the files as dependencies and then use a genrule to convert the files into a tar
    ball. Once the tar ball is unrolled into the sandbox, the links are broken, but the build is
    still hermetic since those files are referred to as dependencies in the genrule.

    Also note that the onos-gui.tar contains files from //tools/gui and //web/gui, so the files are placed into
    the sandbox at the proper locations and then returned as a tar ball.
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

exports_files(["src/main/webapp/data"])

"""
    Files that get put at the top level of the tar ball
"""

filegroup(
    name = "_root_level_files",
    srcs =
        [
            ":src/main/webapp/bower.json",
            ":src/main/webapp/bs-config.js",
            ":src/main/webapp/dev_server.js",
            ":src/main/webapp/package.json",
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
            ":src/main/webapp/index.html",
            ":src/main/webapp/login.html",
            ":src/main/webapp/nav.html",
            ":src/main/webapp/not-ready.html",
            ":src/main/webapp/onos.js",
        ],
)

"""
    webapp raw files
"""

filegroup(
    name = "_raw_classes_files",
    srcs = glob(["src/main/webapp/raw/**"]),
)

"""
    Install node.js and npm, and gather files needed from //tools/gui
"""

genrule(
    name = "_onos-gui-npm-install",
    srcs = [
        "@nodejs//:npm",
        "@gui1_npm//:node_modules",
        "//tools/gui:tools-gui-gulp",
    ],
    outs = [
        "onos-gui-npm-install.jar",
        "onos-gui1-npm-install.log",
    ],
    cmd = " ROOT=`pwd` &&" +
          " export HOME=. &&" +
          " export XDG_CONFIG_HOME=$(@D)/config &&" +  # npm config cache to the sandbos
          " export BABEL_DISABLE_CACHE=1 &&" +  # turn off babel cache
          ' if [[ ! -z $${HTTP_PROXY-} ]]; then NPM_ARGS="--proxy $$HTTP_PROXY --without-ssl --insecure"; else NPM_ARGS=""; fi &&' +
          " NPM=$$ROOT/$(location @nodejs//:npm) &&" +
          " mkdir -p tools/gui &&" +
          " cd tools/gui &&" +
          " jar xf ../../$(location //tools/gui:tools-gui-gulp) &&" +
          " $$NPM $$NPM_ARGS install  --no-cache --loglevel=error > $$ROOT/$(location onos-gui1-npm-install.log) 2>&1 &&" +
          " find . -name package.json | while read pjson; do egrep -v '/execroot/' $$pjson > ptmp; mv ptmp $$pjson; done &&" +
          " find package.json gulpfile.babel.js node_modules gulp-tasks -type f -exec touch -t 202101010000 {} \\; &&" +
          " jar Mcf $$ROOT/$(location onos-gui-npm-install.jar) package.json gulpfile.babel.js node_modules gulp-tasks &&" +
          " touch $$ROOT/$(location onos-gui1-npm-install.log)",  # to get the log always as the 2nd file
)

"""
    Run npm build to create node.js files
"""

genrule(
    name = "_onos-gui-npm-build",
    srcs = [
        "@nodejs//:npm",
        ":_onos-gui-npm-install",
        ":_web_app_all",
    ],
    outs = [
        "onos-gui-npm-build.jar",
        "onos-gui1-npm-build.log",
    ],
    cmd = "ROOT=`pwd` &&" +
          " export HOME=. &&" +
          " export XDG_CONFIG_HOME=$(@D)/config &&" +
          " export BABEL_DISABLE_CACHE=1 &&" +
          " INSTALL_FILES=($(locations :_onos-gui-npm-install)) &&" +  # An array of filenames - sorted by time created
          ' if [[ ! -z $${HTTP_PROXY-} ]]; then NPM_ARGS="--proxy $$HTTP_PROXY --without-ssl --insecure"; else NPM_ARGS=""; fi &&' +
          " NPM=$(location @nodejs//:npm) &&" +
          " (mkdir -p web/gui && cd web/gui && jar xf ../../$(location :_web_app_all)) &&" +
          " mkdir -p tools/gui && cd tools/gui &&" +
          " jar xf $$ROOT/$${INSTALL_FILES[0]} &&" +
          " chmod a+x ./node_modules/gulp/bin/gulp.js &&" +
          " $$ROOT/$$NPM $$NPM_ARGS run build --no-cache --loglevel=error > $$ROOT/$(location onos-gui1-npm-build.log) &&" +
          " cd ../../web/gui/src/main/webapp &&" +
          " find dist vendor data README.md _doc _dev app/fw app/*.css app/*.js app/*.txt -type f -exec touch -t 201806280000 {} \\; &&" +
          " jar Mcf $$ROOT/$(location onos-gui-npm-build.jar) dist vendor data README.md _doc _dev app/fw app/*.css app/*.js app/*.txt &&" +
          " touch $$ROOT/$(location onos-gui1-npm-build.log)",  # to get the log always as the 2nd file
)

"""
    Make a jar file of all the webapp files. Useful for breaking symblic links in the sandbox
"""

genrule(
    name = "_web_app_all",
    srcs = glob(
        [
            "src/main/webapp/**",
            "src/main/webapp/app/**/*.js",
        ],
        exclude = [
            "src/main/webapp/tests/**",
            "src/main/webapp/node_modules/**",
            "src/main/webapp/dist/**",
            "src/main/webapp/vendor/**",
            "src/main/webapp/npm-debug.log",
        ],
    ),
    outs = ["web_app_all.jar"],
    cmd = "cd web/gui &&" +
          " find src/main/webapp -type f -exec touch -t 201806280000 {} \\; &&" +
          " jar Mcf ../../$@ src/main/webapp",
)

"""
    app/view is packaged as a tar file because it has subdirectories that need to be preserved
"""

genrule(
    name = "_app_view_tar",
    srcs = glob(["src/main/webapp/app/view/**"]),
    outs = ["app_view_tar.jar"],
    cmd = " ROOT=`pwd` &&" +
          " cd web/gui/src/main/webapp/app/view &&" +
          " find . -type f -exec touch -t 201806280000 {} \\; &&" +
          " jar Mcf $$ROOT/$@ .",
)

"""
    Builds the java jar for the java code provided by the GUI
"""

osgi_jar_with_tests(
    name = "onos-gui-jar",
    exclude_tests = [
        "org.onosproject.ui.impl.AbstractUiImplTest",
        "org.onosproject.ui.impl.topo.model.AbstractTopoModelTest",
    ],
    karaf_command_packages = [
        "org.onosproject.ui.impl.cli",
        "org.onosproject.ui.impl.topo",
    ],
    test_deps = TEST_DEPS,
    web_context = "/onos/ui",
    deps = COMPILE_DEPS,
)

genrule(
    name = "onos-gui-java-for-gui2",
    srcs = glob([
        "src/main/java/org/onosproject/ui/impl/**/*.java",
    ]),
    outs = ["onos-gui-java-for-gui2.srcjar"],
    cmd = "jar cf $@ $(SRCS)",
    visibility = ["//visibility:public"],
)

genrule(
    name = "onos-gui-lion-for-gui2",
    srcs = glob([
        "src/main/resources/org/onosproject/ui/lion/**/*",
        "src/main/resources/core/**/*",
    ]),
    outs = ["onos-gui-lion-for-gui2.srcjar"],
    cmd = "jar cf $@ $(SRCS)",
    visibility = ["//visibility:public"],
)

"""
    Builds the tar ball for the ONOS GUI
"""

genrule(
    name = "onos-web-gui",
    srcs = [
        ":_onos-gui-npm-build",
        ":onos-gui-jar",
        ":_root_level_files",
        ":_web_inf_classes_files",
        ":_raw_classes_files",
        ":_app_view_tar",
    ],
    outs = ["onos-gui.jar"],
    cmd = " ROOT=`pwd` &&" +
          " BUILD_FILES=($(locations :_onos-gui-npm-build)) &&" +  # An array of filenames - sorted by time created
          " mkdir -p gui/WEB-INF/classes &&" +
          " cd gui &&" +
          " jar xf $$ROOT/$${BUILD_FILES[0]} &&" +
          " (cd WEB-INF/classes && mkdir -p app/view && cd app/view && jar xf $$ROOT/$(location :_app_view_tar)) &&" +
          " for i in $(locations :_root_level_files); do cp $$ROOT/$$i .; done &&" +
          " for i in $(locations :_web_inf_classes_files); do cp $$ROOT/$$i ./WEB-INF/classes/; done &&" +
          " mkdir ./WEB-INF/classes/raw && " +
          " for i in $(locations :_raw_classes_files); do cp $$ROOT/$$i ./WEB-INF/classes/raw/; done &&" +
          " jar xf $$ROOT/$(location :onos-gui-jar) &&" +
          " find . -type f -exec touch -t 201806280000 {} \\; &&" +
          " jar cmf META-INF/MANIFEST.MF $$ROOT/$@ .",
    output_to_bindir = 1,
    visibility = ["//visibility:public"],
)

onos_app(
    category = "Graphical User Interface",
    description = "ONOS GUI - the original ONOS GUI based " +
                  "on the latest AngularJS - new development should be on GUI2",
    title = "ONOS Legacy GUI",
    url = "http://onosproject.org",
)
