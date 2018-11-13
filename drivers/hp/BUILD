COMPILE_DEPS = CORE_DEPS + KRYO + [
    "@openflowj//jar",
    "//drivers/default:onos-drivers-default",
    "//drivers/utilities:onos-drivers-utilities",
    "//protocols/openflow/api:onos-protocols-openflow-api",
    "//core/store/serializers:onos-core-serializers",
]

osgi_jar_with_tests(
    resources = glob(["src/main/resources/**"]),
    resources_root = "src/main/resources",
    test_deps = TEST_ADAPTERS,
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.drivers.hp",
    category = "Drivers",
    description = "Adds support for HP devices.",
    required_apps = ["org.onosproject.openflow"],
    title = "HP Drivers",
    url = "http://onosproject.org",
)
