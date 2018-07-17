COMPILE_DEPS = CORE_DEPS + ONOS_YANG + [
    "@onos_yang_runtime//jar",
    "//models/l3vpn:onos-models-l3vpn",
    "//models/huawei:onos-models-huawei",
    "//drivers/utilities:onos-drivers-utilities",
    "//protocols/netconf/api:onos-protocols-netconf-api",
    "//apps/l3vpn:onos-apps-l3vpn",
    "//apps/config:onos-apps-config",
    "//apps/yang:onos-apps-yang",
    "//models/common:onos-models-common",
]

APPS = [
    "org.onosproject.yang",
    "org.onosproject.yang-gui",
    "org.onosproject.config",
    "org.onosproject.restconf",
    "org.onosproject.protocols.restconfserver",
    "org.onosproject.netconf",
    "org.onosproject.netconfsb",
    "org.onosproject.models.common",
    "org.onosproject.models.l3vpn",
    "org.onosproject.l3vpn",
    "org.onosproject.models.huawei",
]

TEST_DEPS = TEST_ADAPTERS + [
    "//utils/osgi:onlab-osgi-tests",
]

osgi_jar_with_tests(
    resources = glob(["src/main/resources/**"]),
    resources_root = "src/main/resources",
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.drivers.huawei",
    category = "Drivers",
    description = "Adds support for Huawei devices.",
    required_apps = APPS,
    title = "Huawei Drivers",
    url = "http://onosproject.org",
)
