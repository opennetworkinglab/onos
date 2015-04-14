/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.segmentrouting;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.Event;
import org.onosproject.segmentrouting.grouphandler.DefaultGroupHandler;
import org.onosproject.segmentrouting.grouphandler.NeighborSet;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupEvent;
import org.onosproject.net.group.GroupKey;
import org.onosproject.net.group.GroupListener;
import org.onosproject.net.group.GroupService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkListener;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.segmentrouting.config.NetworkConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("ALL")
@Component(immediate = true)
public class SegmentRoutingManager {

    private static Logger log = LoggerFactory.getLogger(SegmentRoutingManager.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentService intentService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected GroupService groupService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;
    protected ArpHandler arpHandler = null;
    protected IcmpHandler icmpHandler = null;
    protected IpHandler ipHandler = null;
    protected RoutingRulePopulator routingRulePopulator = null;
    protected ApplicationId appId;
    protected DeviceConfiguration deviceConfiguration = null;

    private DefaultRoutingHandler defaultRoutingHandler = null;
    private InternalPacketProcessor processor = new InternalPacketProcessor();
    private InternalEventHandler eventHandler = new InternalEventHandler();

    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    private static ScheduledFuture<?> eventHandlerFuture = null;
    private ConcurrentLinkedQueue<Event> eventQueue = new ConcurrentLinkedQueue<Event>();
    private Map<DeviceId, DefaultGroupHandler> groupHandlerMap
            = new ConcurrentHashMap<DeviceId, DefaultGroupHandler>();

    private NetworkConfigManager networkConfigService = new NetworkConfigManager();;

    private static int numOfEvents = 0;
    private static int numOfHandlerExecution = 0;
    private static int numOfHandlerScheduled = 0;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication("org.onosproject.segmentrouting");
        networkConfigService.init();
        deviceConfiguration = new DeviceConfiguration(networkConfigService);
        arpHandler = new ArpHandler(this);
        icmpHandler = new IcmpHandler(this);
        ipHandler = new IpHandler(this);
        routingRulePopulator = new RoutingRulePopulator(this);
        defaultRoutingHandler = new DefaultRoutingHandler(this);

        packetService.addProcessor(processor, PacketProcessor.ADVISOR_MAX + 2);
        linkService.addListener(new InternalLinkListener());
        groupService.addListener(new InternalGroupListener());
        deviceService.addListener(new InternalDeviceListener());

        for (Device device: deviceService.getDevices()) {
            if (mastershipService.
                    getLocalRole(device.id()) == MastershipRole.MASTER) {
                DefaultGroupHandler groupHandler =
                    DefaultGroupHandler.createGroupHandler(device.id(),
                            appId, deviceConfiguration, linkService, groupService);
                groupHandler.createGroups();
                groupHandlerMap.put(device.id(), groupHandler);
                defaultRoutingHandler.populateTtpRules(device.id());
                log.debug("Initiating default group handling for {}", device.id());
            } else {
                log.debug("Activate: Local role {} "
                                + "is not MASTER for device {}",
                        mastershipService.
                                getLocalRole(device.id()),
                        device.id());
            }
        }

        defaultRoutingHandler.startPopulationProcess();
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        packetService.removeProcessor(processor);
        processor = null;
        log.info("Stopped");
    }

    /**
     * Returns the GrouopKey object for the device and the NighborSet given.
     *
     * @param ns NeightborSet object for the GroupKey
     * @return GroupKey object for the NeighborSet
     */
    public GroupKey getGroupKey(NeighborSet ns) {

        for (DefaultGroupHandler groupHandler: groupHandlerMap.values()) {
            return groupHandler.getGroupKey(ns);
        }

        return null;
    }

    private class InternalPacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {

            if (context.isHandled()) {
                return;
            }

            InboundPacket pkt = context.inPacket();
            Ethernet ethernet = pkt.parsed();

            if (ethernet.getEtherType() == Ethernet.TYPE_ARP) {
                arpHandler.processPacketIn(pkt);
            } else if (ethernet.getEtherType() == Ethernet.TYPE_IPV4) {
                IPv4 ipPacket = (IPv4) ethernet.getPayload();
                ipHandler.addToPacketBuffer(ipPacket);
                if (ipPacket.getProtocol() == IPv4.PROTOCOL_ICMP) {
                    icmpHandler.processPacketIn(pkt);
                } else {
                    ipHandler.processPacketIn(pkt);
                }
            }
        }
    }

    private class InternalLinkListener implements LinkListener {
        @Override
        public void event(LinkEvent event) {
            if (event.type() == LinkEvent.Type.LINK_ADDED ||
                    event.type() == LinkEvent.Type.LINK_REMOVED) {
                scheduleEventHandlerIfNotScheduled(event);
            }
        }
    }

    private class InternalDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {
            if (mastershipService.
                    getLocalRole(event.subject().id()) != MastershipRole.MASTER) {
                log.debug("Local role {} is not MASTER for device {}",
                        mastershipService.
                                getLocalRole(event.subject().id()),
                        event.subject().id());
                return;
            }

            switch (event.type()) {
            case DEVICE_ADDED:
            case PORT_REMOVED:
            case DEVICE_UPDATED:
            case DEVICE_AVAILABILITY_CHANGED:
                scheduleEventHandlerIfNotScheduled(event);
                break;
            default:
            }
        }
    }

    private class InternalGroupListener implements GroupListener {

        @Override
        public void event(GroupEvent event) {
            switch (event.type()) {
                case GROUP_ADDED:
                    scheduleEventHandlerIfNotScheduled(event);
                    break;
                case GROUP_ADD_REQUESTED:
                    log.info("Group add requested");
                    break;
                case GROUP_UPDATED:
                    break;
                default:
                    log.warn("Unhandled group event type: {}", event.type());
            }
        }
    }

    private void scheduleEventHandlerIfNotScheduled(Event event) {

        eventQueue.add(event);
        numOfEvents++;
        if (eventHandlerFuture == null ||
                eventHandlerFuture.isDone()) {
            eventHandlerFuture = executorService.schedule(eventHandler,
                    100, TimeUnit.MILLISECONDS);
            numOfHandlerScheduled++;
        }

        log.trace("numOfEvents {}, numOfEventHanlderScheduled {}", numOfEvents,
                numOfHandlerScheduled);

    }

    private class InternalEventHandler implements Runnable {

        @Override
        public void run() {
            numOfHandlerExecution++;
            while (!eventQueue.isEmpty()) {
                Event event = eventQueue.poll();
                if (event.type() == LinkEvent.Type.LINK_ADDED) {
                    processLinkAdded((Link) event.subject());
                } else if (event.type() == LinkEvent.Type.LINK_REMOVED) {
                    processLinkRemoved((Link) event.subject());
                } else if (event.type() == GroupEvent.Type.GROUP_ADDED) {
                    processGroupAdded((Group) event.subject());
                } else if (event.type() == DeviceEvent.Type.DEVICE_ADDED ||
                        event.type() == DeviceEvent.Type.DEVICE_AVAILABILITY_CHANGED ||
                        event.type() == DeviceEvent.Type.DEVICE_UPDATED) {
                    if (deviceService.isAvailable(((Device) event.subject()).id())) {
                        processDeviceAdded((Device) event.subject());
                    }
                } else if (event.type() == DeviceEvent.Type.PORT_REMOVED) {
                    processPortRemoved((Device) event.subject(),
                            ((DeviceEvent) event).port());
                } else {
                    log.warn("Unhandled event type: {}", event.type());
                }
            }
            log.debug("numOfHandlerExecution {} numOfEventHanlderScheduled {} numOfEvents {}",
                    numOfHandlerExecution, numOfHandlerScheduled, numOfEvents);
        }
    }



    private void processLinkAdded(Link link) {
        log.debug("A new link {} was added", link.toString());

        if (mastershipService.
                getLocalRole(link.src().deviceId()) == MastershipRole.MASTER) {
            DefaultGroupHandler groupHandler =
                    groupHandlerMap.get(link.src().deviceId());
            if (groupHandler != null) {
                groupHandler.linkUp(link);
            }
        }
        defaultRoutingHandler.populateRoutingRulesForLinkStatusChange(null);
    }

    private void processLinkRemoved(Link link) {
        log.debug("A link {} was removed", link.toString());
        defaultRoutingHandler.populateRoutingRulesForLinkStatusChange(link);
    }


    private void processGroupAdded(Group group) {
        log.debug("A new group with ID {} was added", group.id());
        defaultRoutingHandler.resumePopulationProcess();
    }

    private void processDeviceAdded(Device device) {
        log.debug("A new device with ID {} was added", device.id());
        defaultRoutingHandler.populateTtpRules(device.id());
        DefaultGroupHandler dgh = DefaultGroupHandler.createGroupHandler(
                device.id(),
                appId,
                deviceConfiguration,
                linkService,
                groupService);
        dgh.createGroups();
        groupHandlerMap.put(device.id(), dgh);
    }

    private void processPortRemoved(Device device, Port port) {
        log.debug("Port {} was removed", port.toString());
        DefaultGroupHandler groupHandler =
                groupHandlerMap.get(device.id());
        if (groupHandler != null) {
            groupHandler.portDown(port.number());
        }
    }
}
