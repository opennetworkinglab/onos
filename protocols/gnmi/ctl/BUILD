COMPILE_DEPS = CORE_DEPS + KRYO + [
    "//protocols/gnmi/api:onos-protocols-gnmi-api",
    "//protocols/gnmi/stub:onos-protocols-gnmi-stub",
    "//protocols/grpc/api:onos-protocols-grpc-api",
    "//protocols/grpc/ctl:onos-protocols-grpc-ctl",
    "//lib:io_grpc_grpc_core_context",
    "//lib:com_google_protobuf_protobuf_java",
    "//lib:io_grpc_grpc_netty",
    "//lib:io_grpc_grpc_protobuf_lite",
    "//lib:io_grpc_grpc_stub",
    "@com_google_api_grpc_proto_google_common_protos//jar",
]

TEST_DEPS = TEST + [
    "@minimal_json//jar",
    "//lib:io_grpc_grpc_core_context",
    "//lib:io_grpc_grpc_protobuf_lite",
]

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)
