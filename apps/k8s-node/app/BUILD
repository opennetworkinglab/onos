COMPILE_DEPS = CORE_DEPS + JACKSON + KRYO + CLI + REST + [
    "//core/store/serializers:onos-core-serializers",
    "//protocols/ovsdb/api:onos-protocols-ovsdb-api",
    "//protocols/ovsdb/rfc:onos-protocols-ovsdb-rfc",
    "//apps/k8s-node/api:onos-apps-k8s-node-api",
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
    api_description = "REST API for Kubernetes Node",
    api_package = "org.onosproject.k8snode.web",
    api_title = "Kubernetes Node API",
    api_version = "1.0",
    karaf_command_packages = ["org.onosproject.k8snode.cli"],
    test_deps = TEST_DEPS,
    web_context = "/onos/k8snode",
    deps = COMPILE_DEPS,
)
