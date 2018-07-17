COMPILE_DEPS = CORE_DEPS + JACKSON + KRYO + [
    "//apps/tetopology/api:onos-apps-tetopology-api",
    "//incubator/api:onos-incubator-api",
    "//core/store/serializers:onos-core-serializers",
]

TEST_DEPS = TEST_ADAPTERS + [
    "//utils/osgi:onlab-osgi-tests",
    "//incubator/api:onos-incubator-api-tests",
]

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)
