COMPILE_DEPS = CORE_DEPS + NETTY + JACKSON + KRYO + [
    "//core/common:onos-core-common",
    "//utils/rest:onlab-rest",
    "//core/store/serializers:onos-core-serializers",
    "@netty_transport//jar",
    "@netty_codec//jar",
    "@netty_handler//jar",
    "@netty_transport_native_epoll//jar",
    "@netty_resolver//jar",
    "@commons_math3//jar",
    "//incubator/api:onos-incubator-api",
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
