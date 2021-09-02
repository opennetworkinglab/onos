COMPILE_DEPS = CORE_DEPS + [
    "//drivers/p4runtime:onos-drivers-p4runtime",
]

TEST_DEPS = TEST_ADAPTERS

BUNDLES = [
    ":onos-drivers-barefoot",
]

osgi_jar_with_tests(
    resources = glob(["src/main/resources/**"]),
    resources_root = "src/main/resources",
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.drivers.barefoot",
    category = "Drivers",
    description = "Adds support for switches based on Barefoot Networks ASICs",
    included_bundles = BUNDLES,
    required_apps = [
        "org.onosproject.drivers.stratum",
        "org.onosproject.drivers.p4runtime",
    ],
    title = "Barefoot Drivers",
    url = "http://onosproject.org",
)
