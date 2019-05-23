COMPILE_DEPS = CORE_DEPS + [
    "//drivers/utilities:onos-drivers-utilities",
    "//protocols/netconf/api:onos-protocols-netconf-api",
]

BUNDLES = [
    ":onos-drivers-juniper",
    "//drivers/utilities:onos-drivers-utilities",
]

TEST_DEPS = TEST_ADAPTERS

osgi_jar_with_tests(
    resources = glob(["src/main/resources/**"]),
    resources_root = "src/main/resources",
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.drivers.juniper",
    category = "Drivers",
    description = "Adds support for Juniper devices.",
    included_bundles = BUNDLES,
    required_apps = ["org.onosproject.netconf"],
    title = "Juniper Drivers",
    url = "http://onosproject.org",
)
