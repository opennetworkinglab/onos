COMPILE_DEPS = CORE_DEPS + KRYO + [
    "//drivers/default:onos-drivers-default",
    "//apps/flowspec-api/flowapi:onos-apps-flowspec-api-flowapi",
]

osgi_jar_with_tests(
    resources = glob(["src/main/resources/**"]),
    resources_root = "src/main/resources",
    test_deps = TEST_ADAPTERS,
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.drivers.flowspec",
    category = "Drivers",
    description = "Adds support for devices using flow specs.",
    required_apps = [
        "org.onosproject.drivers",
        "org.onosproject.flowspec-api",
    ],
    title = "Generic Flow Spec Drivers",
    url = "http://onosproject.org",
)
