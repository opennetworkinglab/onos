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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.openstacktelemetry.api.KafkaTelemetryAdminService;
import org.onosproject.openstacktelemetry.api.KafkaTelemetryConfigService;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;
import org.onosproject.openstacktelemetry.config.DefaultKafkaTelemetryConfig;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

import static org.onosproject.openstacktelemetry.api.Constants.DEFAULT_DISABLE;
import static org.onosproject.openstacktelemetry.api.Constants.DEFAULT_KAFKA_BATCH_SIZE;
import static org.onosproject.openstacktelemetry.api.Constants.DEFAULT_KAFKA_KEY_SERIALIZER;
import static org.onosproject.openstacktelemetry.api.Constants.DEFAULT_KAFKA_LINGER_MS;
import static org.onosproject.openstacktelemetry.api.Constants.DEFAULT_KAFKA_MEMORY_BUFFER;
import static org.onosproject.openstacktelemetry.api.Constants.DEFAULT_KAFKA_REQUIRED_ACKS;
import static org.onosproject.openstacktelemetry.api.Constants.DEFAULT_KAFKA_RETRIES;
import static org.onosproject.openstacktelemetry.api.Constants.DEFAULT_KAFKA_SERVER_IP;
import static org.onosproject.openstacktelemetry.api.Constants.DEFAULT_KAFKA_SERVER_PORT;
import static org.onosproject.openstacktelemetry.api.Constants.DEFAULT_KAFKA_VALUE_SERIALIZER;
import static org.onosproject.openstacktelemetry.util.OpenstackTelemetryUtil.getBooleanProperty;

/**
 * Kafka server configuration manager for publishing openstack telemetry.
 */
@Component(immediate = true)
@Service
public class KafkaTelemetryConfigManager implements KafkaTelemetryConfigService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String ENABLE_SERVICE = "enableService";
    private static final String ADDRESS = "address";
    private static final String PORT = "port";
    private static final String RETRIES = "retries";
    private static final String REQUIRED_ACKS = "requiredAcks";
    private static final String BATCH_SIZE = "batchSize";
    private static final String LINGER_MS = "lingerMs";
    private static final String MEMORY_BUFFER = "memoryBuffer";
    private static final String KEY_SERIALIZER = "keySerializer";
    private static final String VALUE_SERIALIZER = "valueSerializer";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService componentConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected KafkaTelemetryAdminService kafkaTelemetryAdminService;

    @Property(name = ADDRESS, value = DEFAULT_KAFKA_SERVER_IP,
            label = "Default IP address to establish initial connection to Kafka server")
    protected String address = DEFAULT_KAFKA_SERVER_IP;

    @Property(name = PORT, intValue = DEFAULT_KAFKA_SERVER_PORT,
            label = "Default port number to establish initial connection to Kafka server")
    protected Integer port = DEFAULT_KAFKA_SERVER_PORT;

    @Property(name = RETRIES, intValue = DEFAULT_KAFKA_RETRIES,
            label = "Number of times the producer can retry to send after first failure")
    protected int retries = DEFAULT_KAFKA_RETRIES;

    @Property(name = REQUIRED_ACKS, value = DEFAULT_KAFKA_REQUIRED_ACKS,
            label = "Producer will get an acknowledgement after the leader has replicated the data")
    protected String requiredAcks = DEFAULT_KAFKA_REQUIRED_ACKS;

    @Property(name = BATCH_SIZE, intValue = DEFAULT_KAFKA_BATCH_SIZE,
            label = "The largest record batch size allowed by Kafka")
    protected Integer batchSize = DEFAULT_KAFKA_BATCH_SIZE;

    @Property(name = LINGER_MS, intValue = DEFAULT_KAFKA_LINGER_MS,
            label = "The producer groups together any records that arrive in " +
                    "between request transmissions into a single batched request")
    protected Integer lingerMs = DEFAULT_KAFKA_LINGER_MS;

    @Property(name = MEMORY_BUFFER, intValue = DEFAULT_KAFKA_MEMORY_BUFFER,
            label = "The total memory used for log cleaner I/O buffers across all cleaner threads")
    protected Integer memoryBuffer = DEFAULT_KAFKA_MEMORY_BUFFER;

    @Property(name = KEY_SERIALIZER, value = DEFAULT_KAFKA_KEY_SERIALIZER,
            label = "Serializer class for key that implements the Serializer interface")
    protected String keySerializer = DEFAULT_KAFKA_KEY_SERIALIZER;

    @Property(name = VALUE_SERIALIZER, value = DEFAULT_KAFKA_VALUE_SERIALIZER,
            label = "Serializer class for value that implements the Serializer interface")
    protected String valueSerializer = DEFAULT_KAFKA_VALUE_SERIALIZER;

    @Property(name = ENABLE_SERVICE, boolValue = DEFAULT_DISABLE,
            label = "Specify the default behavior of telemetry service")
    protected Boolean enableService = DEFAULT_DISABLE;

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

        if (enableService) {
            if (kafkaTelemetryAdminService.isRunning()) {
                kafkaTelemetryAdminService.restart(getConfig());
            } else {
                kafkaTelemetryAdminService.start(getConfig());
            }
        } else {
            if (kafkaTelemetryAdminService.isRunning()) {
                kafkaTelemetryAdminService.stop();
            }
        }
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

        String addressStr = Tools.get(properties, ADDRESS);
        address = addressStr != null ? addressStr : DEFAULT_KAFKA_SERVER_IP;
        log.info("Configured. Kafka server address is {}", address);

        Integer portConfigured = Tools.getIntegerProperty(properties, PORT);
        if (portConfigured == null) {
            port = DEFAULT_KAFKA_SERVER_PORT;
            log.info("Kafka server port is NOT configured, default value is {}", port);
        } else {
            port = portConfigured;
            log.info("Configured. Kafka server port is {}", port);
        }

        Integer retriesConfigured = Tools.getIntegerProperty(properties, RETRIES);
        if (retriesConfigured == null) {
            retries = DEFAULT_KAFKA_RETRIES;
            log.info("Kafka number of retries property is NOT configured, default value is {}", retries);
        } else {
            retries = retriesConfigured;
            log.info("Configured. Kafka number of retries is {}", retries);
        }

        String requiredAcksStr = Tools.get(properties, REQUIRED_ACKS);
        requiredAcks = requiredAcksStr != null ? requiredAcksStr : DEFAULT_KAFKA_REQUIRED_ACKS;
        log.info("Configured, Kafka required acknowledgement is {}", requiredAcks);

        Integer batchSizeConfigured = Tools.getIntegerProperty(properties, BATCH_SIZE);
        if (batchSizeConfigured == null) {
            batchSize = DEFAULT_KAFKA_BATCH_SIZE;
            log.info("Kafka batch size property is NOT configured, default value is {}", batchSize);
        } else {
            batchSize = batchSizeConfigured;
            log.info("Configured. Kafka batch size is {}", batchSize);
        }

        Integer lingerMsConfigured = Tools.getIntegerProperty(properties, LINGER_MS);
        if (lingerMsConfigured == null) {
            lingerMs = DEFAULT_KAFKA_LINGER_MS;
            log.info("Kafka lingerMs property is NOT configured, default value is {}", lingerMs);
        } else {
            lingerMs = lingerMsConfigured;
            log.info("Configured. Kafka lingerMs is {}", lingerMs);
        }

        Integer memoryBufferConfigured = Tools.getIntegerProperty(properties, MEMORY_BUFFER);
        if (memoryBufferConfigured == null) {
            memoryBuffer = DEFAULT_KAFKA_MEMORY_BUFFER;
            log.info("Kafka memory buffer property is NOT configured, default value is {}", memoryBuffer);
        } else {
            memoryBuffer = memoryBufferConfigured;
            log.info("Configured. Kafka memory buffer is {}", memoryBuffer);
        }

        String keySerializerStr = Tools.get(properties, KEY_SERIALIZER);
        keySerializer = keySerializerStr != null ? keySerializerStr : DEFAULT_KAFKA_KEY_SERIALIZER;
        log.info("Configured, Kafka key serializer is {}", keySerializer);

        String valueSerializerStr = Tools.get(properties, VALUE_SERIALIZER);
        valueSerializer = valueSerializerStr != null ? valueSerializerStr : DEFAULT_KAFKA_VALUE_SERIALIZER;
        log.info("Configured, Kafka value serializer is {}", valueSerializer);

        Boolean enableServiceConfigured =
                getBooleanProperty(properties, ENABLE_SERVICE);
        if (enableServiceConfigured == null) {
            enableService = DEFAULT_DISABLE;
            log.info("Kafka service enable flag is NOT " +
                    "configured, default value is {}", enableService);
        } else {
            enableService = enableServiceConfigured;
            log.info("Configured. Kafka service enable flag is {}", enableService);
        }
    }
}
