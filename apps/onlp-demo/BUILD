COMPILE_DEPS = CORE_DEPS + JACKSON + [
    "@com_google_protobuf//:protobuf_java",
    "@io_grpc_grpc_java//core",
    "@io_grpc_grpc_java//netty",
    "@io_grpc_grpc_java//stub",
    "//core/store/serializers:onos-core-serializers",
    "//protocols/gnmi/stub:onos-protocols-gnmi-stub",
    "//protocols/gnmi/api:onos-protocols-gnmi-api",
    "//protocols/grpc/api:onos-protocols-grpc-api",
    "//protocols/grpc/proto:onos-protocols-grpc-proto",
]

osgi_jar_with_tests(
    deps = COMPILE_DEPS,
)

onos_app(
    category = "GUI",
    description = "Provides a GUI overlay for displaying ONLP device management information.",
    required_apps = [
        "org.onosproject.protocols.gnmi",
    ],
    title = "ONLP device demo",
    url = "http://onosproject.org",
)
