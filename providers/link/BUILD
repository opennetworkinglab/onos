TEST_DEPS = TEST_ADAPTERS + [
    "//utils/osgi:onlab-osgi-tests",
]

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    deps = CORE_DEPS,
)

onos_app(
    app_name = "org.onosproject.linkdiscovery",
    category = "Provider",
    description = "ONOS link discovery provider.",
    title = "Link Discovery Provider",
    url = "http://onosproject.org",
)
