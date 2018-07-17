COMPILE_DEPS = CORE_DEPS + JACKSON + REST + [
    "@jersey_client//jar",
    "@jersey_server//jar",
]

osgi_jar_with_tests(
    test_deps = TEST_REST,
    deps = COMPILE_DEPS,
)
