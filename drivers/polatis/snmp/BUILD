COMPILE_DEPS = CORE_DEPS + [
    "@org_apache_servicemix_bundles_snmp4j//jar",
    "//drivers/utilities:onos-drivers-utilities",
    "//protocols/snmp/api:onos-protocols-snmp-api",
    "//apps/optical-model:onos-apps-optical-model",
]

APPS = [
    "org.onosproject.snmp",
    "org.onosproject.optical-model",
    "org.onosproject.faultmanagement",
]

TEST_DEPS = TEST_ADAPTERS + [
    "//core/api:onos-api-tests",
]

osgi_jar_with_tests(
    resources = glob(["src/main/resources/**"]),
    resources_root = "src/main/resources",
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.drivers.polatis.snmp",
    category = "Drivers",
    description = "ONOS Polatis SNMP Device Drivers application.",
    required_apps = APPS,
    title = "Polatis SNMP Device Drivers",
    url = "http://www.polatis.com",
)
