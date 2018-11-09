COMPILE_DEPS = CORE_DEPS + JACKSON + CLI + REST + [
    "//apps/simplefabric/api:onos-apps-simplefabric-api",
    "//apps/route-service/api:onos-apps-route-service-api",
    "@concurrent_trees//jar",
]

TEST_DEPS = TEST_ADAPTERS + TEST_REST + [
    "//core/api:onos-api-tests",
    "//core/common:onos-core-common-tests",
    "//web/api:onos-rest-tests",
]

osgi_jar_with_tests(
    api_description = "REST API for Simple Fabric",
    api_package = "org.onosproject.simplefabric.web",
    api_title = "Simple Fabric API",
    api_version = "1.0",
    karaf_command_packages = ["org.onosproject.simplefabric.cli"],
    test_deps = TEST_DEPS,
    web_context = "/onos/v1/simplefabric",
    deps = COMPILE_DEPS,
)
