COMPILE_DEPS = CORE_DEPS + KRYO + [
    "//core/store/serializers:onos-core-serializers",
    "//protocols/grpc/api:onos-protocols-grpc-api",
    "//protocols/grpc/ctl:onos-protocols-grpc-ctl",
    "//protocols/p4runtime/api:onos-protocols-p4runtime-api",
    "//protocols/p4runtime/proto:onos-protocols-p4runtime-proto",
    "@com_google_protobuf//:protobuf_java",
    "//protocols/grpc:grpc-core-repkg",
    "@io_grpc_grpc_java//netty",
    "@io_grpc_grpc_java//protobuf-lite",
    "@io_grpc_grpc_java//stub",
    "@com_google_api_grpc_proto_google_common_protos//jar",
]

TEST_DEPS = TEST + [
    "@minimal_json//jar",
    "@io_grpc_grpc_java//core:inprocess",
    "@io_grpc_grpc_java//protobuf-lite",
]

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)
