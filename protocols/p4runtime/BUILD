BUNDLES = [
    "//protocols/p4runtime/api:onos-protocols-p4runtime-api",
    "//protocols/p4runtime/ctl:onos-protocols-p4runtime-ctl",
    "//protocols/p4runtime/model:onos-protocols-p4runtime-model",
    "//protocols/p4runtime/proto:onos-protocols-p4runtime-proto",
    "//protocols/p4runtime/utils:onos-protocols-p4runtime-utils",
]

onos_app(
    app_name = "org.onosproject.protocols.p4runtime",
    category = "Protocol",
    description = "ONOS P4Runtime protocol subsystem",
    included_bundles = BUNDLES,
    required_apps = [
        "org.onosproject.protocols.grpc",
    ],
    title = "P4Runtime Protocol Subsystem",
    url = "http://onosproject.org",
)
