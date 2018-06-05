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

import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;

/**
 * Provides constants used in OpenstackTelemetry.
 */
public final class Constants {

    private Constants() {
    }

    public static final String OPENSTACK_TELEMETRY_APP_ID = "org.onosproject.openstacktelemetry";

    private static final String DEFAULT_SERVER_IP = "localhost";

    public static final String DEFAULT_KAFKA_SERVER_IP = DEFAULT_SERVER_IP;
    public static final int DEFAULT_KAFKA_SERVER_PORT = 9092;
    public static final int DEFAULT_KAFKA_RETRIES = 0;
    public static final String DEFAULT_KAFKA_REQUIRED_ACKS = "all";
    public static final int DEFAULT_KAFKA_BATCH_SIZE = 16384;
    public static final int DEFAULT_KAFKA_LINGER_MS = 1;
    public static final int DEFAULT_KAFKA_MEMORY_BUFFER = 33554432;
    public static final String DEFAULT_KAFKA_KEY_SERIALIZER = StringSerializer.class.toString();
    public static final String DEFAULT_KAFKA_VALUE_SERIALIZER = ByteArraySerializer.class.toString();

    public static final String DEFAULT_REST_SERVER_IP = DEFAULT_SERVER_IP;
    public static final int DEFAULT_REST_SERVER_PORT = 80;
}
