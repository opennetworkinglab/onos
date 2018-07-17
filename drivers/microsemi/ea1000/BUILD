COMPILE_DEPS = CORE_DEPS + ONOS_YANG + CLI + [
    "@onos_yang_runtime//jar",
    "//drivers/utilities:onos-drivers-utilities",
    "//protocols/netconf/api:onos-protocols-netconf-api",
    "//protocols/netconf/ctl:onos-protocols-netconf-ctl",
    "//models/common:onos-models-common",
    "//models/microsemi:onos-models-microsemi",
    "//apps/cfm/api:onos-apps-cfm-api",
]

TEST_DEPS = TEST_ADAPTERS + [
    "//core/api:onos-api-tests",
    "//drivers/netconf:onos-drivers-netconf-tests",
    "//utils/osgi:onlab-osgi-tests",
    "//apps/cfm/app:onos-apps-cfm-app",
    "//apps/cfm/app:onos-apps-cfm-app-tests",
]

APPS = [
    "org.onosproject.yang",
    "org.onosproject.config",
    "org.onosproject.netconf",
    "org.onosproject.netconfsb",
    "org.onosproject.drivers.netconf",
    "org.onosproject.models.common",
    "org.onosproject.models.microsemi",
    "org.onosproject.cfm",
]

osgi_jar_with_tests(
    exclude_tests = [
        "org.onosproject.drivers.microsemi.EA1000CfmMepProgrammableTest",
        "org.onosproject.drivers.microsemi.EA1000FlowRuleProgrammableTest",
        "org.onosproject.drivers.microsemi.EA1000MeterProviderTest",
        "org.onosproject.drivers.microsemi.EA1000SoamDmProgrammableTest",
        "org.onosproject.drivers.microsemi.yang.IetfSystemManagerTest",
        "org.onosproject.drivers.microsemi.yang.MseaCfmManagerTest",
        "org.onosproject.drivers.microsemi.yang.MseaSaFilteringManagerTest",
        "org.onosproject.drivers.microsemi.yang.MseaSaFilteringManagerTest",
        "org.onosproject.drivers.microsemi.yang.MseaUniEvcServiceManagerTest",
    ],
    resources = glob(["src/main/resources/**"]),
    resources_root = "src/main/resources",
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.drivers.microsemi.ea1000",
    category = "Drivers",
    description = "Adds support for Microsemi EA1000 devices.",
    required_apps = APPS,
    title = "Microsemi Drivers EA1000",
    url = "http://onosproject.org",
)
