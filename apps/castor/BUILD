COMPILE_DEPS = CORE_DEPS + JACKSON + KRYO + REST + [
    "//core/store/serializers:onos-core-serializers",
    "//apps/routing-api:onos-apps-routing-api",
    "//apps/intentsync:onos-apps-intentsync",
    "@jaxb_api//jar",
]

BUNDLES = [
    "//apps/castor:onos-apps-castor",
    "//apps/routing-api:onos-apps-routing-api",
    "//apps/routing/common:onos-apps-routing-common",
]

TEST_DEPS = TEST_ADAPTERS + [
    "//apps/routing-api:onos-apps-routing-api-tests",
]

osgi_jar_with_tests(
    api_description = "REST API for Castor",
    api_package = "org.onosproject.castor",
    api_title = "Castor",
    api_version = "1.0",
    test_deps = TEST_DEPS,
    web_context = "/onos/castor",
    deps = COMPILE_DEPS,
)

onos_app(
    category = "Utility",
    description = "Castor application",
    included_bundles = BUNDLES,
    required_apps = [
        "org.onosproject.intentsynchronizer",
        "org.onosproject.route-service",
    ],
    title = "Castor",
    url = "http://onosproject.org",
)
