COMPILE_DEPS = CORE_DEPS + JACKSON

TEST_DEPS = TEST_ADAPTERS

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    visibility = ["//visibility:public"],
    deps = COMPILE_DEPS,
)
