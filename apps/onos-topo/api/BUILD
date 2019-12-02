load("//tools/build/bazel:osgi_java_library.bzl", "osgi_proto_jar")

PROTOS = [
    "@com_github_onosproject_onos_topo//:topo_device_proto",
]

osgi_proto_jar(
    grpc_proto_lib = "@com_github_onosproject_onos_topo//:topo_device_proto",
    proto_libs = PROTOS,
    deps = [
        "@com_google_api_grpc_proto_google_common_protos//jar",
    ],
)
