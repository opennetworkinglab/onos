COMPILE_DEPS = CORE_DEPS + [
    "//drivers/default:onos-drivers-default",
    "//drivers/utilities:onos-drivers-utilities",
    "//protocols/netconf/api:onos-protocols-netconf-api",
    "//apps/optical-model:onos-apps-optical-model",
]

BUNDLES = [
    ":onos-drivers-oplink",
    "//drivers/utilities:onos-drivers-utilities",
]

osgi_jar_with_tests(
    resources = glob(["src/main/resources/**"]),
    resources_root = "src/main/resources",
    test_deps = TEST_ADAPTERS,
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.drivers.oplink",
    category = "Drivers",
    description = "Adds support for Oplink devices.",
    included_bundles = BUNDLES,
    required_apps = [
        "org.onosproject.netconf",
        "org.onosproject.optical-model",
    ],
    title = "Oplink Drivers",
    url = "http://onosproject.org",
)
