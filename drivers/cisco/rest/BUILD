COMPILE_DEPS = CORE_DEPS + JACKSON + [
    "@javax_ws_rs_api//jar",
    "//utils/rest:onlab-rest",
    "//drivers/utilities:onos-drivers-utilities",
    "//protocols/rest/api:onos-protocols-rest-api",
]

BUNDLES = [
    ":onos-drivers-cisco-rest",
    "//drivers/utilities:onos-drivers-utilities",
]

osgi_jar_with_tests(
    resources = glob(["src/main/resources/**"]),
    resources_root = "src/main/resources",
    test_deps = TEST_ADAPTERS,
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.drivers.cisco.rest",
    category = "Drivers",
    description = "Adds support for Cisco devices using REST.",
    included_bundles = BUNDLES,
    required_apps = ["org.onosproject.restsb"],
    title = "Cisco REST Drivers",
    url = "http://onosproject.org",
)
