COMPILE_DEPS = CORE_DEPS + KRYO + JACKSON + [
    "//deps:io_grpc_grpc_api_context",
    "//drivers/p4runtime:onos-drivers-p4runtime",
    "//drivers/gnmi:onos-drivers-gnmi",
    "//drivers/gnoi:onos-drivers-gnoi",
    "//pipelines/basic:onos-pipelines-basic",
    "//protocols/grpc/api:onos-protocols-grpc-api",
    "//protocols/grpc/utils:onos-protocols-grpc-utils",
]

osgi_jar(
    resources = glob(["src/main/resources/**"]),
    resources_root = "src/main/resources",
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.drivers.stratum",
    category = "Drivers",
    description = "Adds support for Stratum-based devices",
    required_apps = [
        "org.onosproject.generaldeviceprovider",
        "org.onosproject.drivers.gnmi",
        "org.onosproject.drivers.gnoi",
        "org.onosproject.drivers.p4runtime",
        "org.onosproject.pipelines.basic",
    ],
    title = "Stratum Drivers",
    url = "http://onosproject.org",
)
