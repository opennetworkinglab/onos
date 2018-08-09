BUNDLES = [
    "//protocols/p4runtime/api:onos-protocols-p4runtime-api",
    "//protocols/p4runtime/ctl:onos-protocols-p4runtime-ctl",
    "//protocols/p4runtime/model:onos-protocols-p4runtime-model",
    "//protocols/p4runtime/proto:p4runtime_java_grpc",
    "//protocols/p4runtime/proto:p4config_java_proto",
    "//protocols/p4runtime/proto:p4data_java_proto",
    "//protocols/p4runtime/proto:p4info_java_proto",
    "//protocols/p4runtime/proto:p4runtime_java_proto",
    "//protocols/p4runtime/proto:p4types_java_proto",
    "//protocols/p4runtime/proto:status_java_proto",
]

onos_app(
    app_name = "org.onosproject.protocols.p4runtime",
    title = "P4Runtime Protocol Subsystem",
    category = "Protocol",
    url = "http://onosproject.org",
    description = "ONOS P4Runtime protocol subsystem",
    included_bundles = BUNDLES,
    required_apps = [
        "org.onosproject.protocols.grpc"
    ],
)
