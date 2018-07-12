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

COMPILE_DEPS = CORE_DEPS + JACKSON + KRYO + [
    "@javax_ws_rs_api//jar",
    "@servlet_api//jar",
    "@jetty_websocket//jar",
    "@jetty_util//jar",
    "@jersey_media_multipart//jar",
    "@org_apache_karaf_shell_console//jar",
    "@jersey_server//jar",
    "//cli:onos-cli",
    "//incubator/api:onos-incubator-api",
    "//incubator/net:onos-incubator-net",
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
    dependency to expose all webapp files in the sandbox
"""

filegroup(
    name = "_src_main_webapp_files",
    srcs = glob([
        "src/main/webapp/**",
    ]),
)

"""
    Install node.js and npm, and gather files needed from //tools/gui
"""

genrule(
    name = "_onos-gui-npm-install",
    srcs = [
        "@nodejs//:bin/npm",
        "//tools/gui:tools-gui-gulp",
    ] + glob(
        [
            "src/main/webapp/*.js",
            "src/main/webapp/app/**/*.js",
        ],
        exclude = ["src/main/webapp/dist/*.js"],
    ),
    outs = ["onos-gui-npm-install.tar"],
    cmd = " ROOT=`pwd` &&" +
          " export XDG_CONFIG_HOME=$(@D)/config &&" +  # npm config cache to the sandbos
          " export BABEL_DISABLE_CACHE=1" +  # turn off babel cache
          " NPM=$$ROOT/$(location @nodejs//:bin/npm) &&" +
          " mkdir -p tools/gui &&" +
          " cd tools/gui &&" +
          " tar xf ../../$(location //tools/gui:tools-gui-gulp) &&" +
          " $$NPM install  --no-cache --loglevel=error >npm-install.out 2>&1 &&" +
          " tar hcf $$ROOT/$@ package.json gulpfile.babel.js node_modules gulp-tasks",
)

"""
    Run npm build to create node.js files
"""

genrule(
    name = "_onos-gui-npm-build",
    srcs = [
        "@nodejs//:bin/npm",
        "@nodejs//:bin/node",
        ":_onos-gui-npm-install",
        ":_web_app_all",
    ],
    outs = ["onos-gui-npm-build.tar"],
    cmd = "(ROOT=`pwd` &&" +
          " export XDG_CONFIG_HOME=$(@D)/config &&" +
          " export BABEL_DISABLE_CACHE=1" +
          " NPM=$(location @nodejs//:bin/npm) &&" +
          " (mkdir -p web/gui && cd web/gui && tar xf ../../$(location :_web_app_all)) &&" +
          " mkdir -p tools/gui && cd tools/gui &&" +
          " tar xf $$ROOT/$(location :_onos-gui-npm-install) &&" +
          " $$ROOT/$$NPM run build --no-cache --loglevel=error >npm-build.out &&" +
          " cd ../../web/gui/src/main/webapp &&" +
          " jar cf $$ROOT/$@ dist vendor data tests README.md _doc _dev app/fw app/*.css app/*.js app/*.txt)",
)

"""
    Make a jar file of all the webapp files. Useful for breaking symblic links in the sandbox
"""

genrule(
    name = "_web_app_all",
    srcs = [":_src_main_webapp_files"],
    outs = ["web_app_all.tar"],
    cmd = "cd web/gui && tar hcf ../../$@ src/main/webapp",
)

"""
    app/view is packaged as a tar file because it has subdirectories that need to be preserved
"""

genrule(
    name = "_app_view_tar",
    srcs = glob(["src/main/webapp/app/view/**"]),
    outs = ["app_view_tar.tar"],
    cmd = " ROOT=`pwd` &&" +
          " cd web/gui/src/main/webapp/app/view &&" +
          " tar hcf $$ROOT/$@ .",
)

"""
    Builds the java jar for the java code provided by the GUI
"""

osgi_jar_with_tests(
    name = "_onos-gui-base-jar",
    exclude_tests = [
        "org.onosproject.ui.impl.AbstractUiImplTest",
        "org.onosproject.ui.impl.topo.model.AbstractTopoModelTest",
    ],
    test_deps = TEST_DEPS,
    web_context = "/onos/ui",
    deps = COMPILE_DEPS,
)

"""
    Builds the tar ball for the ONOS GUI
"""

genrule(
    name = "onos-gui",
    srcs = [
        ":_onos-gui-npm-build",
        ":_onos-gui-base-jar",
        ":_root_level_files",
        ":_web_inf_classes_files",
        ":_raw_classes_files",
        ":_app_view_tar",
    ],
    outs = ["onos-gui.jar"],
    cmd = " ROOT=`pwd` &&" +
          " mkdir -p gui/WEB-INF/classes &&" +
          " cd gui &&" +
          " tar xf $$ROOT/$(location :_onos-gui-npm-build) &&" +
          " (cd WEB-INF/classes && mkdir -p app/view && cd app/view && tar xf $$ROOT/$(location :_app_view_tar)) &&" +
          " for i in $(locations :_root_level_files); do cp $$ROOT/$$i .; done &&" +
          " for i in $(locations :_web_inf_classes_files); do cp $$ROOT/$$i ./WEB-INF/classes/; done &&" +
          " mkdir ./WEB-INF/classes/raw && " +
          " for i in $(locations :_raw_classes_files); do cp $$ROOT/$$i ./WEB-INF/classes/raw/; done &&" +
          " jar xf $$ROOT/$(location :_onos-gui-base-jar) &&" +
          " jar cmf META-INF/MANIFEST.MF $$ROOT/$@ .",
    output_to_bindir = 1,
    visibility = ["//visibility:public"],
)
