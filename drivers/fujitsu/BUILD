COMPILE_DEPS = CORE_DEPS + CLI + [
    "//drivers/utilities:onos-drivers-utilities",
    "//protocols/netconf/api:onos-protocols-netconf-api",
    "//protocols/netconf/ctl:onos-protocols-netconf-ctl",
    "//apps/optical-model:onos-apps-optical-model",
]

BUNDLES = [
    ":onos-drivers-fujitsu",
    "//drivers/utilities:onos-drivers-utilities",
]

osgi_jar_with_tests(
    exclude_tests = ["org.onosproject.drivers.fujitsu.FujitsuNetconfSessionListenerTest"],
    karaf_command_packages = ["org.onosproject.drivers.fujitsu.cli"],
    resources = glob(["src/main/resources/**"]),
    resources_root = "src/main/resources",
    test_deps = TEST_ADAPTERS,
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.drivers.fujitsu",
    category = "Drivers",
    description = "Adds support for Fujitsu devices.",
    included_bundles = BUNDLES,
    required_apps = [
        "org.onosproject.netconf",
        "org.onosproject.optical-model",
        "org.onosproject.drivers.optical",
    ],
    title = "Fujitsu Drivers",
    url = "http://onosproject.org",
)
