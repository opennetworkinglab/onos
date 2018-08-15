COMPILE_DEPS = CORE_DEPS + [
    "//drivers/p4runtime:onos-drivers-p4runtime",
]

BUNDLES = [
    ":onos-drivers-mellanox",
]

osgi_jar(
    resources = glob(["src/main/resources/**"]),
    resources_root = "src/main/resources",
    deps = COMPILE_DEPS,
)

onos_app(
    app_name = "org.onosproject.drivers.mellanox",
    category = "Drivers",
    description = "Adds support for Mellanox Spectrum-based devices using P4Runtime",
    included_bundles = BUNDLES,
    required_apps = [
        "org.onosproject.drivers.p4runtime",
        "org.onosproject.pipelines.fabric",
    ],
    title = "Mellanox Drivers",
    url = "http://onosproject.org",
)
