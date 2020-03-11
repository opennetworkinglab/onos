load("//tools/build/bazel:osgi_features.bzl", "osgi_feature", "osgi_feature_repo")
load("//tools/build/bazel:modules.bzl", "FEATURES")

osgi_feature_repo(
    name = "onos-features",
    exported_features = FEATURES,
    visibility = ["//visibility:public"],
)

osgi_feature(
    name = "onos-thirdparty-base",
    description = "ONOS 3rd party dependencies",
    included_bundles = ATOMIX + [
        "@commons_lang//jar",
        "@commons_lang3//jar",
        "@commons_text//jar",
        "@commons_codec//jar",
        "@commons_configuration//jar",
        "@com_google_guava_guava//jar",
        "@io_netty_netty//jar",
        "@io_netty_netty_common//jar",
        "@io_netty_netty_buffer//jar",
        "@io_netty_netty_handler//jar",
        "@io_netty_netty_tcnative_boringssl//jar",
        "@io_netty_netty_codec//jar",
        "@io_netty_netty_codec_http//jar",
        "@io_netty_netty_codec_http2//jar",
        "@io_netty_netty_codec_socks//jar",
        "@io_netty_netty_handler_proxy//jar",
        "@io_netty_netty_transport//jar",
        "@io_netty_netty_transport_native_epoll//jar",
        "@io_netty_netty_transport_native_unix_common//jar",
        "@io_netty_netty_resolver//jar",
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
        "@concurrent_trees//jar",
        "@commons_io//jar",
        "@commons_jxpath//jar",
        "@mapdb//jar",
        "@bcpkix_jdk15on//jar",
        "@bcprov_jdk15on//jar",
        "@commons_beanutils//jar",
        "@jdom//jar",
        "@sigar//jar",
        "@org_osgi_util_function//jar",
        "@org_osgi_util_promise//jar",
        "@jaxb_api//jar",
        "@kafka_clients//jar",
    ],
    required_features = [],
    visibility = ["//visibility:public"],
)

osgi_feature(
    name = "onos-thirdparty-web",
    description = "ONOS 3rd party dependencies for web apps",
    included_bundles = [
        "@jersey_common//jar",
        "@javax_annotation_javax_annotation_api//jar",
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
    name = "onos-rest",
    description = "ONOS REST API components",
    included_bundles = [
        "//web/api:onos-rest",
    ],
    required_features = [
        "onos-api",
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

#osgi_feature(
#    name = "onos-security",
#    description = "Security-Mode ONOS",
#    included_bundles = [
#        "@org_apache_felix_framework_security//jar",
#        "//core/security:onos-security",
#    ],
#    required_features = ["onos-api"],
#    visibility = ["//visibility:public"],
#)
