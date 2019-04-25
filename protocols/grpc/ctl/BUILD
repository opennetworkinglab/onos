COMPILE_DEPS = CORE_DEPS + [
    "//protocols/grpc/api:onos-protocols-grpc-api",
    "//lib:io_grpc_grpc_core_context",
    "//lib:io_grpc_grpc_netty",
    "//lib:io_grpc_grpc_protobuf_lite",
    "//lib:com_google_protobuf_protobuf_java",
    "@com_google_api_grpc_proto_google_common_protos//jar",
    "@io_netty_netty_handler//jar",
]

osgi_jar(
    deps = COMPILE_DEPS,
)
