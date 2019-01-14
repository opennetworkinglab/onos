COMPILE_DEPS = CORE_DEPS + NETTY + JACKSON + [
    "//core/common:onos-core-common",
    "@openflowj//jar",
    "@io_netty_netty_transport//jar",
]

TEST_DEPS = TEST + [
    "//core/api:onos-api-tests",
]

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    visibility = ["//visibility:public"],
    deps = COMPILE_DEPS,
)
