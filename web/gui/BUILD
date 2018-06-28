NODE_VERSION = "8.0.1"

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

genrule(
    name = "_onos-gui-npm-install",
    srcs = [
        "//tools/gui:package.json",
        "@nodejs//:bin/npm",
    ] + glob(
        [
            "src/main/webapp/*.js",
            "src/main/webapp/app/**/*.js",
        ],
        exclude = ["src/main/webapp/dist/*.js"],
    ),
    outs = ["onos-gui-npm-install.tar"],
    cmd = "(ROOT=`pwd` &&" +
          " NPM=$$ROOT/$(location @nodejs//:bin/npm) &&" +
          " cd tools/gui &&" +
          " $$NPM install --loglevel=error --no-cache --cache=$(@D)/.npm --no-update-notifier &&" +
          " tar cf $$ROOT/$@ package.json node_modules)",
    local = True,
    visibility = ["//visibility:public"],
)

genrule(
    name = "_onos-gui-npm-build",
    srcs = [
        "//tools/gui:gulpfile.babel.js",
        ":_onos-gui-npm-install",
        "@nodejs//:bin/npm",
        "//tools/gui:package.json",
    ] + glob(["src/main/webapp/*"]),
    outs = ["onos-gui-npm-build.tar"],
    cmd = "(ROOT=`pwd` &&" +
          " NPM=$$ROOT/$(location @nodejs//:bin/npm) &&" +
          " cd tools/gui &&" +
          " tar xf ../../$(location :_onos-gui-npm-install) &&" +
          " $$NPM run build --no-update-notifier &&" +
          "  cd ../../web/gui/src/main/webapp &&" +
          " jar cf $$ROOT/$@ dist vendor data tests README.md _doc _dev app/fw app/*.css app/*.js app/*.txt)",
    local = True,
    visibility = ["//visibility:public"],
)

genrule(
    name = "_onos-gui-npm",
    srcs = [
        ":_onos-gui-npm-build",
    ],
    outs = ["onos-gui-npm.log"],
    cmd = "ROOT=`pwd` && cd web/gui/src/main/webapp && tar xf $$ROOT/$(location :_onos-gui-npm-build) >$$ROOT/$@",
    local = True,
    visibility = ["//visibility:public"],
)

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

filegroup(
    name = "_raw_classes_files",
    srcs = glob(["src/main/webapp/raw/**"]),
)

# app/view is packaged as a tar file because it has subdirectories that need to be preserved
genrule(
    name = "_app_view_tar",
    srcs = glob(["src/main/webapp/app/view/**"]),
    outs = ["app_view_tar.tar"],
    cmd = " ROOT=`pwd` &&" +
          " cd web/gui/src/main/webapp/app/view &&" +
          " tar cf $$ROOT/$@ .",
)

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
          " mkdir ./WEB-INF/classes/raw && for i in $(locations :_raw_classes_files); do cp $$ROOT/$$i ./WEB-INF/classes/raw/; done &&" +
          " jar xf $$ROOT/$(location :_onos-gui-base-jar) &&" +
          " jar cmf META-INF/MANIFEST.MF $$ROOT/$@ .",
    local = True,
    output_to_bindir = 1,
    visibility = ["//visibility:public"],
)
