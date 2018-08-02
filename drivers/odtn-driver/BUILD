COMPILE_DEPS = CORE_DEPS + [
    "@commons_jxpath//jar",
    "//drivers/utilities:onos-drivers-utilities",
    "//protocols/netconf/api:onos-protocols-netconf-api",
    "//apps/odtn/api:onos-apps-odtn-api",
]

TEST_DEPS = TEST_ADAPTERS + [
    "@slf4j_jdk14//jar",
    "//core/api:onos-api-tests",
]

BUNDLES = [
    ":onos-drivers-odtn-driver",
    "//drivers/utilities:onos-drivers-utilities",
    #     "//lib:commons-jxpath",
    #     "//lib:commons-beanutils", # jxpath dependency
    #     "//lib:jdom",  # jxpath dependency
]

osgi_jar_with_tests(
    resources = glob(["src/main/resources/**"]),
    resources_root = "src/main/resources",
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.drivers.odtn-driver",
    category = "Drivers",
    description = "Drivers related to ODTN",
    included_bundles = BUNDLES,
    required_apps = [
        "org.onosproject.netconf",
        "org.onosproject.config",
        "org.onosproject.odtn-api",
    ],
    title = "ODTN Driver",
    url = "https://wiki.onosproject.org/display/ODTN/ODTN",
)
