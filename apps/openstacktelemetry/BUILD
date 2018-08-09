BUNDLES = [
    "@kafka_clients//jar",
    "@influxdb_java//jar",
    "@commons_codec//jar",
    "@retrofit//jar",
    "@okhttp//jar",
    "@logging_interceptor//jar",
    "@com_google_code_gson_gson//jar",
    "@okio//jar",
    "@moshi//jar",
    "@converter_moshi//jar",
    "@com_google_protobuf//:protobuf_java",
    "@io_grpc_grpc_java//core",
    "//apps/openstacktelemetry/api:onos-apps-openstacktelemetry-api",
    "//apps/openstacktelemetry/app:onos-apps-openstacktelemetry-app",
]

onos_app(
    category = "Integration",
    description = "SONA Openstack Telemetry Application.",
    included_bundles = BUNDLES,
    required_apps = [
        "org.onosproject.openstacknetworking",
    ],
    title = "OpenStack Telemetry Application",
    url = "https://wiki.onosproject.org/display/ONOS/SONA%3A+DC+Network+Virtualization",
)
