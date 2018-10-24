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

import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.openstacktelemetry.api.KafkaTelemetryAdminService;
import org.onosproject.openstacktelemetry.api.KafkaTelemetryConfigService;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;
import org.onosproject.openstacktelemetry.config.DefaultKafkaTelemetryConfig;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_KAFKA_ADDRESS;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_KAFKA_ADDRESS_DEFAULT;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_KAFKA_BATCH_SIZE;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_KAFKA_BATCH_SIZE_DEFAULT;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_KAFKA_ENABLE_SERVICE;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_KAFKA_ENABLE_SERVICE_DEFAULT;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_KAFKA_KEY_SERIALIZER;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_KAFKA_KEY_SERIALIZER_DEFAULT;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_KAFKA_LINGER_MS;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_KAFKA_LINGER_MS_DEFAULT;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_KAFKA_MEMORY_BUFFER;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_KAFKA_MEMORY_BUFFER_DEFAULT;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_KAFKA_PORT;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_KAFKA_PORT_DEFAULT;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_KAFKA_REQUIRED_ACKS;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_KAFKA_REQUIRED_ACKS_DEFAULT;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_KAFKA_RETRIES;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_KAFKA_RETRIES_DEFAULT;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_KAFKA_VALUE_SERIALIZER;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_KAFKA_VALUE_SERIALIZER_DEFAULT;
import static org.onosproject.openstacktelemetry.util.OpenstackTelemetryUtil.getBooleanProperty;
import static org.onosproject.openstacktelemetry.util.OpenstackTelemetryUtil.initTelemetryService;

/**
 * Kafka server configuration manager for publishing openstack telemetry.
 */
@Component(
    immediate = true,
    service = KafkaTelemetryConfigService.class,
    property = {
        PROP_KAFKA_ADDRESS + "=" + PROP_KAFKA_ADDRESS_DEFAULT,
        PROP_KAFKA_PORT + ":Integer=" + PROP_KAFKA_PORT_DEFAULT,
        PROP_KAFKA_RETRIES + ":Integer=" + PROP_KAFKA_RETRIES_DEFAULT,
        PROP_KAFKA_REQUIRED_ACKS + "=" + PROP_KAFKA_REQUIRED_ACKS_DEFAULT,
        PROP_KAFKA_BATCH_SIZE + ":Integer=" + PROP_KAFKA_BATCH_SIZE_DEFAULT,
        PROP_KAFKA_LINGER_MS + ":Integer=" + PROP_KAFKA_LINGER_MS_DEFAULT,
        PROP_KAFKA_MEMORY_BUFFER + ":Integer=" + PROP_KAFKA_MEMORY_BUFFER_DEFAULT,
        PROP_KAFKA_KEY_SERIALIZER + "=" + PROP_KAFKA_KEY_SERIALIZER_DEFAULT,
        PROP_KAFKA_VALUE_SERIALIZER + "=" + PROP_KAFKA_VALUE_SERIALIZER_DEFAULT,
        PROP_KAFKA_ENABLE_SERVICE + ":Boolean=" + PROP_KAFKA_ENABLE_SERVICE_DEFAULT
    }
)
public class KafkaTelemetryConfigManager implements KafkaTelemetryConfigService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService componentConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KafkaTelemetryAdminService kafkaTelemetryAdminService;

    /** Default IP address to establish initial connection to Kafka server. */
    protected String address = PROP_KAFKA_ADDRESS_DEFAULT;

    /** Default port number to establish initial connection to Kafka server. */
    protected Integer port = PROP_KAFKA_PORT_DEFAULT;

    /** Number of times the producer can retry to send after first failure. */
    protected int retries = PROP_KAFKA_RETRIES_DEFAULT;

    /** Producer will get an acknowledgement after the leader has replicated the data. */
    protected String requiredAcks = PROP_KAFKA_REQUIRED_ACKS_DEFAULT;

    /** The largest record batch size allowed by Kafka. */
    protected Integer batchSize = PROP_KAFKA_BATCH_SIZE_DEFAULT;

    /** The producer groups together any records that arrive between request transmissions into a single batch. */
    protected Integer lingerMs = PROP_KAFKA_LINGER_MS_DEFAULT;

    /** The total memory used for log cleaner I/O buffers across all cleaner threads. */
    protected Integer memoryBuffer = PROP_KAFKA_MEMORY_BUFFER_DEFAULT;

    /** Serializer class for key that implements the Serializer interface. */
    protected String keySerializer = PROP_KAFKA_KEY_SERIALIZER_DEFAULT;

    /** Serializer class for value that implements the Serializer interface. */
    protected String valueSerializer = PROP_KAFKA_VALUE_SERIALIZER_DEFAULT;

    /** Specify the default behavior of telemetry service. */
    protected Boolean enableService = PROP_KAFKA_ENABLE_SERVICE_DEFAULT;

    @Activate
    protected void activate(ComponentContext context) {
        componentConfigService.registerProperties(getClass());

        if (enableService) {
            kafkaTelemetryAdminService.start(getConfig());
        }
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        componentConfigService.unregisterProperties(getClass(), false);

        if (enableService) {
            kafkaTelemetryAdminService.stop();
        }
        log.info("Stopped");
    }

    @Modified
    private void modified(ComponentContext context) {
        readComponentConfiguration(context);
        initTelemetryService(kafkaTelemetryAdminService, getConfig(), enableService);
        log.info("Modified");
    }

    @Override
    public TelemetryConfig getConfig() {
        return new DefaultKafkaTelemetryConfig.DefaultBuilder()
                .withAddress(address)
                .withPort(port)
                .withRetries(retries)
                .withRequiredAcks(requiredAcks)
                .withBatchSize(batchSize)
                .withLingerMs(lingerMs)
                .withMemoryBuffer(memoryBuffer)
                .withKeySerializer(keySerializer)
                .withValueSerializer(valueSerializer)
                .build();
    }

    /**
     * Extracts properties from the component configuration context.
     *
     * @param context the component context
     */
    private void readComponentConfiguration(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();

        String addressStr = Tools.get(properties, PROP_KAFKA_ADDRESS);
        address = addressStr != null ? addressStr : PROP_KAFKA_ADDRESS_DEFAULT;
        log.info("Configured. Kafka server address is {}", address);

        Integer portConfigured = Tools.getIntegerProperty(properties, PROP_KAFKA_PORT);
        if (portConfigured == null) {
            port = PROP_KAFKA_PORT_DEFAULT;
            log.info("Kafka server port is NOT configured, default value is {}", port);
        } else {
            port = portConfigured;
            log.info("Configured. Kafka server port is {}", port);
        }

        Integer retriesConfigured = Tools.getIntegerProperty(properties, PROP_KAFKA_RETRIES);
        if (retriesConfigured == null) {
            retries = PROP_KAFKA_RETRIES_DEFAULT;
            log.info("Kafka number of retries property is NOT configured, default value is {}", retries);
        } else {
            retries = retriesConfigured;
            log.info("Configured. Kafka number of retries is {}", retries);
        }

        String requiredAcksStr = Tools.get(properties, PROP_KAFKA_REQUIRED_ACKS);
        requiredAcks = requiredAcksStr != null ? requiredAcksStr : PROP_KAFKA_REQUIRED_ACKS_DEFAULT;
        log.info("Configured, Kafka required acknowledgement is {}", requiredAcks);

        Integer batchSizeConfigured = Tools.getIntegerProperty(properties, PROP_KAFKA_BATCH_SIZE);
        if (batchSizeConfigured == null) {
            batchSize = PROP_KAFKA_BATCH_SIZE_DEFAULT;
            log.info("Kafka batch size property is NOT configured, default value is {}", batchSize);
        } else {
            batchSize = batchSizeConfigured;
            log.info("Configured. Kafka batch size is {}", batchSize);
        }

        Integer lingerMsConfigured = Tools.getIntegerProperty(properties, PROP_KAFKA_LINGER_MS);
        if (lingerMsConfigured == null) {
            lingerMs = PROP_KAFKA_LINGER_MS_DEFAULT;
            log.info("Kafka lingerMs property is NOT configured, default value is {}", lingerMs);
        } else {
            lingerMs = lingerMsConfigured;
            log.info("Configured. Kafka lingerMs is {}", lingerMs);
        }

        Integer memoryBufferConfigured = Tools.getIntegerProperty(properties, PROP_KAFKA_MEMORY_BUFFER);
        if (memoryBufferConfigured == null) {
            memoryBuffer = PROP_KAFKA_MEMORY_BUFFER_DEFAULT;
            log.info("Kafka memory buffer property is NOT configured, default value is {}", memoryBuffer);
        } else {
            memoryBuffer = memoryBufferConfigured;
            log.info("Configured. Kafka memory buffer is {}", memoryBuffer);
        }

        String keySerializerStr = Tools.get(properties, PROP_KAFKA_KEY_SERIALIZER);
        keySerializer = keySerializerStr != null ? keySerializerStr : PROP_KAFKA_KEY_SERIALIZER_DEFAULT;
        log.info("Configured, Kafka key serializer is {}", keySerializer);

        String valueSerializerStr = Tools.get(properties, PROP_KAFKA_VALUE_SERIALIZER);
        valueSerializer = valueSerializerStr != null ? valueSerializerStr : PROP_KAFKA_VALUE_SERIALIZER_DEFAULT;
        log.info("Configured, Kafka value serializer is {}", valueSerializer);

        Boolean enableServiceConfigured =
                getBooleanProperty(properties, PROP_KAFKA_ENABLE_SERVICE);
        if (enableServiceConfigured == null) {
            enableService = PROP_KAFKA_ENABLE_SERVICE_DEFAULT;
            log.info("Kafka service enable flag is NOT " +
                    "configured, default value is {}", enableService);
        } else {
            enableService = enableServiceConfigured;
            log.info("Configured. Kafka service enable flag is {}", enableService);
        }
    }
}
