load("//tools/build/bazel:osgi_java_library.bzl", "wrapped_osgi_jar")
load("@io_grpc_grpc_java//:java_grpc_library.bzl", "java_grpc_library")

wrapped_osgi_jar(
    name = "p4config_java_proto",
    jar = "@com_github_p4lang_pi//:p4config_java_proto_native",
    deps = [
        "@com_google_protobuf//:protobuf_java",
    ],
    visibility = ["//visibility:public"],
)

wrapped_osgi_jar(
    name = "p4data_java_proto",
    jar = "@com_github_p4lang_p4runtime//:p4data_java_proto_native",
    deps = [
        "@com_google_protobuf//:protobuf_java",
    ],
    visibility = ["//visibility:public"],
)

wrapped_osgi_jar(
    name = "p4info_java_proto",
    jar = "@com_github_p4lang_p4runtime//:p4info_java_proto_native",
    deps = [
        "@com_google_protobuf//:protobuf_java",
    ],
    visibility = ["//visibility:public"],
)

wrapped_osgi_jar(
    name = "p4runtime_java_proto",
    jar = "@com_github_p4lang_p4runtime//:p4runtime_java_proto_native",
    deps = [
        "@com_google_protobuf//:protobuf_java",
    ],
    visibility = ["//visibility:public"],
)

wrapped_osgi_jar(
    name = "p4runtime_java_grpc",
    jar = "@com_github_p4lang_p4runtime//:p4runtime_java_grpc_native",
    deps = [
        "@io_grpc_grpc_java//stub",
        "@io_grpc_grpc_java//core",
        "@io_grpc_grpc_java//protobuf",
    ],
    visibility = ["//visibility:public"],
)

wrapped_osgi_jar(
    name = "status_java_proto",
    jar = "@com_github_googleapis//:status_java_proto_native",
    deps = [
        "@com_google_protobuf//:protobuf_java",
    ],
    visibility = ["//visibility:public"],
)
