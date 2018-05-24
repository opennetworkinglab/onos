GRPC_VER = "1_3_1"

COMPILE_DEPS = CORE_DEPS + [
    "@grpc_core_" + GRPC_VER + "//jar",
]

TEST_DEPS = TEST + [
    "//core/api:onos-api-tests",
]

osgi_jar_with_tests (
    deps = COMPILE_DEPS,
    test_deps = TEST_DEPS,
)
