/*
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

package org.onosproject.rabbitmq.listener;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;

import java.util.concurrent.ExecutorService;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkListener;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.link.ProbedLinkProvider;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.topology.TopologyEvent;
import org.onosproject.net.topology.TopologyListener;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.rabbitmq.api.MQConstants;
import org.onosproject.rabbitmq.api.MQService;
import org.onosproject.rabbitmq.impl.MQServiceImpl;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Listens to events generated from Device Event/PKT_IN/Topology/Link.
 * Then publishes events to rabbitmq server via publish() api.
 */

@Component(immediate = true)
public class MQEventHandler extends AbstractProvider
                            implements ProbedLinkProvider {

    private static final Logger log = LoggerFactory.getLogger(
                                                    MQEventHandler.class);
    private static final String PROVIDER_NAME = MQConstants.ONOS_APP_NAME;
    private static final int PKT_PROC_PRIO = 1;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    private MQService mqService;
    private DeviceListener deviceListener;
    protected ExecutorService eventExecutor;

    private final InternalPacketProcessor packetProcessor =
                                          new InternalPacketProcessor();
    private final LinkListener linkListener = new InternalLinkListener();
    private final TopologyListener topologyListener =
                                           new InternalTopologyListener();

    /**
     * Initialize parent class with provider.
     */
    public MQEventHandler() {
        super(new ProviderId("rabbitmq", PROVIDER_NAME));
    }

    @Activate
    protected void activate(ComponentContext context) {
        mqService = new MQServiceImpl(context);
        eventExecutor = newSingleThreadScheduledExecutor(
                groupedThreads("onos/deviceevents", "events-%d", log));
        deviceListener = new InternalDeviceListener();
        deviceService.addListener(deviceListener);
        packetService.addProcessor(packetProcessor,
                                   PacketProcessor.advisor(PKT_PROC_PRIO));
        linkService.addListener(linkListener);
        topologyService.addListener(topologyListener);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        deviceService.removeListener(deviceListener);
        packetService.removeProcessor(packetProcessor);
        eventExecutor.shutdownNow();
        eventExecutor = null;
        linkService.removeListener(linkListener);
        topologyService.removeListener(topologyListener);
        log.info("Stopped");
    }

    /**
     * Captures incoming device events.
     */
    private class InternalDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {
            if (event == null) {
                log.error("Device event is null.");
                return;
            }
            mqService.publish(event);
        }
    }

    /**
     * Captures incoming packets from switches connected to ONOS
     * controller..
     */
    private class InternalPacketProcessor implements PacketProcessor {
        @Override
        public void process(PacketContext context) {
            if (context == null) {
                log.error("Packet context is null.");
                return;
            }
            mqService.publish(context);
        }
    }

    /**
     * Listens to link events and processes the link additions.
     */
    private class InternalLinkListener implements LinkListener {
        @Override
        public void event(LinkEvent event) {
            if (event == null) {
                log.error("Link event is null.");
                return;
            }
            mqService.publish(event);
        }
    }

    /**
     * Listens to topology events and processes the topology changes.
     */
    private class InternalTopologyListener implements TopologyListener {

        @Override
        public void event(TopologyEvent event) {
            if (event == null) {
                log.error("Topology event is null.");
                return;
            }
            mqService.publish(event);
        }
    }
}
