BUNDLES = [
    "@kafka_clients//jar",
    "@influxdb_java//jar",
    "@commons_codec//jar",
    "@retrofit//jar",
    "@okhttp//jar",
    "@logging_interceptor//jar",
    "@gson//jar",
    "@okio//jar",
    "@moshi//jar",
    "@converter_moshi//jar",
    "@protobuf_java_3_2_0//jar",
    "@google_instrumentation_0_3_0//jar",
    "@grpc_core_1_3_1//jar",
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
