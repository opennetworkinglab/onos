COMPILE_DEPS = CORE_DEPS + JACKSON + KRYO + CLI + [
    "//core/store/serializers:onos-core-serializers",
    "//apps/optical-model:onos-apps-optical-model",
]

TEST_DEPS = TEST_ADAPTERS + [
    "//utils/osgi:onlab-osgi-tests",
]

osgi_jar_with_tests(
    karaf_command_packages = ["org.onosproject.newoptical.cli"],
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)

onos_app(
    category = "Optical",
    description = "Packet/Optical use-case application.",
    required_apps = ["org.onosproject.optical-model"],
    title = "Packet/Optical Use-Case",
    url = "http://onosproject.org",
)
