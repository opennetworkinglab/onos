COMPILE_DEPS = CORE_DEPS + KRYO + [
    "//core/store/serializers:onos-core-serializers",
    "//pipelines/basic:onos-pipelines-basic",
    "//apps/inbandtelemetry/api:onos-apps-inbandtelemetry-api",
]

TEST_DEPS = TEST_ADAPTERS + JACKSON

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)
