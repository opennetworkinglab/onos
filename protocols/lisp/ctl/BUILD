COMPILE_DEPS = CORE_DEPS + NETTY + [
    "@io_netty_netty_codec//jar",
    "@io_netty_netty_transport//jar",
    "@io_netty_netty_transport_native_epoll//jar",
    "@io_netty_netty_handler//jar",
    "@io_netty_netty_resolver//jar",
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
