COMPILE_DEPS = CORE_DEPS + NETTY + JACKSON + [
    "@openflowj//jar",
    "//protocols/openflow/api:onos-protocols-openflow-api",
    "//drivers/default:onos-drivers-default",
    "//apps/optical-model:onos-apps-optical-model",
]

TEST_DEPS = TEST_ADAPTERS + [
    "//core/api:onos-api-tests",
    "//utils/osgi:onlab-osgi-tests",
]

osgi_jar_with_tests(
    resources = glob(["src/main/resources/**"]),
    resources_root = "src/main/resources",
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.drivers.optical",
    category = "Drivers",
    description = "Suite of basic optical device drivers.",
    required_apps = [
        "org.onosproject.drivers",
        "org.onosproject.optical-model",
    ],
    title = "Basic Optical Drivers",
    url = "http://onosproject.org",
)
