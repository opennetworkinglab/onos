COMPILE_DEPS = CORE_DEPS

TEST_DEPS = TEST_ADAPTERS + [
    "//utils/osgi:onlab-osgi-tests",
]

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.hostprobingprovider",
    category = "Provider",
    description = "Provides host probing mechanism that discovers or verifies the existence of a host at specific location",
    title = "Host Probing Provider",
    url = "http://onosproject.org",
)
