COMPILE_DEPS = CORE_DEPS + [
    "//apps/onos-topo/api:onos-apps-onos-topo-api",
    "//core/store/serializers:onos-core-serializers",
    "@io_grpc_grpc_java//api",
    "@io_grpc_grpc_java//netty",
    "@io_grpc_grpc_java//stub",
]

osgi_jar_with_tests(
    test_deps = TEST_ADAPTERS,
    deps = COMPILE_DEPS,
)
