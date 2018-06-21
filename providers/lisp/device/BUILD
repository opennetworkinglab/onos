COMPILE_DEPS = CORE_DEPS + [
    "//protocols/lisp/api:onos-protocols-lisp-api",
]

TEST_DEPS = TEST_ADAPTERS + [
    "//protocols/lisp/api:onos-protocols-lisp-api-tests",
]

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)
