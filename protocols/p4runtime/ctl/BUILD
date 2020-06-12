COMPILE_DEPS = CORE_DEPS + KRYO + [
    "//core/store/serializers:onos-core-serializers",
    "//protocols/grpc/api:onos-protocols-grpc-api",
    "//protocols/grpc/ctl:onos-protocols-grpc-ctl",
    "//protocols/p4runtime/api:onos-protocols-p4runtime-api",
    "//protocols/p4runtime/proto:onos-protocols-p4runtime-proto",
    "//protocols/p4runtime/utils:onos-protocols-p4runtime-utils",
    "//deps:com_google_protobuf_protobuf_java",
    "//deps:io_grpc_grpc_api_context",
    "//deps:io_grpc_grpc_netty",
    "//deps:io_grpc_grpc_protobuf_lite",
    "//deps:io_grpc_grpc_stub",
    "@com_google_api_grpc_proto_google_common_protos//jar",
]

TEST_DEPS = TEST + [
    "@minimal_json//jar",
    "//deps:io_grpc_grpc_api_context",
    "//deps:io_grpc_grpc_core_internal",
    "//deps:io_grpc_grpc_core_inprocess",
    "//deps:io_grpc_grpc_protobuf_lite",
]

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)
