
BUNDLES = [
    "//protocols/grpc/proto:dummy_java_grpc",
    "//protocols/grpc/proto:dummy_java_proto",
    "//protocols/grpc/api:onos-protocols-grpc-api",
    "//protocols/grpc/ctl:onos-protocols-grpc-ctl",
    # gRPC dependencies
    "@io_grpc_grpc_java//core",
    "@io_grpc_grpc_java//stub",
    "@io_grpc_grpc_java//netty",
    "@io_grpc_grpc_java//auth",
    "@io_grpc_grpc_java//protobuf",
    "@io_grpc_grpc_java//protobuf-lite",
    "@com_google_protobuf//:protobuf_java",
    # Lazily adding all netty-related packages.
    # Some of them might not be necessary.
    "@io_netty_netty//jar",
    "@io_netty_netty_buffer//jar",
    "@io_netty_netty_codec//jar",
    "@io_netty_netty_codec_http//jar",
    "@io_netty_netty_codec_http2//jar",
    "@io_netty_netty_common//jar",
    "@io_netty_netty_handler//jar",
    "@io_netty_netty_transport//jar",
    "@io_netty_netty_transport_native_epoll//jar",
    "@io_netty_netty_resolver//jar",
]

onos_app (
    app_name = "org.onosproject.protocols.grpc",
    title = "RPC Protocol Subsystem",
    category = "rotocol",
    url = "ttp://onosproject.org",
    description = "Exposes APIs to operate with gRPC channels",
    included_bundles = BUNDLES,
)
