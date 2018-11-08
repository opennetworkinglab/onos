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

package org.onosproject.kafkaintegration.kafka;

/**
 * Constants for default values of configurable properties.
 */
public final class OsgiPropertyConstants {
    private OsgiPropertyConstants() {
    }

    static final String BOOTSTRAP_SERVERS = "bootstrapServers";
    static final String BOOTSTRAP_SERVERS_DEFAULT = "localhost:9092";

    static final String RETRIES = "retries";
    static final int RETRIES_DEFAULT = 1;

    static final String MAX_IN_FLIGHT = "maxInFlightRequestsPerConnection";
    static final int MAX_IN_FLIGHT_DEFAULT = 5;

    static final String REQUIRED_ACKS = "requestRequiredAcks";
    static final int REQUIRED_ACKS_DEFAULT = 1;

    static final String KEY_SERIALIZER = "keySerializer";
    static final String KEY_SERIALIZER_DEFAULT = "org.apache.kafka.common.serialization.StringSerializer";

    static final String VALUE_SERIALIZER = "valueSerializer";
    static final String VALUE_SERIALIZER_DEFAULT = "org.apache.kafka.common.serialization.ByteArraySerializer";

}
