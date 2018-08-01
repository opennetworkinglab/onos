COMPILE_DEPS = CORE_DEPS + KRYO + CLI + [
    "//core/store/serializers:onos-core-serializers",
]

TEST_DEPS = TEST_ADAPTERS

osgi_jar_with_tests(
    karaf_command_packages = ["org.onosproject.l2lb.cli"],
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.l2lb",
    category = "Utilities",
    description = "L2 Load Balance Service",
    title = "L2 Load Balance Service",
    url = "http://onosproject.org",
)
