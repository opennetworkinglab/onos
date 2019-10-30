COMPILE_DEPS = CORE_DEPS + JACKSON + CLI + [
    "//core/store/serializers:onos-core-serializers",
    "//apps/optical-model:onos-apps-optical-model",
]

TEST_DEPS = TEST_ADAPTERS + [
    "//utils/osgi:onlab-osgi-tests",
]

osgi_jar_with_tests(
    karaf_command_packages = ["org.onosproject.roadm.cli"],
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)
