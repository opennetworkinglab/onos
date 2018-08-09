COMPILE_DEPS = CORE_DEPS + NETTY + JACKSON + CLI + [
    "@io_netty_netty//jar",
    "//incubator/api:onos-incubator-api",
    "//apps/routing-api:onos-apps-routing-api",
    "//apps/route-service/api:onos-apps-route-service-api",
    "//core/common:onos-core-common",
]

TEST_DEPS = TEST_ADAPTERS + [
    "//incubator/api:onos-incubator-api-tests",
    "//apps/routing-api:onos-apps-routing-api-tests",
]

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)
