COMPILE_DEPS = CORE_DEPS + KRYO + [
    '//incubator/api:onos-incubator-api',
]

TEST_DEPS = TEST

osgi_jar_with_tests (
    name = 'onos-core-serializers',
    deps = COMPILE_DEPS,
    test_deps = TEST_DEPS,
    visibility = ["//visibility:public"],
)
