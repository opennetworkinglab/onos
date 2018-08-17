/**
 * Copyright 2016-present Open Networking Foundation
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

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.kafkaintegration.api.KafkaConfigService;
import org.onosproject.kafkaintegration.api.KafkaPublisherService;
import org.onosproject.kafkaintegration.api.KafkaEventStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.onosproject.kafkaintegration.api.dto.OnosEvent;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component(immediate = true)
public class EventPublisher {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KafkaConfigService kafkaConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KafkaEventStorageService kafkaStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KafkaPublisherService kafkaPublisher;

    protected ScheduledExecutorService exService;

    private static final String SUBSCRIBER_TOPIC = "WORK_QUEUE_SUBSCRIBER";

    private NodeId localNodeId;

    // Thread Scheduler Parameters
    private final long delay = 0;
    private final long period = 1;

    private EventCollector eventCollector;

    @Activate
    protected void activate() {

        leadershipService.runForLeadership(SUBSCRIBER_TOPIC);

        localNodeId = clusterService.getLocalNode().id();

        startCollector();

        log.info("Started");
    }

    private void startCollector() {
        exService = Executors.newSingleThreadScheduledExecutor();
        eventCollector = new EventCollector();
        exService.scheduleAtFixedRate(eventCollector, delay, period, TimeUnit.SECONDS);
    }

    @Deactivate
    protected void deactivate() {
        stopCollector();
        log.info("Stopped");
    }

    private void stopCollector() {
        exService.shutdown();
    }

    private class EventCollector implements Runnable {

        @Override
        public void run() {

            // do not allow to proceed without leadership
            NodeId leaderNodeId = leadershipService.getLeader(SUBSCRIBER_TOPIC);
            if (!Objects.equals(localNodeId, leaderNodeId)) {
                log.debug("Not a Leader so cannot consume event");
                return;
            }

            try {
                OnosEvent onosEvent = kafkaStore.consumeEvent();

                if (onosEvent != null) {
                    kafkaPublisher.send(new ProducerRecord<>(onosEvent.type().toString(),
                                                            onosEvent.subject())).get();

                    log.info("Event Type - {}, Subject {} sent successfully.",
                             onosEvent.type(), onosEvent.subject());
                }
            } catch (InterruptedException e1) {
                log.error("Thread interupted");
                Thread.currentThread().interrupt();
            } catch (ExecutionException e2) {
                log.error("Cannot publish data to Kafka - {}", e2);
            }
        }
    }

}
