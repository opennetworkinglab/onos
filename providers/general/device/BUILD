COMPILE_DEPS = CORE_DEPS + JACKSON + [
    "//protocols/gnmi/stub:onos-protocols-gnmi-stub",
    "//protocols/gnmi/api:onos-protocols-gnmi-api",
    "//deps:com_google_protobuf_protobuf_java",
    "//protocols/grpc/api:onos-protocols-grpc-api",
]

osgi_jar_with_tests(
    test_deps = TEST_ADAPTERS,
    deps = COMPILE_DEPS,
)
