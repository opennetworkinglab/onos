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
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.kafkaintegration.api.EventConversionService;
import org.onosproject.kafkaintegration.api.EventSubscriptionService;
import org.onosproject.kafkaintegration.api.KafkaEventStorageService;
import org.onosproject.kafkaintegration.api.dto.OnosEvent;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkListener;
import org.onosproject.net.link.LinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.kafkaintegration.api.dto.OnosEvent.Type.DEVICE;
import static org.onosproject.kafkaintegration.api.dto.OnosEvent.Type.LINK;


/**
 * Encapsulates the behavior of monitoring various ONOS events.
 * */
@Component(immediate = true)
public class EventListener {
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
    protected KafkaEventStorageService kafkaStoreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    private final DeviceListener deviceListener = new InternalDeviceListener();
    private final LinkListener linkListener = new InternalLinkListener();

    protected ExecutorService eventExecutor;

    private static final String PUBLISHER_TOPIC = "WORK_QUEUE_PUBLISHER";

    private NodeId localNodeId;

    @Activate
    protected void activate() {

        eventExecutor = newSingleThreadScheduledExecutor(groupedThreads("onos/onosEvents", "events-%d", log));
        deviceService.addListener(deviceListener);
        linkService.addListener(linkListener);

        localNodeId = clusterService.getLocalNode().id();

        leadershipService.runForLeadership(PUBLISHER_TOPIC);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        deviceService.removeListener(deviceListener);
        linkService.removeListener(linkListener);

        eventExecutor.shutdownNow();
        eventExecutor = null;

        log.info("Stopped");
    }

    private class InternalDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {

            // do not allow to proceed without leadership
            NodeId leaderNodeId = leadershipService.getLeader(PUBLISHER_TOPIC);
            if (!Objects.equals(localNodeId, leaderNodeId)) {
                log.debug("Not a Leader, cannot publish!");
                return;
            }

            if (!eventSubscriptionService.getEventSubscribers(DEVICE).isEmpty()) {
                OnosEvent onosEvent = eventConversionService.convertEvent(event);
                eventExecutor.execute(() -> {
                    kafkaStoreService.publishEvent(onosEvent);
                });
                log.debug("Pushed event {} to kafka storage", onosEvent);
            }

        }
    }

    private class InternalLinkListener implements LinkListener {

        @Override
        public void event(LinkEvent event) {

            // do not allow to proceed without leadership
            NodeId leaderNodeId = leadershipService.getLeader(PUBLISHER_TOPIC);
            if (!Objects.equals(localNodeId, leaderNodeId)) {
                log.debug("Not a Leader, cannot publish!");
                return;
            }

            if (!eventSubscriptionService.getEventSubscribers(LINK).isEmpty()) {
                OnosEvent onosEvent = eventConversionService.convertEvent(event);
                eventExecutor.execute(() -> {
                    kafkaStoreService.publishEvent(onosEvent);
                });
                log.debug("Pushed event {} to kafka storage", onosEvent);
            }

        }
    }
}