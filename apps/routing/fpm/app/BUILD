COMPILE_DEPS = CORE_DEPS + NETTY + KRYO + CLI + [
    "@io_netty_netty//jar",
    "//apps/routing-api:onos-apps-routing-api",
    "//apps/route-service/api:onos-apps-route-service-api",
    "//core/store/serializers:onos-core-serializers",
    "//apps/routing/fpm/api:onos-apps-routing-fpm-api",
]

osgi_jar_with_tests(
    karaf_command_packages = ["org.onosproject.routing.fpm.cli"],
    test_deps = TEST_ADAPTERS,
    deps = COMPILE_DEPS,
)
