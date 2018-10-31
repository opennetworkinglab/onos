COMPILE_DEPS = CORE_DEPS + KRYO + JACKSON + [
    "@io_grpc_grpc_java//core",
    "//drivers/p4runtime:onos-drivers-p4runtime",
    "//drivers/gnmi:onos-drivers-gnmi",
    "//pipelines/basic:onos-pipelines-basic",
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
        "org.onosproject.drivers.p4runtime",
    ],
    title = "Stratum Drivers",
    url = "http://onosproject.org",
)
