COMPILE_DEPS = CORE_DEPS + NETTY + [
    "@io_netty_netty_transport//jar",
    "@io_netty_netty_codec//jar",
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
