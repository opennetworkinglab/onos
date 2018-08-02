COMPILE_DEPS = CORE_DEPS + JACKSON + KRYO + REST + CLI + GRPC_1_3 + [
    "@kafka_clients//jar",
    "@jersey_client//jar",
    "@influxdb_java//jar",
    "@grpc_core_1_3_1//jar",
    "//core/store/serializers:onos-core-serializers",
    "//apps/openstacknode/api:onos-apps-openstacknode-api",
    "//apps/openstacknetworking/api:onos-apps-openstacknetworking-api",
    "//apps/openstacktelemetry/api:onos-apps-openstacktelemetry-api",
    "@grpc_protobuf_lite_1_3_1//jar",
]

TEST_DEPS = TEST_ADAPTERS + TEST_REST + [
    "//core/api:onos-api-tests",
    "//core/common:onos-core-common-tests",
    "//web/api:onos-rest-tests",
]

osgi_jar_with_tests(
    api_description = "OpenStack Network Telemetry REST API",
    api_package = "org.onosproject.openstacktelemetry.web",
    api_title = "OpenStack Network Telemetry REST API",
    api_version = "1.0",
    test_deps = TEST_DEPS,
    web_context = "/onos/openstacktelemetry",
    deps = COMPILE_DEPS,
)
