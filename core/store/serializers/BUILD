COMPILE_DEPS = CORE_DEPS + KRYO

TEST_DEPS = TEST

osgi_jar_with_tests(
    name = "onos-core-serializers",
    test_deps = TEST_DEPS,
    visibility = ["//visibility:public"],
    deps = COMPILE_DEPS,
)
