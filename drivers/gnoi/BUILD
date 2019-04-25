COMPILE_DEPS = CORE_DEPS + KRYO + [
    "//lib:com_google_protobuf_protobuf_java",
    "//lib:io_grpc_grpc_core_context",
    "//lib:io_grpc_grpc_netty",
    "//lib:io_grpc_grpc_stub",
    "//protocols/gnoi/stub:onos-protocols-gnoi-stub",
    "//protocols/gnoi/api:onos-protocols-gnoi-api",
    "//protocols/grpc/api:onos-protocols-grpc-api",
    "//protocols/grpc/utils:onos-protocols-grpc-utils",
]

BUNDLES = [
    ":onos-drivers-gnoi",
]

osgi_jar(
    resources = glob(["src/main/resources/**"]),
    resources_root = "src/main/resources",
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.drivers.gnoi",
    category = "Drivers",
    description = "Adds support for devices using gNOI protocol based on " +
                  " openconfig proto definitions: https://github.com/openconfig/gnoi/ .",
    included_bundles = BUNDLES,
    required_apps = [
        "org.onosproject.generaldeviceprovider",
        "org.onosproject.protocols.gnoi",
    ],
    title = "gNOI Drivers",
    url = "https://github.com/openconfig/gnoi/",
)
