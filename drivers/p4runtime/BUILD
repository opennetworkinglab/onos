COMPILE_DEPS = CORE_DEPS + KRYO + [
    "//core/store/serializers:onos-core-serializers",
    "//protocols/grpc/api:onos-protocols-grpc-api",
    "//protocols/grpc/utils:onos-protocols-grpc-utils",
    "//protocols/p4runtime/api:onos-protocols-p4runtime-api",
    "//deps:io_grpc_grpc_api_context",
]

BUNDLES = [
    ":onos-drivers-p4runtime",
]

osgi_jar(
    resources = glob(["src/main/resources/**"]),
    resources_root = "src/main/resources",
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.drivers.p4runtime",
    category = "Drivers",
    description = "Adds support for devices using P4 Runtime protocol.",
    included_bundles = BUNDLES,
    required_apps = [
        "org.onosproject.generaldeviceprovider",
        "org.onosproject.protocols.p4runtime",
        "org.onosproject.p4runtime",
        "org.onosproject.drivers",
    ],
    title = "P4Runtime Drivers",
    url = "http://onosproject.org",
)
