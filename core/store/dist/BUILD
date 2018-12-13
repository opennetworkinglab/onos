COMPILE_DEPS = CORE_DEPS + NETTY + JACKSON + KRYO + [
    "//core/common:onos-core-common",
    "//utils/rest:onlab-rest",
    "//core/store/serializers:onos-core-serializers",
    "@io_netty_netty_transport//jar",
    "@io_netty_netty_transport_native_epoll//jar",
    "@io_netty_netty_transport_native_unix_common//jar",
    "@io_netty_netty_codec//jar",
    "@io_netty_netty_handler//jar",
    "@io_netty_netty_resolver//jar",
    "@commons_math3//jar",
]

TEST_DEPS = TEST + [
    "//core/api:onos-api-tests",
    "//core/common:onos-core-common-tests",
]

osgi_jar_with_tests(
    name = "onos-core-dist",
    exclude_tests = [
        "org.onosproject.store.link.impl.ECLinkStoreTest",
        "org.onosproject.store.flow.impl.DistributedFlowRuleStoreTest",
    ],
    test_deps = TEST_DEPS,
    visibility = ["//visibility:public"],
    deps = COMPILE_DEPS,
)
