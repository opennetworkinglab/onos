COMPILE_DEPS = CORE_DEPS + NETTY + JACKSON + CLI + [
    "@io_netty_netty//jar",
    "//apps/routing-api:onos-apps-routing-api",
    "//apps/route-service/api:onos-apps-route-service-api",
    "//core/common:onos-core-common",
]

TEST_DEPS = TEST_ADAPTERS + [
    "//apps/routing-api:onos-apps-routing-api-tests",
]

osgi_jar_with_tests(
    karaf_command_packages = ["org.onosproject.routing.cli"],
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)
