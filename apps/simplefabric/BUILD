COMPILE_DEPS = CORE_DEPS + JACKSON + CLI + REST + [
    "@concurrent_trees//jar",
]

BUNDLES = [
    "//apps/simplefabric:onos-apps-simplefabric",
]

osgi_jar_with_tests(
    test_deps = TEST_ADAPTERS,
    web_context = "/onos/v1/simplefabric",
    deps = COMPILE_DEPS,
)

onos_app(
    category = "Traffic Engineering",
    description = "Simple Fabric application",
    included_bundles = BUNDLES,
    required_apps = [
        "org.onosproject.openflow-base",
        "org.onosproject.lldpprovider",
        "org.onosproject.hostprovider",
    ],
    title = "SimpleFabric",
    url = "http://onosproject.org",
)
