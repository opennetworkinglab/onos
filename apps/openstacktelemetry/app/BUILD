COMPILE_DEPS = CORE_DEPS + JACKSON + KRYO + CLI + REST + [
    "@kafka_clients//jar",
    "@jersey_client//jar",
    "@influxdb_java//jar",
    "@simpleclient//jar",
    "@simpleclient_common//jar",
    "@simpleclient_hotspot//jar",
    "@simpleclient_servlet//jar",
    "@jetty_servlet//jar",
    "@jetty_http//jar",
    "@jetty_server//jar",
    "@jetty_util//jar",
    "@jetty_websocket//jar",
    "@servlet_api//jar",
    "//deps:io_grpc_grpc_api_context",
    "//deps:io_grpc_grpc_protobuf_lite",
    "//core/store/serializers:onos-core-serializers",
    "//apps/openstacknode/api:onos-apps-openstacknode-api",
    "//apps/openstacknetworking/api:onos-apps-openstacknetworking-api",
    "//apps/openstacktelemetry/api:onos-apps-openstacktelemetry-api",
]

TEST_DEPS = TEST_ADAPTERS + [
    "//core/api:onos-api-tests",
    "//core/common:onos-core-common-tests",
    "//apps/openstacktelemetry/api:onos-apps-openstacktelemetry-api-tests",
]

osgi_jar_with_tests(
    karaf_command_packages = ["org.onosproject.openstacktelemetry.cli"],
    test_deps = TEST_DEPS,
    deps = COMPILE_DEPS,
)
