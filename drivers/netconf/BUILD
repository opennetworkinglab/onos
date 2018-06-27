COMPILE_DEPS = CORE_DEPS + [
    "//drivers/utilities:onos-drivers-utilities",
    "//protocols/netconf/api:onos-protocols-netconf-api",
]

BUNDLES = [
    ":onos-drivers-netconf",
    "//drivers/utilities:onos-drivers-utilities",
]

osgi_jar_with_tests(
    resources = glob(["src/main/resources/**"]),
    resources_root = "src/main/resources",
    test_deps = TEST_ADAPTERS,
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.drivers.netconf",
    category = "Drivers",
    description = "Adds support for devices using NETCONF.",
    included_bundles = BUNDLES,
    required_apps = ["org.onosproject.netconf"],
    title = "Generic NETCONF Drivers",
    url = "http://onosproject.org",
)
