load("//tools/build/bazel:osgi_java_library.bzl", "wrapped_osgi_jar")
load("@io_grpc_grpc_java//:java_grpc_library.bzl", "java_grpc_library")

wrapped_osgi_jar(
    name = "gnmi_java_grpc",
    jar = ":gnmi_java_grpc_native",
    deps = [
        "@io_grpc_grpc_java//stub",
        "@io_grpc_grpc_java//core",
        "@io_grpc_grpc_java//protobuf"
    ],
    visibility = ["//visibility:public"],
)

wrapped_osgi_jar(
    name = "gnmi_java_proto",
    jar = ":gnmi_java_proto_native",
    deps = [
        "@com_google_protobuf//:protobuf_java",
    ],
    visibility = ["//visibility:public"],
)

java_proto_library(
    name = "gnmi_java_proto_native",
    deps = [":gnmi_proto"],
    visibility = ["//visibility:public"],
)

java_grpc_library(
    name = "gnmi_java_grpc_native",
    srcs = [":gnmi_proto"],
    deps = [":gnmi_java_proto_native"],
)

proto_library(
    name = "gnmi_proto",
    srcs = ["src/main/proto/gnmi.proto"],
    deps = [
        "@com_google_protobuf//:any_proto",
        "@com_google_protobuf//:descriptor_proto",
    ]
)
