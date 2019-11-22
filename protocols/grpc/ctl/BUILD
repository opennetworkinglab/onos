COMPILE_DEPS = CORE_DEPS + [
    "//protocols/grpc/api:onos-protocols-grpc-api",
    "//protocols/grpc/proto:onos-protocols-grpc-proto",
    "//deps:io_grpc_grpc_api_context",
    "//deps:io_grpc_grpc_core_internal",
    "//deps:io_grpc_grpc_netty",
    "//deps:io_grpc_grpc_protobuf_lite",
    "//deps:com_google_protobuf_protobuf_java",
    "@com_google_api_grpc_proto_google_common_protos//jar",
    "@io_netty_netty_handler//jar",
    "@io_grpc_grpc_java//protobuf-lite",
    "@com_google_protobuf//:protobuf_java",
]

osgi_jar(
    deps = COMPILE_DEPS,
)
