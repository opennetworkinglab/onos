COMPILE_DEPS = CORE_DEPS + JACKSON + [
    "@javax_ws_rs_api//jar",
    "//utils/rest:onlab-rest",
    "//drivers/utilities:onos-drivers-utilities",
    "//drivers/netconf:onos-drivers-netconf",
    "//protocols/netconf/api:onos-protocols-netconf-api",
]

TEST_DEPS = TEST_ADAPTERS + [
    "//drivers/netconf:onos-drivers-netconf-tests",
]

BUNDLES = [
    ":onos-drivers-ciena-c5170",
    "//drivers/utilities:onos-drivers-utilities",
    "//drivers/netconf:onos-drivers-netconf",
]

REQUIRED_APPS = [
    "org.onosproject.linkdiscovery",
    "org.onosproject.netconf",
    #"org.onosproject.netconfsb",
    "org.onosproject.drivers.netconf",
]

osgi_jar_with_tests(
    resources = glob(["src/main/resources/**"]),
    resources_root = "src/main/resources",
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.drivers.ciena.c5170",
    category = "Drivers",
    description = "Adds support for Ciena 5170 devices.",
    included_bundles = BUNDLES,
    required_apps = REQUIRED_APPS,
    title = "Ciena 5170 Drivers",
    url = "http://onosproject.org",
)
