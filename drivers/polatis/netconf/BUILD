COMPILE_DEPS = CORE_DEPS + JACKSON + ONOS_YANG + [
    "//models/polatis:onos-models-polatis",
    "//drivers/utilities:onos-drivers-utilities",
    "//protocols/netconf/api:onos-protocols-netconf-api",
    "//apps/optical-model:onos-apps-optical-model",
]

APPS = [
    "org.onosproject.netconf",
    "org.onosproject.models.common",
    "org.onosproject.models.polatis",
    "org.onosproject.optical-model",
    "org.onosproject.faultmanagement",
]

TEST_DEPS = TEST_ADAPTERS + [
    "//utils/osgi:onlab-osgi-tests",
]

BUNDLES = [
    ":onos-drivers-polatis-netconf",
    "//drivers/utilities:onos-drivers-utilities",
]

osgi_jar_with_tests(
    resources = glob(["src/main/resources/**"]),
    resources_root = "src/main/resources",
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.drivers.polatis.netconf",
    category = "Drivers",
    description = "ONOS Polatis Device Drivers application.",
    included_bundles = BUNDLES,
    required_apps = APPS,
    title = "Polatis Device Drivers",
    url = "http://www.polatis.com",
)
