COMPILE_DEPS = CORE_DEPS + KRYO + [
    "@com_google_protobuf//:protobuf_java",
    "@io_grpc_grpc_java//core",
    "@io_grpc_grpc_java//netty",
    "@io_grpc_grpc_java//stub",
    "//core/store/serializers:onos-core-serializers",
    "//protocols/gnmi/stub:gnmi_java_grpc",
    "//protocols/gnmi/stub:gnmi_java_proto",
    "//protocols/grpc/api:onos-protocols-grpc-api",
    "//protocols/grpc/proto:onos-protocols-grpc-proto",
]

BUNDLES = [
    ":onos-drivers-gnmi",
]

osgi_jar (
    deps = COMPILE_DEPS,
    resources = glob(["src/main/resources/**"]),
    resources_root = "src/main/resources",
)

onos_app (
    app_name = "org.onosproject.drivers.gnmi",
    title = "gNMI Drivers",
    category = "Drivers",
    url = "http://onosproject.org",
    description = "Adds support for devices using gNMI protocol based on " +
    " openconfig models: http://openconfig.net/ .",
    included_bundles = BUNDLES,
    required_apps = [
        "org.onosproject.generaldeviceprovider",
        "org.onosproject.protocols.grpc",
        "org.onosproject.protocols.gnmi"
    ],
)
