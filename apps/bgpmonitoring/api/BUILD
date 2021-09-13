COMPILE_DEPS = CORE_DEPS + [
    "//core/store/serializers:onos-core-serializers",
    "//protocols/bgp/bgpio:onos-protocols-bgp-bgpio",
    "@io_netty_netty//jar",
]

osgi_jar_with_tests(
    test_deps = TEST_ADAPTERS,
    deps = COMPILE_DEPS,
)
