COMPILE_DEPS = CORE_DEPS + JACKSON + [
    "//deps:com_google_protobuf_protobuf_java",
    "//deps:io_grpc_grpc_api_context",
    "//deps:io_grpc_grpc_netty",
    "//deps:io_grpc_grpc_stub",
    "//core/store/serializers:onos-core-serializers",
    "//protocols/gnmi/stub:onos-protocols-gnmi-stub",
    "//protocols/gnmi/api:onos-protocols-gnmi-api",
    "//protocols/grpc/api:onos-protocols-grpc-api",
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
