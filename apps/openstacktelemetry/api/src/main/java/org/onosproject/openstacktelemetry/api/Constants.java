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
package org.onosproject.openstacktelemetry.api;

/**
 * Provides constants used in OpenstackTelemetry.
 */
public final class Constants {

    private Constants() {
    }

    public static final String OPENSTACK_TELEMETRY_APP_ID = "org.onosproject.openstacktelemetry";

    private static final String DEFAULT_SERVER_IP = "localhost";

    // default configuration variables for gRPC
    public static final String DEFAULT_GRPC_SERVER_IP = DEFAULT_SERVER_IP;
    public static final int DEFAULT_GRPC_SERVER_PORT = 50051;
    public static final boolean DEFAULT_GRPC_USE_PLAINTEXT = true;
    public static final int DEFAULT_GRPC_MAX_INBOUND_MSG_SIZE = 4 * 1024 * 1024;

    // default configuration variables for InfluxDB
    public static final String DEFAULT_INFLUXDB_SERVER_IP = DEFAULT_SERVER_IP;
    public static final int DEFAULT_INFLUXDB_SERVER_PORT = 8086;
    public static final String DEFAULT_INFLUXDB_USERNAME = "onos";
    public static final String DEFAULT_INFLUXDB_PASSWORD = "onos";
    public static final String DEFAULT_INFLUXDB_DATABASE = "onos";
    public static final boolean DEFAULT_INFLUXDB_ENABLE_BATCH = true;

    // default configuration variables for Kafka
    public static final String DEFAULT_KAFKA_SERVER_IP = DEFAULT_SERVER_IP;
    public static final int DEFAULT_KAFKA_SERVER_PORT = 9092;
    public static final int DEFAULT_KAFKA_RETRIES = 0;
    public static final String DEFAULT_KAFKA_REQUIRED_ACKS = "all";
    public static final int DEFAULT_KAFKA_BATCH_SIZE = 16384;
    public static final int DEFAULT_KAFKA_LINGER_MS = 1;
    public static final int DEFAULT_KAFKA_MEMORY_BUFFER = 33554432;
    public static final String DEFAULT_KAFKA_KEY_SERIALIZER =
            "org.apache.kafka.common.serialization.StringSerializer";
    public static final String DEFAULT_KAFKA_VALUE_SERIALIZER =
            "org.apache.kafka.common.serialization.ByteArraySerializer";

    // default configuration variables for REST API
    public static final String DEFAULT_REST_SERVER_IP = DEFAULT_SERVER_IP;
    public static final int DEFAULT_REST_SERVER_PORT = 80;
    public static final String DEFAULT_REST_ENDPOINT = "telemetry";
    public static final String DEFAULT_REST_METHOD = "POST";
    public static final String DEFAULT_REST_REQUEST_MEDIA_TYPE = "application/json";
    public static final String DEFAULT_REST_RESPONSE_MEDIA_TYPE = "application/json";

    public static final boolean DEFAULT_DISABLE = false;
    public static final boolean DEFAULT_ENABLE = true;

    public static final String VXLAN = "VXLAN";
    public static final String VLAN = "VLAN";
    public static final String FLAT = "FLAT";
}
