load("//tools/build/bazel:osgi_java_library.bzl", "osgi_proto_jar")

osgi_proto_jar(
    grpc_proto_lib = ":dummy_proto",
    proto_libs = [":dummy_proto"],
)

proto_library(
    name = "dummy_proto",
    srcs = ["dummy.proto"],
)
