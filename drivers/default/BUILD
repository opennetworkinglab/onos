COMPILE_DEPS = CORE_DEPS + JACKSON + KRYO + [
    '@openflowj//jar',
    '//protocols/openflow/api:onos-protocols-openflow-api',
    '//core/store/serializers:onos-core-serializers',
]

TEST_DEPS = TEST_ADAPTERS + [
    '//core/api:onos-api-tests',
]

osgi_jar_with_tests (
    deps = COMPILE_DEPS,
    test_deps = TEST_DEPS,
    #resources_root = 'src/main/resources',
    resources = glob(['src/main/resources/**']),
    visibility = ["//visibility:public"],
)
