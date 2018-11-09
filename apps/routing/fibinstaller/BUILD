COMPILE_DEPS = CORE_DEPS + [
    "//apps/routing-api:onos-apps-routing-api",
    "//apps/route-service/api:onos-apps-route-service-api",
]

TEST_DEPS = TEST_ADAPTERS + [
    "//apps/routing-api:onos-apps-routing-api-tests",
    "//apps/route-service/api:onos-apps-route-service-api-tests",
]

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)

BUNDLES = [
    "//apps/routing/fibinstaller:onos-apps-routing-fibinstaller",
    "//apps/routing-api:onos-apps-routing-api",
]

onos_app(
    app_name = "org.onosproject.fibinstaller",
    category = "Traffic Engineering",
    description = "Installs routing rules into switches",
    included_bundles = BUNDLES,
    required_apps = ["org.onosproject.route-service"],
    title = "FIB Installer",
    url = "http://onosproject.org",
)
