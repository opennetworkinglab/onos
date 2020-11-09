osgi_jar(
    resources = glob(["src/main/resources/**"]),
    resources_root = "src/main/resources",
    deps = CORE_DEPS,
)

onos_app(
    app_name = "org.onosproject.drivers.stratum-odtn",
    category = "Drivers",
    description = "Adds support for Stratum Odtn-based devices",
    required_apps = [
        "org.onosproject.drivers.stratum",
        "org.onosproject.drivers.odtn-driver",
    ],
    title = "Stratum Odtn Drivers",
    url = "http://onosproject.org",
)
