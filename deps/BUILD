load("//tools/build/bazel:osgi_java_library.bzl", "wrapped_osgi_jar")

# Wrap protobuf and grpc-related JARs in OSGi-compatible ones, since the
# original ones are built with Bazel and NOT imported via mvn.

wrapped_osgi_jar(
    # Differently from the official "grpc-api" package, here we also include
    # "grpc-context" to solve the OSGI split-brain problem:
    # https://github.com/grpc/grpc-java/issues/2727
    # We use patched Bazel BUILD files to package together api and context (see
    # tools/build/bazel/grpc_workspace.bzl). If you need grpc-context as a
    # compile-time dependency, please use this one.
    name = "io_grpc_grpc_api_context",
    # Name resolver and load balancer providers which are part of the internal
    # package are loaded dynamically, hence this import statement.
    import_packages = "io.grpc.internal,*",
    jar = "@io_grpc_grpc_java//api",
    visibility = ["//visibility:public"],
    deps = [],
)

wrapped_osgi_jar(
    name = "io_grpc_grpc_core_inprocess",
    jar = "@io_grpc_grpc_java//core:inprocess",
    visibility = ["//visibility:public"],
    deps = [
        "@io_grpc_grpc_java//api",
        "@io_grpc_grpc_java//core:internal",
    ],
)

wrapped_osgi_jar(
    name = "io_grpc_grpc_core_internal",
    jar = "@io_grpc_grpc_java//core:internal",
    visibility = ["//visibility:public"],
    deps = [
        "@io_grpc_grpc_java//api",
        "@io_opencensus_opencensus_api//jar",
        "@io_opencensus_opencensus_contrib_grpc_metrics//jar",
        "@io_perfmark_perfmark_api//jar",
    ],
)

wrapped_osgi_jar(
    name = "io_grpc_grpc_core_util",
    jar = "@io_grpc_grpc_java//core:util",
    visibility = ["//visibility:public"],
    deps = [
        "@io_grpc_grpc_java//api",
        "@io_grpc_grpc_java//core:internal",
    ],
)

wrapped_osgi_jar(
    name = "io_grpc_grpc_stub",
    jar = "@io_grpc_grpc_java//stub",
    visibility = ["//visibility:public"],
    deps = ["@io_grpc_grpc_java//api"],
)

wrapped_osgi_jar(
    name = "io_grpc_grpc_netty",
    jar = "@io_grpc_grpc_java//netty",
    visibility = ["//visibility:public"],
    deps = [
        "@io_grpc_grpc_java//api",
        "@io_grpc_grpc_java//core:internal",
        "@io_netty_netty_buffer//jar",
        "@io_netty_netty_codec//jar",
        "@io_netty_netty_codec_http//jar",
        "@io_netty_netty_codec_http2//jar",
        "@io_netty_netty_codec_socks//jar",
        "@io_netty_netty_common//jar",
        "@io_netty_netty_handler//jar",
        "@io_netty_netty_handler_proxy//jar",
        "@io_netty_netty_resolver//jar",
        "@io_netty_netty_transport//jar",
        "@io_perfmark_perfmark_api//jar",
    ],
)

wrapped_osgi_jar(
    name = "io_grpc_grpc_auth",
    jar = "@io_grpc_grpc_java//auth",
    visibility = ["//visibility:public"],
    deps = [
        "@com_google_auth_google_auth_library_credentials//jar",
        "@io_grpc_grpc_java//api",
        "@io_grpc_grpc_java//core:internal",
    ],
)

wrapped_osgi_jar(
    name = "io_grpc_grpc_protobuf",
    jar = "@io_grpc_grpc_java//protobuf",
    visibility = ["//visibility:public"],
    deps = [
        "@com_google_api_grpc_proto_google_common_protos//jar",
        "@com_google_protobuf//:protobuf_java",
        "@com_google_protobuf//:protobuf_java_util",
        "@io_grpc_grpc_java//api",
    ],
)

wrapped_osgi_jar(
    name = "io_grpc_grpc_protobuf_lite",
    jar = "@io_grpc_grpc_java//protobuf-lite",
    visibility = ["//visibility:public"],
    deps = [
        "@com_google_protobuf//:protobuf_java",
        "@io_grpc_grpc_java//api",
    ],
)

wrapped_osgi_jar(
    name = "com_google_protobuf_protobuf_java",
    jar = "@com_google_protobuf//:protobuf_java",
    visibility = ["//visibility:public"],
    deps = [
        "@io_grpc_grpc_java//api",
        "@io_grpc_grpc_java//core:internal",
    ],
)
