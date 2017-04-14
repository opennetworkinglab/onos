/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.kafkaintegration.api;

import java.util.concurrent.Future;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

/**
 * APIs for controlling the Kafka Producer.
 *
 */
public interface KafkaPublisherService {
    /**
     * Sends message to Kafka Server.
     *
     * @param record a message to be sent
     * @return metadata for a record that as been acknowledged
     */
    Future<RecordMetadata> send(ProducerRecord<String, byte[]> record);
}
