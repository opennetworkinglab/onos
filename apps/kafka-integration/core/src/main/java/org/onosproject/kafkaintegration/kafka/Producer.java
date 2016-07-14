/**
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

package org.onosproject.kafkaintegration.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.Future;

/**
 * Implementation of Kafka Producer.
 */
public class Producer {
    private KafkaProducer<String, byte[]> kafkaProducer = null;

    private final Logger log = LoggerFactory.getLogger(getClass());

    Producer(String bootstrapServers, int retries, int maxInFlightRequestsPerConnection,
             int requestRequiredAcks, String keySerializer, String valueSerializer) {

        Properties prop = new Properties();
        prop.put("bootstrap.servers", bootstrapServers);
        prop.put("retries", retries);
        prop.put("max.in.flight.requests.per.connection", maxInFlightRequestsPerConnection);
        prop.put("request.required.acks", requestRequiredAcks);
        prop.put("key.serializer", keySerializer);
        prop.put("value.serializer", valueSerializer);

        kafkaProducer = new KafkaProducer<>(prop);
    }

    public void start() {
        log.info("Started");
    }

    public void stop() {
        if (kafkaProducer != null) {
            kafkaProducer.close();
            kafkaProducer = null;
        }

        log.info("Stopped");
    }

    public Future<RecordMetadata> send(ProducerRecord<String, byte[]> record) {
        return kafkaProducer.send(record);
    }
}
