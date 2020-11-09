COMPILE_DEPS = CORE_DEPS + ONOS_YANG + [
    "//drivers/utilities:onos-drivers-utilities",
    "//drivers/netconf:onos-drivers-netconf",
    "//protocols/netconf/api:onos-protocols-netconf-api",
    "//protocols/netconf/ctl:onos-protocols-netconf-ctl",
    "//models/common:onos-models-common",
    "//models/ciena/waveserverai:onos-models-ciena-waveserverai",
    "//apps/optical-model:onos-apps-optical-model",
    "//drivers/optical:onos-drivers-optical",
]

TEST_DEPS = TEST_ADAPTERS + [
    "//core/api:onos-api-tests",
    "//drivers/netconf:onos-drivers-netconf-tests",
    "//utils/osgi:onlab-osgi-tests",
]

BUNDLES = [
    ":onos-drivers-ciena-waveserverai",
    "//drivers/utilities:onos-drivers-utilities",
    "//drivers/netconf:onos-drivers-netconf",
]

REQUIRED_APPS = [
    "org.onosproject.yang",
    "org.onosproject.optical-model",
    "org.onosproject.drivers.optical",
    "org.onosproject.models.ciena.waveserverai",
    "org.onosproject.netconf",
    #"org.onosproject.netconfsb",
    "org.onosproject.drivers.netconf",
    "org.onosproject.linkdiscovery",
]

osgi_jar_with_tests(
    resources = glob(["src/main/resources/**"]),
    resources_root = "src/main/resources",
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.drivers.ciena.waveserverai",
    category = "Drivers",
    description = "Adds support for Ciena Waveserver Ai devices.",
    included_bundles = BUNDLES,
    required_apps = REQUIRED_APPS,
    title = "Ciena Waveserver Ai Drivers",
    url = "http://onosproject.org",
)
