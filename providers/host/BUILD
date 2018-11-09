COMPILE_DEPS = CORE_DEPS

TEST_DEPS = TEST_ADAPTERS + [
    "//utils/osgi:onlab-osgi-tests",
]

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.hostprovider",
    category = "Provider",
    description = "Provides host discovery and location to the ONOS core by eavesdropping on the ARP and NDP packets.",
    title = "Host Location Provider",
    url = "http://onosproject.org",
)
