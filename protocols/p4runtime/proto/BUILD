load("//tools/build/bazel:osgi_java_library.bzl", "osgi_proto_jar")

PROTOS = [
    "@com_github_p4lang_p4runtime//:" + p for p in [
        "p4info_proto",
        "p4types_proto",
        "p4data_proto",
        "p4runtime_proto",
]] + [
    "@com_github_p4lang_pi//:p4config_proto"
]

osgi_proto_jar(
    proto_libs = PROTOS,
    grpc_proto_lib = "@com_github_p4lang_p4runtime//:p4runtime_proto",
    deps = [
        "@com_google_api_grpc_proto_google_common_protos//jar",
    ]
)
