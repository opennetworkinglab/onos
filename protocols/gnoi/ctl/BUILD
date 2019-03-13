COMPILE_DEPS = CORE_DEPS + KRYO + [
    "//protocols/gnoi/api:onos-protocols-gnoi-api",
    "//protocols/gnoi/stub:onos-protocols-gnoi-stub",
    "//protocols/grpc/api:onos-protocols-grpc-api",
    "//protocols/grpc/ctl:onos-protocols-grpc-ctl",
    "//protocols/grpc:grpc-core",
    "@com_google_protobuf//:protobuf_java",
    "@io_grpc_grpc_java//netty",
    "@io_grpc_grpc_java//protobuf-lite",
    "@io_grpc_grpc_java//stub",
    "@com_google_api_grpc_proto_google_common_protos//jar",
]

TEST_DEPS = TEST + [
    "@minimal_json//jar",
    "@io_grpc_grpc_java//core:inprocess",
    "@io_grpc_grpc_java//protobuf-lite",
]

osgi_jar_with_tests(
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)
