COMPILE_DEPS = CORE_DEPS + JACKSON + [
    "//protocols/isis/api:onos-protocols-isis-api",
    "//protocols/isis/ctl:onos-protocols-isis-ctl",
]

osgi_jar_with_tests(
    test_deps = TEST_ADAPTERS,
    deps = COMPILE_DEPS,
)
