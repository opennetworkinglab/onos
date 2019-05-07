load("//tools/build/bazel:osgi_java_library.bzl", "osgi_proto_jar")

PROTOS = [
    "@com_github_p4lang_p4runtime//:" + p
    for p in [
        "p4info_proto",
        "p4types_proto",
        "p4data_proto",
        "p4runtime_proto",
    ]
]

osgi_proto_jar(
    grpc_proto_lib = "@com_github_p4lang_p4runtime//:p4runtime_proto",
    proto_libs = PROTOS,
    deps = [
        "@com_google_api_grpc_proto_google_common_protos//jar",
    ],
)
