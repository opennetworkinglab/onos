BUNDLES = [
    "//protocols/grpc/api:onos-protocols-grpc-api",
    "//protocols/grpc/ctl:onos-protocols-grpc-ctl",
    "//protocols/grpc/utils:onos-protocols-grpc-utils",
    # gRPC dependencies (with patched core)
    "//deps:io_grpc_grpc_api_context",
    "//deps:io_grpc_grpc_core_internal",
    "//deps:io_grpc_grpc_core_inprocess",
    "//deps:io_grpc_grpc_core_util",
    "//deps:io_grpc_grpc_stub",
    "//deps:io_grpc_grpc_netty",
    "//deps:io_grpc_grpc_auth",
    "//deps:io_grpc_grpc_protobuf",
    "//deps:io_grpc_grpc_protobuf_lite",
    "//deps:com_google_protobuf_protobuf_java",
    "@io_perfmark_perfmark_api//jar",
    "@com_google_api_grpc_proto_google_common_protos//jar",
    "@com_google_errorprone_error_prone_annotations//jar",
    "@com_google_auth_google_auth_library_credentials//jar",
    "@io_opencensus_opencensus_api//jar",
    "@io_opencensus_opencensus_contrib_grpc_metrics//jar",
    "@com_google_code_gson_gson//jar",
]

onos_app(
    app_name = "org.onosproject.protocols.grpc",
    category = "Protocol",
    description = "Exposes APIs to operate with gRPC channels",
    included_bundles = BUNDLES,
    title = "gRPC Protocol Subsystem",
    url = "ttp://onosproject.org",
)
