GRPC_VER = "1_3_1"

COMPILE_DEPS = CORE_DEPS + [
    "@grpc_core_" + GRPC_VER + "//jar",
]

TEST_DEPS = TEST + [
    "//core/api:onos-api-tests",
]

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)
