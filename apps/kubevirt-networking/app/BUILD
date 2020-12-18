COMPILE_DEPS = CORE_DEPS + JACKSON + KRYO + CLI + REST + [
    "//core/store/serializers:onos-core-serializers",
    "//protocols/ovsdb/api:onos-protocols-ovsdb-api",
    "//protocols/ovsdb/rfc:onos-protocols-ovsdb-rfc",
    "//apps/kubevirt-node/api:onos-apps-kubevirt-node-api",
    "//apps/kubevirt-networking/api:onos-apps-kubevirt-networking-api",
    "@commons_net//jar",
    "@jersey_client//jar",
    "@json//jar",
    "@k8s_client//jar",
    "@k8s_model//jar",
    "@okhttp//jar",
    "@okio//jar",
    "@logging_interceptor//jar",
    "@jackson_dataformat_yaml//jar",
    "@snakeyaml//jar",
    "@zjsonpatch//jar",
    "@validation_api//jar",
]

TEST_DEPS = TEST_ADAPTERS + TEST_REST + [
    "//core/api:onos-api-tests",
    "//core/common:onos-core-common-tests",
    "//web/api:onos-rest-tests",
]

osgi_jar_with_tests(
    api_description = "REST API for KubeVirt Networking",
    api_package = "org.onosproject.kubevirtnetworking.web",
    api_title = "KubeVirt Networking API",
    api_version = "1.0",
    karaf_command_packages = ["org.onosproject.kubevirtnetworking.cli"],
    test_deps = TEST_DEPS,
    web_context = "/onos/kubevirtnetworking",
    deps = COMPILE_DEPS,
)
