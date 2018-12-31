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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.onosproject.openstacktelemetry.api.FlowInfo;
import org.onosproject.openstacktelemetry.api.KafkaTelemetryAdminService;
import org.onosproject.openstacktelemetry.api.OpenstackTelemetryService;
import org.onosproject.openstacktelemetry.api.TelemetryCodec;
import org.onosproject.openstacktelemetry.api.TelemetryConfigService;
import org.onosproject.openstacktelemetry.api.config.KafkaTelemetryConfig;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Future;

import static org.onosproject.openstacktelemetry.api.Constants.KAFKA_SCHEME;
import static org.onosproject.openstacktelemetry.api.config.TelemetryConfig.ConfigType.KAFKA;
import static org.onosproject.openstacktelemetry.config.DefaultKafkaTelemetryConfig.fromTelemetryConfig;

/**
 * Kafka telemetry manager.
 */
@Component(immediate = true)
@Service
public class KafkaTelemetryManager implements KafkaTelemetryAdminService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String BOOTSTRAP_SERVERS = "bootstrap.servers";
    private static final String RETRIES = "retries";
    private static final String ACKS = "acks";
    private static final String BATCH_SIZE = "batch.size";
    private static final String LINGER_MS = "linger.ms";
    private static final String MEMORY_BUFFER = "buffer.memory";
    private static final String KEY_SERIALIZER = "key.serializer";
    private static final String VALUE_SERIALIZER = "value.serializer";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackTelemetryService openstackTelemetryService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TelemetryConfigService telemetryConfigService;

    private Map<String, Producer<String, byte[]>> producers = Maps.newConcurrentMap();

    @Activate
    protected void activate() {

        openstackTelemetryService.addTelemetryService(this);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        stopAll();

        openstackTelemetryService.removeTelemetryService(this);

        log.info("Stopped");
    }

    @Override
    public Set<Future<RecordMetadata>> publish(Set<FlowInfo> flowInfos) {

        if (producers == null || producers.isEmpty()) {
            log.debug("Kafka telemetry service has not been enabled!");
            return null;
        }

        log.debug("Send telemetry record to kafka server...");
        Set<Future<RecordMetadata>> futureSet = Sets.newHashSet();
        producers.forEach((k, v) -> {
            TelemetryConfig config = telemetryConfigService.getConfig(k);
            KafkaTelemetryConfig kafkaConfig = fromTelemetryConfig(config);

            try {
                Class codecClazz = Class.forName(kafkaConfig.codec());
                TelemetryCodec codec = (TelemetryCodec) codecClazz.newInstance();

                ByteBuffer buffer = codec.encode(flowInfos);
                ProducerRecord record = new ProducerRecord<>(
                        kafkaConfig.topic(), kafkaConfig.key(), buffer.array());
                futureSet.add(v.send(record));
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
                log.warn("Failed to send telemetry record due to {}", e);
            }
        });
        return futureSet;
    }

    @Override
    public boolean isRunning() {
        return !producers.isEmpty();
    }

    @Override
    public void start(String name) {
        TelemetryConfig config = telemetryConfigService.getConfig(name);
        KafkaTelemetryConfig kafkaConfig = fromTelemetryConfig(config);

        if (kafkaConfig != null &&
                !config.name().equals(KAFKA_SCHEME) && config.enabled()) {
            StringBuilder kafkaServerBuilder = new StringBuilder();
            kafkaServerBuilder.append(kafkaConfig.address());
            kafkaServerBuilder.append(":");
            kafkaServerBuilder.append(kafkaConfig.port());

            // Configure Kafka server properties
            Properties prop = new Properties();
            prop.put(BOOTSTRAP_SERVERS, kafkaServerBuilder.toString());
            prop.put(RETRIES, kafkaConfig.retries());
            prop.put(ACKS, kafkaConfig.requiredAcks());
            prop.put(BATCH_SIZE, kafkaConfig.batchSize());
            prop.put(LINGER_MS, kafkaConfig.lingerMs());
            prop.put(MEMORY_BUFFER, kafkaConfig.memoryBuffer());
            prop.put(KEY_SERIALIZER, kafkaConfig.keySerializer());
            prop.put(VALUE_SERIALIZER, kafkaConfig.valueSerializer());

            producers.put(name, new KafkaProducer<>(prop));
        }
    }

    @Override
    public void stop(String name) {
        Producer<String, byte[]> producer = producers.get(name);

        if (producer != null) {
            producer.close();
            producers.remove(name);
        }
    }

    @Override
    public void restart(String name) {
        stop(name);
        start(name);
    }

    @Override
    public void startAll() {
        telemetryConfigService.getConfigsByType(KAFKA).forEach(c -> start(c.name()));
        log.info("Kafka producer has Started");
    }

    @Override
    public void stopAll() {
        if (!producers.isEmpty()) {
            producers.values().forEach(Producer::close);
        }

        producers.clear();

        log.info("Kafka producer has Stopped");
    }

    @Override
    public void restartAll() {
        stopAll();
        startAll();
    }
}
