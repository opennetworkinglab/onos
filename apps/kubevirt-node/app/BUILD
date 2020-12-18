COMPILE_DEPS = CORE_DEPS + JACKSON + KRYO + CLI + REST + [
    "//core/store/serializers:onos-core-serializers",
    "//protocols/ovsdb/api:onos-protocols-ovsdb-api",
    "//protocols/ovsdb/rfc:onos-protocols-ovsdb-rfc",
    "//apps/kubevirt-node/api:onos-apps-kubevirt-node-api",
    "@k8s_client//jar",
    "@k8s_model//jar",
    "@okhttp//jar",
    "@okio//jar",
    "@logging_interceptor//jar",
    "@jackson_dataformat_yaml//jar",
    "@snakeyaml//jar",
    "@commons_net//jar",
]

TEST_DEPS = TEST_ADAPTERS + TEST_REST + [
    "//core/api:onos-api-tests",
    "//core/common:onos-core-common-tests",
    "//web/api:onos-rest-tests",
]

osgi_jar_with_tests(
    api_description = "REST API for KubeVirt Node",
    api_package = "org.onosproject.kubevirtnode.web",
    api_title = "KubeVirt Node API",
    api_version = "1.0",
    karaf_command_packages = ["org.onosproject.kubevirtnode.cli"],
    test_deps = TEST_DEPS,
    web_context = "/onos/kubevirtnode",
    deps = COMPILE_DEPS,
)
