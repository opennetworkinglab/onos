COMPILE_DEPS = CORE_DEPS + JACKSON + CLI + [
    "//apps/routing-api:onos-apps-routing-api",
    "//apps/route-service/api:onos-apps-route-service-api",
    "//apps/intentsync:onos-apps-intentsync",
]

BUNDLES = [
    "//apps/sdnip:onos-apps-sdnip",
    "//apps/routing-api:onos-apps-routing-api",
    "//apps/routing/common:onos-apps-routing-common",
]

TEST_DEPS = TEST_ADAPTERS + [
    "//apps/routing-api:onos-apps-routing-api-tests",
    "//apps/route-service/api:onos-apps-route-service-api-tests",
]

osgi_jar_with_tests(
    karaf_command_packages = [
        "org.onosproject.sdnip.cli",
        "org.onosproject.sdnip.cli.completer",
    ],
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)

onos_app(
    category = "Traffic Engineering",
    description = "SDN-IP peering application",
    included_bundles = BUNDLES,
    required_apps = [
        "org.onosproject.intentsynchronizer",
        "org.onosproject.route-service",
    ],
    title = "SDN-IP",
    url = "http://onosproject.org",
)
