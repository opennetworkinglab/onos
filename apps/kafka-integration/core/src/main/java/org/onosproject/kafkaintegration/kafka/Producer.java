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

import java.util.Properties;
import java.util.concurrent.Future;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.onosproject.kafkaintegration.api.KafkaProducerService;
import org.onosproject.kafkaintegration.api.dto.KafkaServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of a Kafka Producer.
 */
@Component
@Service
public class Producer implements KafkaProducerService {
    private KafkaProducer<String, byte[]> kafkaProducer = null;

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Activate
    protected void activate() {
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }

    @Override
    public void start(KafkaServerConfig config) {

        if (kafkaProducer != null) {
            log.info("Producer has already started");
            return;
        }

        String bootstrapServer =
                new StringBuilder().append(config.getIpAddress()).append(":")
                        .append(config.getPort()).toString();

        // Set Server Properties
        Properties prop = new Properties();
        prop.put("bootstrap.servers", bootstrapServer);
        prop.put("retries", config.getNumOfRetries());
        prop.put("max.in.flight.requests.per.connection",
                 config.getMaxInFlightRequestsPerConnection());
        prop.put("request.required.acks", config.getAcksRequired());
        prop.put("key.serializer", config.getKeySerializer());
        prop.put("value.serializer", config.getValueSerializer());

        kafkaProducer = new KafkaProducer<>(prop);
        log.info("Kafka Producer has started.");
    }

    @Override
    public void stop() {
        if (kafkaProducer != null) {
            kafkaProducer.close();
            kafkaProducer = null;
        }

        log.info("Kafka Producer has Stopped");
    }

    @Override
    public void restart(KafkaServerConfig config) {
        stop();
        start(config);
    }

    @Override
    public Future<RecordMetadata> send(ProducerRecord<String, byte[]> record) {
        return kafkaProducer.send(record);
    }
}
