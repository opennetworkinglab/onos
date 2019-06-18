COMPILE_DEPS = CORE_DEPS + [
    "//protocols/grpc/api:onos-protocols-grpc-api",
    "//deps:io_grpc_grpc_api_context",
]

TEST_DEPS = TEST + [
    "//core/api:onos-api-tests",
]

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)
