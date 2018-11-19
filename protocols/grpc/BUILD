load("//tools/build/bazel:osgi_java_library.bzl", "wrapped_osgi_jar")

BUNDLES = [
    "//protocols/grpc/proto:onos-protocols-grpc-proto",
    "//protocols/grpc/api:onos-protocols-grpc-api",
    "//protocols/grpc/ctl:onos-protocols-grpc-ctl",
    # gRPC dependencies (with patched core)
    ":grpc-core-repkg",
    "@runtime_grpc_stub//jar",
    "@runtime_grpc_netty//jar",
    "@runtime_grpc_auth//jar",
    "@runtime_grpc_protobuf//jar",
    "@runtime_grpc_protobuf_lite//jar",
    "@runtime_protobuf//jar",
    "@com_google_api_grpc_proto_google_common_protos//jar",
    "@com_google_errorprone_error_prone_annotations//jar",
    "@com_google_auth_google_auth_library_credentials//jar",
    "@io_opencensus_opencensus_api//jar",
    "@io_opencensus_opencensus_contrib_grpc_metrics//jar",
    "@com_google_code_gson_gson//jar",
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

onos_app(
    app_name = "org.onosproject.protocols.grpc",
    category = "Protocol",
    description = "Exposes APIs to operate with gRPC channels",
    included_bundles = BUNDLES,
    title = "gRPC Protocol Subsystem",
    url = "ttp://onosproject.org",
)

wrapped_osgi_jar(
    name = "grpc-core-repkg",
    jar = "@io_grpc_grpc_java//core",
    visibility = ["//visibility:public"],
    deps = ["@io_opencensus_opencensus_api//jar"],
)
