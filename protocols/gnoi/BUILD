BUNDLES = [
    "//protocols/gnoi/stub:onos-protocols-gnoi-stub",
    "//protocols/gnoi/ctl:onos-protocols-gnoi-ctl",
    "//protocols/gnoi/api:onos-protocols-gnoi-api",
]

onos_app(
    app_name = "org.onosproject.protocols.gnoi",
    category = "Protocol",
    description = "gNOI protocol subsystem",
    included_bundles = BUNDLES,
    required_apps = [
        "org.onosproject.protocols.grpc",
    ],
    title = "gNOI Protocol Subsystem",
    url = "http://onosproject.org",
)
