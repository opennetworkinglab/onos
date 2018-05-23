COMPILE_DEPS = CORE_DEPS + JACKSON + METRICS + KRYO + [
    "//incubator/store:onos-incubator-store",
    "//incubator/api:onos-incubator-api",
    "//core/common:onos-core-common",
]

TEST_DEPS = TEST_REST + [
    "//incubator/api:onos-incubator-api-tests",
]

osgi_jar_with_tests(
    exclude_tests = [
        "org.onosproject.incubator.net.virtual.impl.VirtualNetworkIntentManagerTest",
    ],
    test_deps = TEST_DEPS,
    visibility = ["//visibility:public"],
    deps = COMPILE_DEPS,
)
