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
    generate_pom = True,
    # Name resolver and load balancer providers which are part of the internal
    # package are loaded dynamically, hence this import statement.
    import_packages = "io.grpc.internal,*",
    jar = "@io_grpc_grpc_java//api",
    visibility = ["//visibility:public"],
    deps = [],
)

wrapped_osgi_jar(
    name = "io_grpc_grpc_core_inprocess",
    generate_pom = True,
    jar = "@io_grpc_grpc_java//core:inprocess",
    visibility = ["//visibility:public"],
    deps = [],
)

wrapped_osgi_jar(
    name = "io_grpc_grpc_core_internal",
    generate_pom = True,
    jar = "@io_grpc_grpc_java//core:internal",
    visibility = ["//visibility:public"],
    deps = [],
)

wrapped_osgi_jar(
    name = "io_grpc_grpc_core_util",
    generate_pom = True,
    jar = "@io_grpc_grpc_java//core:util",
    visibility = ["//visibility:public"],
    deps = [],
)

wrapped_osgi_jar(
    name = "io_grpc_grpc_stub",
    generate_pom = True,
    jar = "@io_grpc_grpc_java//stub",
    visibility = ["//visibility:public"],
    deps = [],
)

wrapped_osgi_jar(
    name = "io_grpc_grpc_netty",
    generate_pom = True,
    jar = "@io_grpc_grpc_java//netty",
    visibility = ["//visibility:public"],
    deps = [],
)

wrapped_osgi_jar(
    name = "io_grpc_grpc_auth",
    generate_pom = True,
    jar = "@io_grpc_grpc_java//auth",
    visibility = ["//visibility:public"],
    deps = [],
)

wrapped_osgi_jar(
    name = "io_grpc_grpc_protobuf",
    generate_pom = True,
    jar = "@io_grpc_grpc_java//protobuf",
    visibility = ["//visibility:public"],
    deps = [],
)

wrapped_osgi_jar(
    name = "io_grpc_grpc_protobuf_lite",
    generate_pom = True,
    jar = "@io_grpc_grpc_java//protobuf-lite",
    visibility = ["//visibility:public"],
    deps = [],
)

wrapped_osgi_jar(
    name = "com_google_protobuf_protobuf_java",
    generate_pom = True,
    jar = "@com_google_protobuf//:protobuf_java",
    visibility = ["//visibility:public"],
    deps = [],
)
