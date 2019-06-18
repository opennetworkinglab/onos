COMPILE_DEPS = CORE_DEPS + JACKSON + KRYO + CLI + [
    "//apps/kafka-integration/api:onos-apps-kafka-integration-api",
    "//core/store/serializers:onos-core-serializers",
    "@kafka_clients//jar",
    "@javax_ws_rs_api//jar",
    "//utils/rest:onlab-rest",
    "//deps:com_google_protobuf_protobuf_java",
    "//core/protobuf/models:onos-core-protobuf-models",
    "//core/protobuf/models/proto:onos-core-protobuf-models-proto",
]

osgi_jar_with_tests(
    api_description = "REST API for Kafka Integration",
    api_package = "org.onosproject.kafkaintegration.rest",
    api_title = "Kafka Integration",
    api_version = "1.0",
    test_deps = TEST_ADAPTERS,
    web_context = "/onos/kafka-integration",
    deps = COMPILE_DEPS,
)
