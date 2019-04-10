load("//tools/build/bazel:osgi_java_library.bzl", "wrapped_osgi_jar")
load("//tools/build/bazel:variables.bzl", "GRPC_JAVA_VERSION")
load("//tools/build/bazel:variables.bzl", "PROTOBUF_VERSION")

BUNDLES = [
    "//protocols/grpc/api:onos-protocols-grpc-api",
    "//protocols/grpc/ctl:onos-protocols-grpc-ctl",
    "//protocols/grpc/utils:onos-protocols-grpc-utils",
    # gRPC dependencies (with patched core)
    ":grpc-core",
    ":grpc-stub",
    ":grpc-netty",
    ":grpc-auth",
    ":grpc-protobuf",
    ":grpc-protobuf-lite",
    ":protobuf-java",
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
# original ones are built with Bazel and NOT imported via mvn.

# FIXME: consider moving these rules somewhere else as other apps depend on it
#  (e.g. openstacktelemetry and kafka-integration) but they don't directly
#  depend on any onos-protocols-grpc-* module.

GRPC_GROUP_ID = "io.grpc"

PROTOBUF_GROUP_ID = "com.google.protobuf"

wrapped_osgi_jar(
    # Differently from the official "grpc-core" package, here we also include
    # "grpc-context" to solve the OSGI split-brain problem:
    # https://github.com/grpc/grpc-java/issues/2727
    # We use patched Bazel BUILD files to package together core and context (see
    # tools/build/bazel/grpc_workspace.bzl). If you need grpc-context as a
    # compile-time dependency, please use this one.
    name = "grpc-core",
    group = GRPC_GROUP_ID,
    jar = "@io_grpc_grpc_java//core",
    version = GRPC_JAVA_VERSION,
    visibility = ["//visibility:public"],
    deps = [],
)

wrapped_osgi_jar(
    name = "grpc-stub",
    group = GRPC_GROUP_ID,
    jar = "@io_grpc_grpc_java//stub",
    version = GRPC_JAVA_VERSION,
    visibility = ["//visibility:public"],
    deps = [],
)

wrapped_osgi_jar(
    name = "grpc-netty",
    group = GRPC_GROUP_ID,
    jar = "@io_grpc_grpc_java//netty",
    version = GRPC_JAVA_VERSION,
    visibility = ["//visibility:public"],
    deps = [],
)

wrapped_osgi_jar(
    name = "grpc-auth",
    group = GRPC_GROUP_ID,
    jar = "@io_grpc_grpc_java//auth",
    version = GRPC_JAVA_VERSION,
    visibility = ["//visibility:public"],
    deps = [],
)

wrapped_osgi_jar(
    name = "grpc-protobuf",
    group = GRPC_GROUP_ID,
    jar = "@io_grpc_grpc_java//protobuf",
    version = GRPC_JAVA_VERSION,
    visibility = ["//visibility:public"],
    deps = [],
)

wrapped_osgi_jar(
    name = "grpc-protobuf-lite",
    group = GRPC_GROUP_ID,
    jar = "@io_grpc_grpc_java//protobuf-lite",
    version = GRPC_JAVA_VERSION,
    visibility = ["//visibility:public"],
    deps = [],
)

wrapped_osgi_jar(
    name = "protobuf-java",
    group = PROTOBUF_GROUP_ID,
    jar = "@com_google_protobuf//:protobuf_java",
    version = PROTOBUF_VERSION,
    visibility = ["//visibility:public"],
    deps = [],
)
