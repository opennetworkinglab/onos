load("//tools/build/bazel:osgi_java_library.bzl", "wrapped_osgi_jar")

BUNDLES = [
    "@kafka_clients//jar",
    "@influxdb_java//jar",
    "@commons_codec//jar",
    "@retrofit//jar",
    "@okhttp//jar",
    "@logging_interceptor//jar",
    "@okio//jar",
    "@moshi//jar",
    "@converter_moshi//jar",
    # prometheus deps
    "@simpleclient//jar",
    "@simpleclient_common//jar",
    "@simpleclient_hotspot//jar",
    "@simpleclient_servlet//jar",
    # gRPC dependencies (with patched core)
    "//lib:io_grpc_grpc_core_context",
    "//lib:io_grpc_grpc_stub",
    "//lib:io_grpc_grpc_netty",
    "//lib:io_grpc_grpc_auth",
    "//lib:io_grpc_grpc_protobuf",
    "//lib:io_grpc_grpc_protobuf_lite",
    "//lib:com_google_protobuf_protobuf_java",
    "@com_google_api_grpc_proto_google_common_protos//jar",
    "@com_google_errorprone_error_prone_annotations//jar",
    "@com_google_auth_google_auth_library_credentials//jar",
    "@io_opencensus_opencensus_api//jar",
    "@io_opencensus_opencensus_contrib_grpc_metrics//jar",
    "@com_google_code_gson_gson//jar",
    "//apps/openstacktelemetry/api:onos-apps-openstacktelemetry-api",
    "//apps/openstacktelemetry/app:onos-apps-openstacktelemetry-app",
    "//apps/openstacktelemetry/web:onos-apps-openstacktelemetry-web",
]

onos_app(
    category = "Integration",
    description = "SONA Openstack Telemetry Application.",
    included_bundles = BUNDLES,
    required_apps = [
        "org.onosproject.openstacknetworking",
    ],
    title = "OpenStack Telemetry Application",
    url = "https://wiki.onosproject.org/display/ONOS/SONA%3A+DC+Network+Virtualization",
)
