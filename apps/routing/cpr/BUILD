COMPILE_DEPS = CORE_DEPS + [
    "//apps/routing-api:onos-apps-routing-api",
]

TEST_DEPS = TEST_ADAPTERS + [
    "//apps/routing-api:onos-apps-routing-api-tests",
    "//utils/osgi:onlab-osgi-tests",
]

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)

BUNDLES = [
    "//apps/routing/common:onos-apps-routing-common",
    "//apps/routing/cpr:onos-apps-routing-cpr",
    "//apps/routing-api:onos-apps-routing-api",
]

APPS = [
    "org.onosproject.route-service",
]

onos_app(
    app_name = "org.onosproject.cpr",
    category = "Traffic Engineering",
    description = "Redirects routing control traffic to a control plane",
    included_bundles = BUNDLES,
    required_apps = APPS,
    title = "Control Plane Redirect",
    url = "http://onosproject.org",
)
