COMPILE_DEPS = CORE_DEPS + KRYO + [
    "//core/store/serializers:onos-core-serializers",
    "//protocols/p4runtime/api:onos-protocols-p4runtime-api",
    "@io_grpc_grpc_java//core",
]

BUNDLES = [
    ":onos-drivers-p4runtime",
]

osgi_jar (
    deps = COMPILE_DEPS,
    resources = glob(["src/main/resources/**"]),
    resources_root = "src/main/resources",
)

onos_app (
    app_name = "org.onosproject.drivers.p4runtime",
    title = "P4Runtime Drivers",
    category = "Drivers",
    url = "http://onosproject.org",
    description = "Adds support for devices using P4 Runtime protocol.",
    included_bundles = BUNDLES,
    required_apps = [
        "org.onosproject.generaldeviceprovider",
        "org.onosproject.protocols.p4runtime",
        "org.onosproject.p4runtime",
        "org.onosproject.drivers",
    ],
)
