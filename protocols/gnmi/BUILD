BUNDLES = [
    "//protocols/gnmi/stub:onos-protocols-gnmi-stub",
    "//protocols/gnmi/ctl:onos-protocols-gnmi-ctl",
    "//protocols/gnmi/api:onos-protocols-gnmi-api",
]

onos_app(
    app_name = "org.onosproject.protocols.gnmi",
    category = "Protocol",
    description = "gNMI protocol subsystem",
    included_bundles = BUNDLES,
    required_apps = [
        "org.onosproject.protocols.grpc",
    ],
    title = "gNMI Protocol Subsystem",
    url = "http://onosproject.org",
)
