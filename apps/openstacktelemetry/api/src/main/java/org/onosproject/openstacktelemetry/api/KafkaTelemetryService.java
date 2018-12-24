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

import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.Set;
import java.util.concurrent.Future;

/**
 * Service API for publishing openstack telemetry through kafka producer.
 */
public interface KafkaTelemetryService extends TelemetryService {

    /**
     * Publishes openstack telemetry to Kafka server.
     *
     * @param flowInfos network metrics to be published
     * @return metadata for a record that has been acknowledged
     */
    Set<Future<RecordMetadata>> publish(Set<FlowInfo> flowInfos);

    /**
     * Returns kafka telemetry service type.
     *
     * @return kafka telemetry service type
     */
    @Override
    default ServiceType type() {
        return ServiceType.KAFKA;
    }
}
