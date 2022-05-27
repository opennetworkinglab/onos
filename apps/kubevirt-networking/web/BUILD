COMPILE_DEPS = CORE_DEPS + JACKSON + KRYO + REST + [
    "//apps/kubevirt-node/api:onos-apps-kubevirt-node-api",
    "//apps/kubevirt-networking/api:onos-apps-kubevirt-networking-api",
]

TEST_DEPS = TEST_ADAPTERS + TEST_REST + [
    "//core/api:onos-api-tests",
    "//core/common:onos-core-common-tests",
    "//web/api:onos-rest-tests",
    "@minimal_json//jar",
]

osgi_jar_with_tests(
    api_description = "REST API for KubeVirt Networking",
    api_package = "org.onosproject.kubevirtnetworking.web",
    api_title = "KubeVirt Networking API",
    api_version = "1.0",
    test_deps = TEST_DEPS,
    web_context = "/onos/kubevirtnetworking",
    deps = COMPILE_DEPS,
)
