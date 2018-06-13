COMPILE_DEPS = CORE_DEPS + JACKSON + KRYO + [
    "@org_apache_karaf_shell_console//jar",
    "//cli:onos-cli",
    "//core/common:onos-core-common",
    "//core/store/serializers:onos-core-serializers",
    "//incubator/api:onos-incubator-api",
    "//apps/route-service/api:onos-apps-route-service-api",
    "//apps/mcast/api:onos-apps-mcast-api",
    "//apps/mcast/cli:onos-apps-mcast-cli",
]

TEST_DEPS = TEST_ADAPTERS + [
    "//core/net:onos-core-net",
    "//incubator/api:onos-incubator-api-tests",
    "//apps/route-service/api:onos-apps-route-service-api-tests",
]

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)
