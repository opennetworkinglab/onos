COMPILE_DEPS = CORE_DEPS + NETTY + [
    "@netty_codec//jar",
    "@netty_transport//jar",
    "@netty_transport_native_epoll//jar",
    "@netty_handler//jar",
    "@netty_resolver//jar",
    "@concurrent_trees//jar",
    "//protocols/lisp/api:onos-protocols-lisp-api",
    "//protocols/lisp/msg:onos-protocols-lisp-msg",
]

TEST_DEPS = TEST_ADAPTERS + [
    "//utils/osgi:onlab-osgi-tests",
    "//core/api:onos-api-tests",
    "//protocols/lisp/api:onos-protocols-lisp-api-tests",
]

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)
