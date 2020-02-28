COMPILE_DEPS = CORE_DEPS + JACKSON + METRICS + KRYO + [
    "//core/store/serializers:onos-core-serializers",
]

TEST_DEPS = TEST + ["//core/api:onos-api-tests"]

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    visibility = ["//visibility:public"],
    deps = COMPILE_DEPS,
)
