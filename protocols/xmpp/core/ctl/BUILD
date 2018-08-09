COMPILE_DEPS = CORE_DEPS + [
    "@io_netty_netty_common//jar",
    "@io_netty_netty_transport//jar",
    "@io_netty_netty_transport_native_epoll//jar",
    "@io_netty_netty_buffer//jar",
    "@io_netty_netty_codec//jar",
    "@tinder_xmpp//jar",
    "@stax2_api//jar",
    "@aalto_xml//jar",
    "@concurrent_hashmap//jar",
    "@gnu_idn//jar",
    "//protocols/xmpp/core/api:onos-protocols-xmpp-core-api",
]

TEST_DEPS = TEST + [
    "//core/api:onos-api-tests",
]

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)
