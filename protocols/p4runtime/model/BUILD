PROTOBUF_VER = "3_2_0"

COMPILE_DEPS = CORE_DEPS + [
    "//protocols/p4runtime/proto:p4_runtime_java_proto",
    "@com_google_protobuf//:protobuf_java",
]

osgi_jar_with_tests(
    deps = COMPILE_DEPS,
)
