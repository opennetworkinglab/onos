COMPILE_DEPS = CORE_DEPS + KRYO + JACKSON + [
    "//core/store/serializers:onos-core-serializers",
    "//apps/inbandtelemetry/api:onos-apps-inbandtelemetry-api",
]

TEST_DEPS = TEST_REST

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)
