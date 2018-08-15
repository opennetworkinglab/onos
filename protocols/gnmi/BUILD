BUNDLES = [
    "//protocols/gnmi/stub:gnmi_java_grpc",
    "//protocols/gnmi/stub:gnmi_java_proto",
]

onos_app(
    app_name = "org.onosproject.protocols.gnmi",
    category = "Protocol",
    description = "ONOS gNMI protocol subsystem",
    included_bundles = BUNDLES,
    required_apps = [
        "org.onosproject.protocols.grpc",
    ],
    title = "gNMI Protocol Subsystem",
    url = "http://onosproject.org",
)
