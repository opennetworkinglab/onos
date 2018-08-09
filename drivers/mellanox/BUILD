COMPILE_DEPS = CORE_DEPS + [
    "//drivers/p4runtime:onos-drivers-p4runtime",
]

BUNDLES = [
    ":onos-drivers-mellanox",
]

osgi_jar(
    deps = COMPILE_DEPS,
    resources_root = "src/main/resources",
    resources = glob(["src/main/resources/**"]),
)

onos_app (
    app_name = "org.onosproject.drivers.mellanox",
    title = "Mellanox Drivers",
    category = "Drivers",
    url = "http://onosproject.org",
    description = "Adds support for Mellanox Spectrum-based devices using P4Runtime",
    included_bundles = BUNDLES,
    required_apps = [
        "org.onosproject.drivers.p4runtime",
        "org.onosproject.pipelines.fabric",
    ],
)
