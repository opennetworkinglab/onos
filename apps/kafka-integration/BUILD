BUNDLES = [
    "@kafka_clients//jar",
    "//deps:com_google_protobuf_protobuf_java",
    "//core/protobuf/models:onos-core-protobuf-models",
    "//core/protobuf/models/proto:onos-core-protobuf-models-proto",
    "//apps/kafka-integration/api:onos-apps-kafka-integration-api",
    "//apps/kafka-integration/app:onos-apps-kafka-integration-app",
]

onos_app(
    category = "Integration",
    description = "Provides integration of ONOS and Kafka message bus so that internal ONOS events " +
                  "can be broadcast over the Kafka message bus to off-platform applications.",
    included_bundles = BUNDLES,
    title = "Kafka Integration",
    url = "http://onosproject.org",
)
