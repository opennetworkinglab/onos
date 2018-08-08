# FIXME: Work in progress

COMPILE_DEPS = CORE_DEPS + JACKSON + KRYO + CLI + [
    "//apps/kafka-integration/api:onos-apps-kafka-integration-api",
    "//core/store/serializers:onos-core-serializers",
    "@kafka_clients//jar",
    "@protobuf-java-3.2.0//jar",
    "//lib:GRPC_1.3",
    "//incubator/protobuf/models:onos-incubator-protobuf-models",
    "//incubator/protobuf/models:onos-incubator-protobuf-models-proto",
]

#osgi_jar_with_tests (
#    deps = COMPILE_DEPS,
#    test_deps = TEST_ADAPTERS,
#    web_context = "/onos/kafka-integration",
#    api_title = "Kafka Integration",
#    api_version = "1.0",
#    api_description = "REST API for Kafka Integration",
#    api_package = "org.onosproject.kafkaintegration.rest",
#)
