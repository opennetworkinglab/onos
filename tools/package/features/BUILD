load("//tools/build/bazel:osgi_features.bzl", "osgi_feature", "osgi_feature_repo")
load("//:modules.bzl", "FEATURES")

osgi_feature_repo(
    name = "onos-features",
    exported_features = FEATURES,
    visibility = ["//visibility:public"],
)

osgi_feature(
    name = "onos-thirdparty-base",
    description = "ONOS 3rd party dependencies",
    included_bundles = [
        "@atomix//jar",
        "@commons_lang//jar",
        "@commons_lang3//jar",
        "@commons_text//jar",
        "@commons_configuration//jar",
        "@guava//jar",
        "@netty//jar",
        "@netty_common//jar",
        "@netty_buffer//jar",
        "@netty_transport//jar",
        "@netty_handler//jar",
        "@netty_codec//jar",
        "@netty_transport_native_epoll//jar",
        "@netty_resolver//jar",
        "@commons_pool//jar",
        "@commons_math3//jar",
        "@joda_time//jar",
        "@metrics_core//jar",
        "@metrics_json//jar",
        "@minimal_json//jar",
        "@kryo//jar",
        "@reflectasm//jar",
        "@asm//jar",
        "@minlog//jar",
        "@objenesis//jar",
        "@jackson_core//jar",
        "@jackson_annotations//jar",
        "@jackson_databind//jar",
        "@commons_collections//jar",
        "@typesafe_config//jar",
        "@concurrent_trees//jar",
        "@commons_io//jar",
        "@mapdb//jar",
        "@bcpkix_jdk15on//jar",
        "@bcprov_jdk15on//jar",
        "@commons_jxpath//jar",
        "@commons_beanutils//jar",
        "@jdom//jar",
        "@sigar//jar",
    ],
    required_features = [],
    visibility = ["//visibility:public"],
)

osgi_feature(
    name = "onos-thirdparty-web",
    description = "ONOS 3rd party dependencies for web apps",
    included_bundles = [
        "@jersey_common//jar",
        "@javax_annotation_api//jar",
        "@javax_ws_rs_api//jar",
        "@hk2_api//jar",
        "@hk2_locator//jar",
        "@hk2_osgi_resource_locator//jar",
        "@javax_inject//jar",
        "@jersey_server//jar",
        "@jersey_hk2//jar",
        "@aopalliance_repackaged//jar",
        "@hk2_utils//jar",
        "@validation_api//jar",
        "@javassist//jar",
        "@jersey_container_servlet//jar",
        "@jersey_container_servlet_core//jar",
        "@jersey_media_multipart//jar",
        "@jersey_client//jar",
        "@mimepull//jar",
    ],
    required_features = [
        "war",
        "onos-thirdparty-base",
    ],
    visibility = ["//visibility:public"],
)

osgi_feature(
    name = "onos-api",
    description = "ONOS services and model API",
    included_bundles = [
        "//utils/misc:onlab-misc",
        "//utils/osgi:onlab-osgi",
        "//utils/rest:onlab-rest",
        "//core/api:onos-api",
        "//incubator/api:onos-incubator-api",
    ],
    required_features = [
        "scr",
        "onos-thirdparty-base",
        "onos-thirdparty-web",
    ],
    visibility = ["//visibility:public"],
)

osgi_feature(
    name = "onos-core",
    description = "ONOS core components",
    included_bundles = [
        "//core/net:onos-core-net",
        "//core/common:onos-core-common",
        "//core/store/dist:onos-core-dist",
        "//core/store/primitives:onos-core-primitives",
        "//core/store/persistence:onos-core-persistence",
        "//core/store/serializers:onos-core-serializers",
    ],
    required_features = ["onos-api"],
    visibility = ["//visibility:public"],
)

osgi_feature(
    name = "onos-incubator",
    description = "ONOS core incubator components",
    included_bundles = [
        "//incubator/net:onos-incubator-net",
        "//incubator/store:onos-incubator-store",
        "//incubator/rpc:onos-incubator-rpc",
    ],
    required_features = ["onos-core"],
    visibility = ["//visibility:public"],
)

osgi_feature(
    name = "onos-rest",
    description = "ONOS REST API components",
    included_bundles = [
        "//utils/rest:onlab-rest",
        "//web/api:onos-rest",
    ],
    required_features = [
        "onos-api",
        "onos-thirdparty-web",
    ],
    visibility = ["//visibility:public"],
)

osgi_feature(
    name = "onos-cli",
    description = "ONOS admin command console components",
    included_bundles = [
        "//cli:onos-cli",
    ],
    required_features = ["onos-api"],
    visibility = ["//visibility:public"],
)

osgi_feature(
    name = "onos-gui",
    description = "ONOS GUI console components",
    included_bundles = [
        "//web/gui:onos-gui",
        "@jetty_websocket//jar",
        "//utils/rest:onlab-rest",
    ],
    required_features = [
        "onos-api",
        "onos-thirdparty-web",
    ],
    visibility = ["//visibility:public"],
)

#osgi_feature (
#  name = "onos-gui2",
#  description = "ONOS GUI2 console components",
#  required_features = ["onos-api", "onos-thirdparty-web"],
#  included_bundles = [
#    "//lib:jetty_websocket",
#    "//utils/rest:onlab-rest",
#    "//web/gui2:onos-gui2",
#  ]
#)

#osgi_feature (
#  name = "onos-security",
#  description ="Security-Mode ONOS",
#  required_features = ["onos-api"],
#  included_bundles = [
#    "//lib:org.apache.felix.framework.security",
#    "//core/security:onos-security",
#  ]
#)
