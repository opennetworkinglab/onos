/*
 * Copyright 2018-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.openstacktelemetry.impl;

/**
 * Name/Value constants for properties.
 */
public final class OsgiPropertyConstants {
    private OsgiPropertyConstants() {
    }

    // REST telemetry

    static final String PROP_REST_ENABLE_SERVICE = "enableService";
    static final boolean PROP_REST_ENABLE_SERVICE_DEFAULT = false;

    static final String PROP_REST_SERVER_ADDRESS = "address";
    static final String PROP_REST_SERVER_ADDRESS_DEFAULT = "localhost";

    static final String PROP_REST_SERVER_PORT = "port";
    static final int PROP_REST_SERVER_PORT_DEFAULT = 80;

    static final String PROP_REST_ENDPOINT = "endpoint";
    static final String PROP_REST_ENDPOINT_DEFAULT = "telemetry";

    static final String PROP_REST_METHOD = "method";
    static final String PROP_REST_METHOD_DEFAULT = "POST";

    static final String PROP_REST_REQUEST_MEDIA_TYPE = "requestMediaType";
    static final String PROP_REST_REQUEST_MEDIA_TYPE_DEFAULT = "application/json";

    static final String PROP_REST_RESPONSE_MEDIA_TYPE = "responseMediaType";
    static final String PROP_REST_RESPONSE_MEDIA_TYPE_DEFAULT = "application/json";

    // Kafka telemetry

    static final String PROP_KAFKA_ENABLE_SERVICE = "enableService";
    static final boolean PROP_KAFKA_ENABLE_SERVICE_DEFAULT = false;

    static final String PROP_KAFKA_ADDRESS = "address";
    static final String PROP_KAFKA_ADDRESS_DEFAULT = "localhost";

    static final String PROP_KAFKA_PORT = "port";
    static final int PROP_KAFKA_PORT_DEFAULT = 9092;

    static final String PROP_KAFKA_RETRIES = "retries";
    static final int PROP_KAFKA_RETRIES_DEFAULT = 0;

    static final String PROP_KAFKA_REQUIRED_ACKS = "requiredAcks";
    static final String PROP_KAFKA_REQUIRED_ACKS_DEFAULT = "all";

    static final String PROP_KAFKA_BATCH_SIZE = "batchSize";
    static final int PROP_KAFKA_BATCH_SIZE_DEFAULT = 16384;

    static final String PROP_KAFKA_LINGER_MS = "lingerMs";
    static final int PROP_KAFKA_LINGER_MS_DEFAULT = 1;

    static final String PROP_KAFKA_MEMORY_BUFFER = "memoryBuffer";
    static final int PROP_KAFKA_MEMORY_BUFFER_DEFAULT = 33554432;

    static final String PROP_KAFKA_KEY_SERIALIZER = "keySerializer";
    static final String PROP_KAFKA_KEY_SERIALIZER_DEFAULT =
        "org.apache.kafka.common.serialization.StringSerializer";

    static final String PROP_KAFKA_VALUE_SERIALIZER = "valueSerializer";
    static final String PROP_KAFKA_VALUE_SERIALIZER_DEFAULT =
        "org.apache.kafka.common.serialization.ByteArraySerializer";

    // Stats flow rule manager

    static final String PROP_REVERSE_PATH_STATS = "reversePathStats";
    static final boolean PROP_REVERSE_PATH_STATS_DEFAULT = false;

    static final String PROP_EGRESS_STATS = "egressStats";
    static final boolean PROP_EGRESS_STATS_DEFAULT = false;

    static final String PROP_PORT_STATS = "portStats";
    static final boolean PROP_PORT_STATS_DEFAULT = true;

    static final String PROP_MONITOR_OVERLAY = "monitorOverlay";
    static final boolean PROP_MONITOR_OVERLAY_DEFAULT = true;

    static final String PROP_MONITOR_UNDERLAY = "monitorUnderlay";
    static final boolean PROP_MONITOR_UNDERLAY_DEFAULT = true;

    // Influx DB Telemetry config manager

    static final String PROP_INFLUXDB_ENABLE_SERVICE = "enableService";
    static final boolean PROP_INFLUXDB_ENABLE_SERVICE_DEFAULT = false;

    static final String PROP_INFLUXDB_SERVER_ADDRESS = "address";
    static final String PROP_INFLUXDB_SERVER_ADDRESS_DEFAULT = "localhost";

    static final String PROP_INFLUXDB_SERVER_PORT = "port";
    static final int PROP_INFLUXDB_SERVER_PORT_DEFAULT = 8086;

    static final String PROP_INFLUXDB_USERNAME = "username";
    static final String PROP_INFLUXDB_USERNAME_DEFAULT = "onos";

    static final String PROP_INFLUXDB_PASSWORD = "password";
    static final String PROP_INFLUXDB_PASSWORD_DEFAULT = "onos";

    static final String PROP_INFLUXDB_DATABASE = "database";
    static final String PROP_INFLUXDB_DATABASE_DEFAULT = "onos";

    static final String PROP_INFLUXDB_MEASUREMENT = "measurement";
    static final String PROP_INFLUXDB_MEASUREMENT_DEFAULT = "sonaflow";

    static final String PROP_INFLUXDB_ENABLE_BATCH = "enableBatch";
    static final boolean PROP_INFLUXDB_ENABLE_BATCH_DEFAULT = true;

    // GRPC Telemetry config manager
    static final String PROP_GRPC_ENABLE_SERVICE = "enableService";
    static final boolean GRPC_ENABLE_SERVICE_DEFAULT = false;

    static final String PROP_GRPC_SERVER_ADDRESS = "address";
    static final String GRPC_SERVER_ADDRESS_DEFAULT = "localhost";

    static final String PROP_GRPC_SERVER_PORT = "port";
    static final int GRPC_SERVER_PORT_DEFAULT = 50051;

    static final String PROP_GRPC_USE_PLAINTEXT = "usePlaintext";
    static final boolean GRPC_USE_PLAINTEXT_DEFAULT = true;

    static final String PROP_GRPC_MAX_INBOUND_MSG_SIZE = "maxInboundMsgSize";
    static final int GRPC_MAX_INBOUND_MSG_SIZE_DEFAULT = 4194304; //4 * 1024 * 1024;

    // Prometheus Telemetry config manager
    static final String PROP_PROMETHEUS_ENABLE_SERVICE = "enableService";
    static final boolean PROMETHEUS_ENABLE_SERVICE_DEFAULT = false;

    static final String PROP_PROMETHEUS_EXPORTER_ADDRESS = "address";
    public static final String PROMETHEUS_EXPORTER_ADDRESS_DEFAULT = "localhost";

    static final String PROP_PROMETHEUS_EXPORTER_PORT = "port";
    public static final int PROMETHEUS_EXPORTER_PORT_DEFAULT = 50051;
}
