COMPILE_DEPS = CORE_DEPS + [
    "@org_apache_servicemix_bundles_snmp4j//jar",
    "//drivers/utilities:onos-drivers-utilities",
    "//protocols/snmp/api:onos-protocols-snmp-api",
    "//protocols/tl1/api:onos-protocols-tl1-api",
    "//protocols/netconf/api:onos-protocols-netconf-api",
    "//apps/optical-model:onos-apps-optical-model",
    "//drivers/odtn-driver:onos-drivers-odtn-driver",
    "//drivers/optical:onos-drivers-optical",
]

BUNDLES = [
    ":onos-drivers-lumentum",
    "//drivers/utilities:onos-drivers-utilities",
]

osgi_jar_with_tests(
    resources = glob(["src/main/resources/**"]),
    resources_root = "src/main/resources",
    test_deps = TEST_ADAPTERS,
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.drivers.lumentum",
    category = "Drivers",
    description = "ONOS Lumentum Device Drivers application.",
    included_bundles = BUNDLES,
    required_apps = [
        "org.onosproject.snmp",
        "org.onosproject.faultmanagement",
        "org.onosproject.optical-model",
        "org.onosproject.tl1",
        "org.onosproject.netconf",
        "org.onosproject.drivers.odtn-driver",
        "org.onosproject.drivers.netconf",
        "org.onosproject.drivers.optical",
    ],
    title = "Lumentum Drivers",
    url = "http://onosproject.org",
)
