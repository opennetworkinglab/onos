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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.JsonCodec;
import org.onosproject.openstacktelemetry.api.FlowInfo;
import org.onosproject.openstacktelemetry.api.KafkaTelemetryAdminService;
import org.onosproject.openstacktelemetry.api.LinkInfo;
import org.onosproject.openstacktelemetry.api.OpenstackTelemetryService;
import org.onosproject.openstacktelemetry.api.TelemetryCodec;
import org.onosproject.openstacktelemetry.api.TelemetryConfigService;
import org.onosproject.openstacktelemetry.api.config.KafkaTelemetryConfig;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;
import org.onosproject.rest.AbstractWebResource;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Future;

import static org.apache.kafka.clients.producer.ProducerConfig.ACKS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.BATCH_SIZE_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.BUFFER_MEMORY_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.LINGER_MS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.MAX_BLOCK_MS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.RECONNECT_BACKOFF_MS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.RETRIES_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.RETRY_BACKOFF_MS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG;
import static org.onosproject.openstacktelemetry.api.Constants.KAFKA_SCHEME;
import static org.onosproject.openstacktelemetry.api.config.TelemetryConfig.ConfigType.KAFKA;
import static org.onosproject.openstacktelemetry.api.config.TelemetryConfig.Status.ENABLED;
import static org.onosproject.openstacktelemetry.config.DefaultKafkaTelemetryConfig.fromTelemetryConfig;
import static org.onosproject.openstacktelemetry.util.OpenstackTelemetryUtil.flowsToLinks;
import static org.onosproject.openstacktelemetry.util.OpenstackTelemetryUtil.testConnectivity;

/**
 * Kafka telemetry manager.
 */
@Component(immediate = true, service = KafkaTelemetryAdminService.class)
public class KafkaTelemetryManager extends AbstractWebResource
        implements KafkaTelemetryAdminService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final int METADATA_FETCH_TIMEOUT_VAL = 300;
    private static final int TIMEOUT_VAL = 300;
    private static final int RETRY_BACKOFF_MS_VAL = 10000;
    private static final int RECONNECT_BACKOFF_MS_VAL = 10000;

    private static final String LINK_INFOS = "linkInfos";

    private static final String BYTE_ARRAY_SERIALIZER =
            "org.apache.kafka.common.serialization.ByteArraySerializer";
    private static final String STRING_SERIALIZER =
            "org.apache.kafka.common.serialization.StringSerializer";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CodecService codecService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackTelemetryService openstackTelemetryService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected TelemetryConfigService telemetryConfigService;

    private Map<String, Producer<String, String>> stringProducers = Maps.newConcurrentMap();
    private Map<String, Producer<String, byte[]>> byteProducers = Maps.newConcurrentMap();

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

        log.debug("Send telemetry record to kafka server...");
        Set<Future<RecordMetadata>> futureSet = Sets.newHashSet();

        if (byteProducers == null || byteProducers.isEmpty()) {
            log.debug("Byte producer is empty!");
        } else {
            byteProducers.forEach((k, v) -> {
                TelemetryConfig config = telemetryConfigService.getConfig(k);
                KafkaTelemetryConfig kafkaConfig = fromTelemetryConfig(config);

                if (kafkaConfig != null &&
                        BYTE_ARRAY_SERIALIZER.equals(kafkaConfig.valueSerializer())) {
                    try {
                        Class codecClazz = Class.forName(kafkaConfig.codec());
                        TelemetryCodec codec = (TelemetryCodec) codecClazz.newInstance();

                        ByteBuffer buffer = codec.encode(flowInfos);
                        ProducerRecord record = new ProducerRecord<>(
                                kafkaConfig.topic(), kafkaConfig.key(), buffer.array());
                        futureSet.add(v.send(record));
                    } catch (ClassNotFoundException |
                            IllegalAccessException | InstantiationException e) {
                        log.warn("Failed to send telemetry record due to {}", e);
                    }
                }
            });
        }

        if (stringProducers == null || stringProducers.isEmpty()) {
            log.debug("String producer is empty!");
        } else {
            stringProducers.forEach((k, v) -> {
                TelemetryConfig config = telemetryConfigService.getConfig(k);
                KafkaTelemetryConfig kafkaConfig = fromTelemetryConfig(config);

                if (kafkaConfig != null &&
                        STRING_SERIALIZER.equals(kafkaConfig.valueSerializer())) {

                    // TODO: this is a workaround to convert flowInfo to linkInfo
                    // need to find a better solution

                    Set<LinkInfo> linkInfos = flowsToLinks(flowInfos);

                    if (!linkInfos.isEmpty()) {
                        ProducerRecord record = new ProducerRecord<>(
                                kafkaConfig.topic(), kafkaConfig.key(),
                                encodeStrings(linkInfos, this,
                                        kafkaConfig.codec()).toString());
                        futureSet.add(v.send(record));
                    }
                }
            });
        }

        return futureSet;
    }

    @Override
    public boolean isRunning() {
        return !byteProducers.isEmpty();
    }

    @Override
    public boolean start(String name) {
        boolean success = false;
        TelemetryConfig config = telemetryConfigService.getConfig(name);
        KafkaTelemetryConfig kafkaConfig = fromTelemetryConfig(config);

        if (kafkaConfig != null && !config.name().equals(KAFKA_SCHEME) &&
                config.status() == ENABLED) {
            StringBuilder kafkaServerBuilder = new StringBuilder();
            kafkaServerBuilder.append(kafkaConfig.address());
            kafkaServerBuilder.append(":");
            kafkaServerBuilder.append(kafkaConfig.port());

            // Configure Kafka server properties
            Properties prop = new Properties();
            prop.put(BOOTSTRAP_SERVERS_CONFIG, kafkaServerBuilder.toString());
            prop.put(RETRIES_CONFIG, kafkaConfig.retries());
            prop.put(ACKS_CONFIG, kafkaConfig.requiredAcks());
            prop.put(BATCH_SIZE_CONFIG, kafkaConfig.batchSize());
            prop.put(LINGER_MS_CONFIG, kafkaConfig.lingerMs());
            prop.put(BUFFER_MEMORY_CONFIG, kafkaConfig.memoryBuffer());
            prop.put(KEY_SERIALIZER_CLASS_CONFIG, kafkaConfig.keySerializer());
            prop.put(VALUE_SERIALIZER_CLASS_CONFIG, kafkaConfig.valueSerializer());
            prop.put(MAX_BLOCK_MS_CONFIG, METADATA_FETCH_TIMEOUT_VAL);
            prop.put(REQUEST_TIMEOUT_MS_CONFIG, TIMEOUT_VAL);
            prop.put(RETRY_BACKOFF_MS_CONFIG, RETRY_BACKOFF_MS_VAL);
            prop.put(RECONNECT_BACKOFF_MS_CONFIG, RECONNECT_BACKOFF_MS_VAL);

            if (testConnectivity(kafkaConfig.address(), kafkaConfig.port())) {
                if (kafkaConfig.valueSerializer().equals(BYTE_ARRAY_SERIALIZER)) {
                    byteProducers.put(name, new KafkaProducer<>(prop));
                }

                if (kafkaConfig.valueSerializer().equals(STRING_SERIALIZER)) {
                    stringProducers.put(name, new KafkaProducer<>(prop));
                }

                success = true;
            } else {
                log.warn("Unable to connect to {}:{}, " +
                            "please check the connectivity manually",
                            kafkaConfig.address(), kafkaConfig.port());
            }
        }

        return success;
    }

    @Override
    public void stop(String name) {
        Producer<String, byte[]> byteProducer = byteProducers.get(name);
        Producer<String, String> stringProducer = stringProducers.get(name);

        if (byteProducer != null) {
            byteProducer.close();
            byteProducers.remove(name);
        }

        if (stringProducer != null) {
            stringProducer.close();
            stringProducers.remove(name);
        }
    }

    @Override
    public boolean restart(String name) {
        stop(name);
        return start(name);
    }

    @Override
    public void startAll() {
        telemetryConfigService.getConfigsByType(KAFKA).forEach(c -> start(c.name()));
        log.info("Kafka producer has Started");
    }

    @Override
    public void stopAll() {
        if (!byteProducers.isEmpty()) {
            byteProducers.values().forEach(Producer::close);
        }

        byteProducers.clear();

        if (!stringProducers.isEmpty()) {
            stringProducers.values().forEach(Producer::close);
        }

        stringProducers.clear();

        log.info("Kafka producer has Stopped");
    }

    @Override
    public void restartAll() {
        stopAll();
        startAll();
    }

    private ObjectNode encodeStrings(Set<LinkInfo> infos,
                                     CodecContext context, String codecName) {
        ObjectNode root = context.mapper().createObjectNode();
        ArrayNode array = context.mapper().createArrayNode();
        try {
            Class codecClazz = Class.forName(codecName);
            JsonCodec codec = codecService.getCodec(codecClazz);

            infos.forEach(l -> array.add(codec.encode(l, context)));
        } catch (ClassNotFoundException e) {
            log.warn("Failed to send telemetry record due to {}", e);
        }

        root.set(LINK_INFOS, array);
        return root;
    }
}
