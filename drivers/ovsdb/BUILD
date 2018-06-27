COMPILE_DEPS = CORE_DEPS + [
    "//drivers/utilities:onos-drivers-utilities",
    "//protocols/ovsdb/api:onos-protocols-ovsdb-api",
    "//protocols/ovsdb/rfc:onos-protocols-ovsdb-rfc",
]

TEST_DEPS = TEST_ADAPTERS + [
    "//protocols/ovsdb/api:onos-protocols-ovsdb-api-tests",
]

osgi_jar_with_tests(
    resources = glob(["src/main/resources/**"]),
    resources_root = "src/main/resources",
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.drivers.ovsdb",
    category = "Drivers",
    description = "Adds support for devices using OVSDB.",
    required_apps = ["org.onosproject.ovsdb-base"],
    title = "Generic OVSDB Drivers",
    url = "http://onosproject.org",
)
