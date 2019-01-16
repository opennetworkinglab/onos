COMPILE_DEPS = CORE_DEPS + JACKSON + KRYO + CLI + [
    "//core/common:onos-core-common",
    "//core/store/serializers:onos-core-serializers",
    "//apps/route-service/api:onos-apps-route-service-api",
    "//apps/mcast/api:onos-apps-mcast-api",
    "//apps/mcast/cli:onos-apps-mcast-cli",
    "//apps/portloadbalancer:onos-apps-portloadbalancer",
]

TEST_DEPS = TEST_ADAPTERS + [
    "//core/net:onos-core-net",
    "//apps/route-service/api:onos-apps-route-service-api-tests",
]

osgi_jar_with_tests(
    karaf_command_packages = ["org.onosproject.segmentrouting.cli"],
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)
