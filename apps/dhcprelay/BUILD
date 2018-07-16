COMPILE_DEPS = CORE_DEPS + JACKSON + KRYO + CLI + [
    "//core/store/serializers:onos-core-serializers",
    "//apps/route-service/api:onos-apps-route-service-api",
    "//apps/routing/fpm/api:onos-apps-routing-fpm-api",
]

TEST_DEPS = TEST + [
    "//apps/route-service/api:onos-apps-route-service-api-tests",
    "//core/api:onos-api-tests",
]

BUNDLES = [
    "//apps/dhcprelay:onos-apps-dhcprelay",
    "//apps/routing/fpm/api:onos-apps-routing-fpm-api",
]

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.dhcprelay",
    category = "Utility",
    description = "DHCP Relay Agent Application.",
    included_bundles = BUNDLES,
    required_apps = ["org.onosproject.route-service"],
    title = "DHCP Relay Agent",
    url = "http://onosproject.org",
)
