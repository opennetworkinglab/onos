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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.kafkaintegration.api.EventConversionService;
import org.onosproject.kafkaintegration.api.EventSubscriptionService;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkListener;
import org.onosproject.net.link.LinkService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.get;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.kafkaintegration.api.dto.OnosEvent.Type.DEVICE;
import static org.onosproject.kafkaintegration.api.dto.OnosEvent.Type.LINK;

/**
 * Encapsulates the behavior of monitoring various ONOS events.
 * */
@Component(immediate = true)
public class EventMonitor {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EventSubscriptionService eventSubscriptionService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EventConversionService eventConversionService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService componentConfigService;

    private final DeviceListener deviceListener = new InternalDeviceListener();
    private final LinkListener linkListener = new InternalLinkListener();

    protected ExecutorService eventExecutor;

    private static final String BOOTSTRAP_SERVERS = "localhost:9092";
    private static final int RETRIES = 1;
    private static final int MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION = 5;
    private static final int REQUEST_REQUIRED_ACKS = 1;
    private static final String KEY_SERIALIZER = "org.apache.kafka.common.serialization.StringSerializer";
    private static final String VALUE_SERIALIZER = "org.apache.kafka.common.serialization.ByteArraySerializer";

    @Property(name = "bootstrap.servers", value = BOOTSTRAP_SERVERS,
            label = "Default host/post pair to establish initial connection to Kafka cluster.")
    private String bootstrapServers = BOOTSTRAP_SERVERS;

    @Property(name = "retries", intValue = RETRIES,
            label = "Number of times the producer can retry to send after first failure")
    private int retries = RETRIES;

    @Property(name = "max.in.flight.requests.per.connection", intValue = MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION,
            label = "The maximum number of unacknowledged requests the client will send before blocking")
    private int maxInFlightRequestsPerConnection = MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION;

    @Property(name = "request.required.acks", intValue = 1,
            label = "Producer will get an acknowledgement after the leader has replicated the data")
    private int requestRequiredAcks = REQUEST_REQUIRED_ACKS;

    @Property(name = "key.serializer", value = KEY_SERIALIZER,
            label = "Serializer class for key that implements the Serializer interface.")
    private String keySerializer = KEY_SERIALIZER;

    @Property(name = "value.serializer", value = VALUE_SERIALIZER,
            label = "Serializer class for value that implements the Serializer interface.")
    private String valueSerializer = VALUE_SERIALIZER;

    private Producer producer;

    @Activate
    protected void activate(ComponentContext context) {
        componentConfigService.registerProperties(getClass());
        eventExecutor = newSingleThreadScheduledExecutor(groupedThreads("onos/onosEvents", "events-%d", log));
        deviceService.addListener(deviceListener);
        linkService.addListener(linkListener);
        producer = new Producer(bootstrapServers, retries, maxInFlightRequestsPerConnection,
                                requestRequiredAcks, keySerializer, valueSerializer);
        producer.start();

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        componentConfigService.unregisterProperties(getClass(), false);
        deviceService.removeListener(deviceListener);
        linkService.removeListener(linkListener);
        producer.stop();
        eventExecutor.shutdownNow();
        eventExecutor = null;

        log.info("Stopped");
    }

    @Modified
    private void modified(ComponentContext context) {
        if (context == null) {
            bootstrapServers = BOOTSTRAP_SERVERS;
            retries = RETRIES;
            maxInFlightRequestsPerConnection = MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION;
            requestRequiredAcks = REQUEST_REQUIRED_ACKS;
            keySerializer = KEY_SERIALIZER;
            valueSerializer = VALUE_SERIALIZER;
            return;
        }
        Dictionary properties = context.getProperties();

        String newBootstrapServers = BOOTSTRAP_SERVERS;
        int newRetries = RETRIES;
        int newMaxInFlightRequestsPerConnection = MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION;
        int newRequestRequiredAcks = REQUEST_REQUIRED_ACKS;
        try {
            String s = get(properties, "bootstrapServers");
            newBootstrapServers = isNullOrEmpty(s)
                    ? bootstrapServers : s.trim();

            s = get(properties, "retries");
            newRetries = isNullOrEmpty(s)
                    ? retries : Integer.parseInt(s.trim());

            s = get(properties, "maxInFlightRequestsPerConnection");
            newMaxInFlightRequestsPerConnection = isNullOrEmpty(s)
                    ? maxInFlightRequestsPerConnection : Integer.parseInt(s.trim());

            s = get(properties, "requestRequiredAcks");
            newRequestRequiredAcks = isNullOrEmpty(s)
                    ? requestRequiredAcks : Integer.parseInt(s.trim());

        } catch (NumberFormatException | ClassCastException e) {
            return;
        }

        boolean modified = newBootstrapServers != bootstrapServers ||
                newRetries != retries ||
                newMaxInFlightRequestsPerConnection != maxInFlightRequestsPerConnection ||
                newRequestRequiredAcks != requestRequiredAcks;

        if (modified) {
            bootstrapServers = newBootstrapServers;
            retries = newRetries;
            maxInFlightRequestsPerConnection = newMaxInFlightRequestsPerConnection;
            requestRequiredAcks = newRequestRequiredAcks;
            if (producer != null) {
                producer.stop();
            }
            producer = new Producer(bootstrapServers, retries, maxInFlightRequestsPerConnection,
                                        requestRequiredAcks, keySerializer, valueSerializer);
            producer.start();
            log.info("Modified");
        } else {
            return;
        }
    }

    private class InternalDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {
            if (!eventSubscriptionService.getEventSubscribers(DEVICE).isEmpty()) {
                eventExecutor.execute(() -> {
                    try {
                        String id = UUID.randomUUID().toString();
                        producer.send(new ProducerRecord<>(DEVICE.toString(),
                                                                id, event.subject().toString().getBytes())).get();
                        log.debug("Device event sent successfully.");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (ExecutionException e) {
                        log.error("Exception thrown {}", e);
                    }
                });
            } else {
                log.debug("No device listeners");
            }
        }
    }

    private class InternalLinkListener implements LinkListener {

        @Override
        public void event(LinkEvent event) {
            if (!eventSubscriptionService.getEventSubscribers(LINK).isEmpty()) {
                eventExecutor.execute(() -> {
                    try {
                        String id = UUID.randomUUID().toString();
                        producer.send(new ProducerRecord<>(LINK.toString(),
                                                                id, event.subject().toString().getBytes())).get();
                        log.debug("Link event sent successfully.");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (ExecutionException e) {
                        log.error("Exception thrown {}", e);
                    }
                });
            } else {
                log.debug("No link listeners");
            }
        }
    }
}
