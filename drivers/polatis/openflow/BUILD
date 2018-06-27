COMPILE_DEPS = CORE_DEPS + [
    "@openflowj//jar",
    "//protocols/openflow/api:onos-protocols-openflow-api",
    "//apps/optical-model:onos-apps-optical-model",
]

APPS = [
    "org.onosproject.drivers.optical",
    "org.onosproject.optical-model",
]

osgi_jar_with_tests(
    resources = glob(["src/main/resources/**"]),
    resources_root = "src/main/resources",
    test_deps = TEST_ADAPTERS,
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.drivers.polatis.openflow",
    category = "Drivers",
    description = "ONOS Polatis OpenFlow Device Drivers application.",
    required_apps = APPS,
    title = "Polatis OpenFlow Device Drivers",
    url = "http://www.polatis.com",
)
