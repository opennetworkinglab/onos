COMPILE_DEPS = CORE_DEPS + NETTY + JACKSON + [
    "@openflowj//jar",
    "@netty_codec//jar",
    "@netty_handler//jar",
    "@netty_transport//jar",
    "@netty_transport_native_epoll//jar",
    "//protocols/openflow/api:onos-protocols-openflow-api",
]

TEST_DEPS = TEST + [
    "//core/api:onos-api-tests",
]

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)
