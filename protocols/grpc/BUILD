load("//tools/build/bazel:osgi_java_library.bzl", "wrapped_osgi_jar")

BUNDLES = [
    "//protocols/grpc/proto:onos-protocols-grpc-proto",
    "//protocols/grpc/api:onos-protocols-grpc-api",
    "//protocols/grpc/ctl:onos-protocols-grpc-ctl",
    # gRPC dependencies (with patched core)
    ":grpc-bundle-core",
    ":grpc-bundle-stub",
    ":grpc-bundle-netty",
    ":grpc-bundle-auth",
    ":grpc-bundle-protobuf",
    ":grpc-bundle-protobuf-lite",
    ":protobuf-java-bundle",
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

# Wrap protobuf and grpc-related JARs in OSGi-compatible ones, since the
# original one are built with Bazel and NOT imported via mvn.

# FIXME: consider moving these rules somewhere else as other apps depend on it
#  (e.g. openstacktelemetry and kafka-integration) but they don't directly
#  depend on any onos-protocols-grpc-* module.

wrapped_osgi_jar(
    # Differently from the official "grpc-core" package, here we also include
    # "grpc-context" to solve the OSGI split-brain problem:
    # https://github.com/grpc/grpc-java/issues/2727
    # We use patched Bazel BUILD files to package together core and context (see
    # tools/build/bazel/grpc_workspace.bzl). If you need grpc-context as a
    # compile-time dependency, please use this one.
    name = "grpc-bundle-core",
    jar = "@io_grpc_grpc_java//core",
    visibility = ["//visibility:public"],
    deps = [],
)

wrapped_osgi_jar(
    name = "grpc-bundle-stub",
    jar = "@io_grpc_grpc_java//stub",
    visibility = ["//visibility:public"],
    deps = [],
)

wrapped_osgi_jar(
    name = "grpc-bundle-netty",
    jar = "@io_grpc_grpc_java//netty",
    visibility = ["//visibility:public"],
    deps = [],
)

wrapped_osgi_jar(
    name = "grpc-bundle-auth",
    jar = "@io_grpc_grpc_java//auth",
    visibility = ["//visibility:public"],
    deps = [],
)

wrapped_osgi_jar(
    name = "grpc-bundle-protobuf",
    jar = "@io_grpc_grpc_java//protobuf",
    visibility = ["//visibility:public"],
    deps = [],
)

wrapped_osgi_jar(
    name = "grpc-bundle-protobuf-lite",
    jar = "@io_grpc_grpc_java//protobuf-lite",
    visibility = ["//visibility:public"],
    deps = [],
)

wrapped_osgi_jar(
    name = "protobuf-java-bundle",
    jar = "@com_google_protobuf//:protobuf_java",
    visibility = ["//visibility:public"],
    deps = [],
)
