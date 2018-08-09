COMPILE_DEPS = CORE_DEPS + [
    "@io_grpc_grpc_java//core",
]

TEST_DEPS = TEST + [
    "//core/api:onos-api-tests",
]

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)
