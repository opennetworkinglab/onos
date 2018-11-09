COMPILE_DEPS = CORE_DEPS + KRYO + [
    "//apps/tetopology/api:onos-apps-tetopology-api",
    "//apps/tetunnel/api:onos-apps-tetunnel-api",
    "//apps/tunnel/api:onos-apps-tunnel-api",
    "//core/store/serializers:onos-core-serializers",
]

TEST_DEPS = TEST_ADAPTERS + [
    "//utils/osgi:onlab-osgi-tests",
    "//apps/tunnel/api:onos-apps-tunnel-api-tests",
]

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)
