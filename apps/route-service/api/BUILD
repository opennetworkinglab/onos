COMPILE_DEPS = CORE_DEPS + JACKSON

TEST_DEPS = TEST + [
    '//core/api:onos-api-tests',
]

osgi_jar_with_tests (
    deps = COMPILE_DEPS,
    test_deps = TEST_DEPS,
)
