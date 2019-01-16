COMPILE_DEPS = CORE_DEPS + KRYO + CLI + [
    "//core/store/serializers:onos-core-serializers",
]

TEST_DEPS = TEST_ADAPTERS

osgi_jar_with_tests(
    karaf_command_packages = ["org.onosproject.portloadbalancer.cli"],
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.portloadbalancer",
    category = "Utilities",
    description = "Port Load Balance Service",
    title = "Port Load Balance Service",
    url = "http://onosproject.org",
)
