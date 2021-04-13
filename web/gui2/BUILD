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
    This uses Angular Buildtools Convergence - https://bazel.angular.io/
    in place of previous bash scripts that wrapped Angular CLI
"""

load("//tools/build/bazel:jdk_genrule.bzl", genrule = "jdk_genrule")
load("@npm//@bazel/typescript:index.bzl", "ts_config")

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

# This export allows targets in other packages to reference files that live
# in this package.
# TODO Try to move this in to /web/gui2-fw-lib when possible
exports_files(
    ["tsconfig.json"],
    visibility = ["//visibility:public"],
)

ts_config(
    name = "tsconfig-test",
    src = "tsconfig-test.json",
    visibility = ["//visibility:public"],
    deps = [":tsconfig.json"],
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
        "//web/gui2/src/main/webapp:prodapp",
        ":_onos-gui2-base-jar",
        "//web/gui:onos-gui-lion-for-gui2",
        "//web/gui2/src/main/webapp:WEB-INF/web.xml",
        "//web/gui:onos-gui-jar_cfgdef.jar",
    ],
    outs = ["onos-gui2.jar"],
    cmd = " ROOT=`pwd` &&" +
          " mkdir -p web/gui2/WEB-INF/classes/org/onosproject/ui/ &&" +
          " cd web/gui2 &&" +
          " jar xf $$ROOT/$(location :_onos-gui2-base-jar) &&" +
          " unzip -q $$ROOT/$(location //web/gui:onos-gui-lion-for-gui2) web/gui/src/main/resources/**/* &&" +
          " mv web/gui/src/main/resources/org/onosproject/ui/lion* WEB-INF/classes/org/onosproject/ui/ &&" +
          " cp -R $$ROOT/$(location //web/gui2/src/main/webapp:prodapp)/* WEB-INF/classes/ &&" +
          " mv $$ROOT/$(location //web/gui2/src/main/webapp:WEB-INF/web.xml) WEB-INF &&" +
          " (cd WEB-INF/classes && jar xf $$ROOT/$(location //web/gui:onos-gui-jar_cfgdef.jar)) &&" +
          " find . -type f -exec touch -t 202001010001 {} \\; &&" +
          " jar cmf META-INF/MANIFEST.MF $$ROOT/$@ WEB-INF/web.xml WEB-INF/classes OSGI-INF/*.xml",
    output_to_bindir = 1,
    visibility = ["//visibility:public"],
)

onos_app(
    category = "Graphical User Interface",
    description = "ONOS GUI2 - a reengineered version of the original ONOS GUI " +
                  "based on the latest Angular framework components",
    title = "ONOS GUI2",
    url = "http://onosproject.org",
)
