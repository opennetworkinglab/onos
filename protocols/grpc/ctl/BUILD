COMPILE_DEPS = CORE_DEPS + [
    "//protocols/grpc/api:onos-protocols-grpc-api",
    "@io_grpc_grpc_java//core",
    "@io_grpc_grpc_java//netty",
    "@io_grpc_grpc_java//protobuf-lite",
    "@com_google_protobuf//:protobuf_java",
    "@com_google_api_grpc_proto_google_common_protos//jar",
    "@io_netty_netty_handler//jar",
]

osgi_jar(
    deps = COMPILE_DEPS,
)
