COMPILE_DEPS = CORE_DEPS + JACKSON + METRICS + KRYO + [
    '//incubator/store:onos-incubator-store',
    '//incubator/api:onos-incubator-api',
    '//core/common:onos-core-common',
]

TEST_DEPS = TEST_REST + [
    '//incubator/api:onos-incubator-api-tests',
]

osgi_jar_with_tests (
    deps = COMPILE_DEPS,
    test_deps = TEST_DEPS,
    exclude_tests = [
        'org.onosproject.incubator.net.virtual.impl.VirtualNetworkIntentManagerTest',
    ],
    visibility = ["//visibility:public"],
)
