COMPILE_DEPS = CORE_DEPS + JACKSON + KRYO + CLI + [
    "//core/store/serializers:onos-core-serializers",
    "//apps/route-service/api:onos-apps-route-service-api",
    "//apps/routing/fpm/api:onos-apps-routing-fpm-api",
]

TEST_DEPS = TEST + [
    "//apps/route-service/api:onos-apps-route-service-api-tests",
    "//core/api:onos-api-tests",
]

osgi_jar_with_tests(
    karaf_command_packages = ["org.onosproject.dhcprelay.cli"],
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)
