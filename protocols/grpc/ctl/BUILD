COMPILE_DEPS = CORE_DEPS + [
    "//protocols/grpc/api:onos-protocols-grpc-api",
    "//protocols/grpc/proto:onos-protocols-grpc-proto",
    "@io_grpc_grpc_java//core",
    "@io_grpc_grpc_java//netty",
    "@io_netty_netty_handler//jar",
]

osgi_jar(
    deps = COMPILE_DEPS,
)
