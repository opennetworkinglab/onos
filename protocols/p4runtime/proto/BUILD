load("//tools/build/bazel:osgi_java_library.bzl", "wrapped_osgi_jar")

java_proto_library(
    name = "p4_runtime_java_proto_proto",
    visibility = ["//visibility:public"],
    deps = ["@p4lang_pi//:p4_runtime_proto"],
)

wrapped_osgi_jar(
    name = "rpc_java_proto-osgi",
    jar = "@google_rpc//:rpc_java_proto",
    visibility = ["//visibility:public"],
    deps = CORE_DEPS + ["@protobuf_java_3_2_0//jar"],
)

wrapped_osgi_jar(
    name = "grpc-core-repkg-1.3.1",
    jar = "@grpc_src_zip_131//:grpc-core-repkg-1.3.1-native",
    visibility = ["//visibility:public"],
    deps = COMPILE,
)

wrapped_osgi_jar(
    name = "p4_runtime_java_proto",
    jar = ":p4_runtime_java_proto_proto",
    visibility = ["//visibility:public"],
    deps = CORE_DEPS + [
        "@protobuf_java_3_2_0//jar",
        "@google_rpc//:rpc_java_proto",
        ":grpc-core-repkg-1.3.1",
    ],
)
