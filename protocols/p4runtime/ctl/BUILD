COMPILE_DEPS = CORE_DEPS + KRYO + [
    "//core/store/serializers:onos-core-serializers",
    "//protocols/grpc/api:onos-protocols-grpc-api",
    "//protocols/p4runtime/api:onos-protocols-p4runtime-api",
    "//protocols/p4runtime/proto:p4config_java_proto",
    "//protocols/p4runtime/proto:p4data_java_proto",
    "//protocols/p4runtime/proto:p4info_java_proto",
    "//protocols/p4runtime/proto:p4runtime_java_grpc",
    "//protocols/p4runtime/proto:p4runtime_java_proto",
    "//protocols/p4runtime/proto:status_java_proto",
    "@com_google_protobuf//:protobuf_java",
    "@io_grpc_grpc_java//context",
    "@io_grpc_grpc_java//core",
    "@io_grpc_grpc_java//netty",
    "@io_grpc_grpc_java//stub",
    "@io_grpc_grpc_java_core_repkg//:internal",
]

TEST_DEPS = TEST + [
    "@minimal_json//jar",
    "@io_grpc_grpc_java//core:inprocess",
    "@io_grpc_grpc_java//protobuf-lite",
]

osgi_jar_with_tests(
    deps = COMPILE_DEPS,
    test_deps = TEST_DEPS,
)
