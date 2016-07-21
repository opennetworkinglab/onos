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
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.onosproject.kafkaintegration.api.EventConversionService;
import org.onosproject.kafkaintegration.api.EventSubscriptionService;
import org.onosproject.kafkaintegration.api.KafkaProducerService;
import org.onosproject.kafkaintegration.api.KafkaConfigService;
import org.onosproject.kafkaintegration.api.dto.OnosEvent;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkListener;
import org.onosproject.net.link.LinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
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
    protected KafkaProducerService kafkaProducer;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected KafkaConfigService kafkaConfigService;

    private final DeviceListener deviceListener = new InternalDeviceListener();
    private final LinkListener linkListener = new InternalLinkListener();

    protected ExecutorService eventExecutor;

    @Activate
    protected void activate() {

        eventExecutor = newSingleThreadScheduledExecutor(groupedThreads("onos/onosEvents", "events-%d", log));
        deviceService.addListener(deviceListener);
        linkService.addListener(linkListener);

        kafkaProducer.start(kafkaConfigService.getConfigParams());

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        deviceService.removeListener(deviceListener);
        linkService.removeListener(linkListener);

        eventExecutor.shutdownNow();
        eventExecutor = null;

        kafkaProducer.stop();

        log.info("Stopped");
    }

    private class InternalDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {

            if (!eventSubscriptionService.getEventSubscribers(DEVICE).isEmpty()) {
                OnosEvent onosEvent = eventConversionService.convertEvent(event);
                eventExecutor.execute(() -> {
                    try {
                        kafkaProducer.send(new ProducerRecord<>(DEVICE.toString(),
                                                               onosEvent.subject().toByteArray())).get();

                        log.debug("Event Type - {}, Subject {} sent successfully.",
                                  DEVICE, onosEvent.subject());

                    } catch (InterruptedException e1) {
                        Thread.currentThread().interrupt();
                    } catch (ExecutionException e2) {
                        log.error("Exception thrown {}", e2);
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
                OnosEvent onosEvent = eventConversionService.convertEvent(event);
                eventExecutor.execute(() -> {
                    try {
                        kafkaProducer.send(new ProducerRecord<>(LINK.toString(),
                                onosEvent.subject().toByteArray())).get();

                        log.debug("Event Type - {}, Subject {} sent successfully.",
                              LINK, onosEvent.subject());

                    } catch (InterruptedException e1) {
                        Thread.currentThread().interrupt();
                    } catch (ExecutionException e2) {
                        log.error("Exception thrown {}", e2);
                    }
                });
            } else {
                log.debug("No link listeners");
            }
        }
    }
}
