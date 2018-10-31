COMPILE_DEPS = CORE_DEPS + KRYO + [
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

BUNDLES = [
    ":onos-drivers-gnmi",
]

osgi_jar(
    resources = glob(["src/main/resources/**"]),
    resources_root = "src/main/resources",
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.drivers.gnmi",
    category = "Drivers",
    description = "Adds support for devices using gNMI protocol based on " +
                  " openconfig models: http://openconfig.net/ .",
    included_bundles = BUNDLES,
    required_apps = [
        "org.onosproject.generaldeviceprovider",
        "org.onosproject.protocols.gnmi",
    ],
    title = "gNMI Drivers",
    url = "http://onosproject.org",
)
