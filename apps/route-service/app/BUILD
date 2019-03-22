COMPILE_DEPS = CORE_DEPS + JACKSON + KRYO + CLI + REST + [
    "@concurrent_trees//jar",
    "@jersey_server//jar",
    "//core/store/serializers:onos-core-serializers",
    "//apps/route-service/api:onos-apps-route-service-api",
]

TEST_DEPS = TEST + [
    "//apps/route-service/api:onos-apps-route-service-api-tests",
    "//core/api:onos-api-tests",
]

osgi_jar_with_tests(
    api_description = "REST API for Route Service App",
    api_package = "org.onosproject.routeservice.rest",
    api_title = "Route Service App",
    api_version = "1.0",
    karaf_command_packages = ["org.onosproject.routeservice.cli"],
    test_deps = TEST_DEPS,
    web_context = "/onos/routeservice",
    deps = COMPILE_DEPS,
)
