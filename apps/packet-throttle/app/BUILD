COMPILE_DEPS = CORE_DEPS + CLI + [
    "//core/store/serializers:onos-core-serializers",
    "//core/store/primitives:onos-core-primitives",
]

osgi_jar_with_tests(
    karaf_command_packages = ["org.onosproject.packetthrottle.cli"],
    test_deps = TEST_ADAPTERS,
    deps = COMPILE_DEPS,
)
