COMPILE_DEPS = CORE_DEPS + [
    "//drivers/utilities:onos-drivers-utilities",
    "//protocols/netconf/api:onos-protocols-netconf-api",
]

BUNDLES = [
    ":onos-drivers-cisco-netconf",
    "//drivers/utilities:onos-drivers-utilities",
]

osgi_jar_with_tests(
    resources = glob(["src/main/resources/**"]),
    resources_root = "src/main/resources",
    test_deps = TEST_ADAPTERS,
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.drivers.cisco.netconf",
    category = "Drivers",
    description = "Adds support for Cisco devices using NETCONF protocol.",
    included_bundles = BUNDLES,
    required_apps = [
        "org.onosproject.netconf",
        "org.onosproject.drivers.netconf",
    ],
    title = "Cisco NETCONF Drivers",
    url = "http://onosproject.org",
)
