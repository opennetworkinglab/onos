COMPILE_DEPS = CORE_DEPS + NETTY + [
    "@netty_transport//jar",
    "@netty_codec//jar",
    "//protocols/tl1/api:onos-protocols-tl1-api",
]

TEST_DEPS = TEST_ADAPTERS + [
    "//utils/osgi:onlab-osgi-tests",
    "//core/api:onos-api-tests",
]

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)
