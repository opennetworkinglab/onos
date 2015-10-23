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
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.Event;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.segmentrouting.config.SegmentRoutingConfig;
import org.onosproject.segmentrouting.grouphandler.DefaultGroupHandler;
import org.onosproject.segmentrouting.grouphandler.NeighborSet;
import org.onosproject.segmentrouting.grouphandler.NeighborSetNextObjectiveStoreKey;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.group.GroupKey;
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
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMapBuilder;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("ALL")
@Service
@Component(immediate = true)
public class SegmentRoutingManager implements SegmentRoutingService {

    private static Logger log = LoggerFactory
            .getLogger(SegmentRoutingManager.class);

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
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    protected ArpHandler arpHandler = null;
    protected IcmpHandler icmpHandler = null;
    protected IpHandler ipHandler = null;
    protected RoutingRulePopulator routingRulePopulator = null;
    protected ApplicationId appId;
    protected DeviceConfiguration deviceConfiguration = null;

    private DefaultRoutingHandler defaultRoutingHandler = null;
    private TunnelHandler tunnelHandler = null;
    private PolicyHandler policyHandler = null;
    private InternalPacketProcessor processor = null;
    private InternalLinkListener linkListener = null;
    private InternalDeviceListener deviceListener = null;
    private InternalEventHandler eventHandler = new InternalEventHandler();

    private ScheduledExecutorService executorService = Executors
            .newScheduledThreadPool(1);

    private static ScheduledFuture<?> eventHandlerFuture = null;
    private ConcurrentLinkedQueue<Event> eventQueue = new ConcurrentLinkedQueue<Event>();
    private Map<DeviceId, DefaultGroupHandler> groupHandlerMap = new ConcurrentHashMap<DeviceId, DefaultGroupHandler>();
    // Per device next objective ID store with (device id + neighbor set) as key
    private EventuallyConsistentMap<NeighborSetNextObjectiveStoreKey,
        Integer> nsNextObjStore = null;
    private EventuallyConsistentMap<String, Tunnel> tunnelStore = null;
    private EventuallyConsistentMap<String, Policy> policyStore = null;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry cfgService;

    private final InternalConfigListener cfgListener =
            new InternalConfigListener(this);

    private final ConfigFactory cfgFactory =
            new ConfigFactory(SubjectFactories.DEVICE_SUBJECT_FACTORY,
                              SegmentRoutingConfig.class,
                              "segmentrouting") {
                @Override
                public SegmentRoutingConfig createConfig() {
                    return new SegmentRoutingConfig();
                }
            };

    private Object threadSchedulerLock = new Object();
    private static int numOfEventsQueued = 0;
    private static int numOfEventsExecuted = 0;
    private static int numOfHandlerExecution = 0;
    private static int numOfHandlerScheduled = 0;

    private KryoNamespace.Builder kryoBuilder = null;

    @Activate
    protected void activate() {
        appId = coreService
                .registerApplication("org.onosproject.segmentrouting");

        kryoBuilder = new KryoNamespace.Builder()
            .register(NeighborSetNextObjectiveStoreKey.class,
                    NeighborSet.class,
                    DeviceId.class,
                    URI.class,
                    WallClockTimestamp.class,
                    org.onosproject.cluster.NodeId.class,
                    HashSet.class,
                    Tunnel.class,
                    DefaultTunnel.class,
                    Policy.class,
                    TunnelPolicy.class,
                    Policy.Type.class
            );

        log.debug("Creating EC map nsnextobjectivestore");
        EventuallyConsistentMapBuilder<NeighborSetNextObjectiveStoreKey, Integer>
                nsNextObjMapBuilder = storageService.eventuallyConsistentMapBuilder();

        nsNextObjStore = nsNextObjMapBuilder
                .withName("nsnextobjectivestore")
                .withSerializer(kryoBuilder)
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .build();
        log.trace("Current size {}", nsNextObjStore.size());

        EventuallyConsistentMapBuilder<String, Tunnel> tunnelMapBuilder =
                storageService.eventuallyConsistentMapBuilder();

        tunnelStore = tunnelMapBuilder
                .withName("tunnelstore")
                .withSerializer(kryoBuilder)
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .build();

        EventuallyConsistentMapBuilder<String, Policy> policyMapBuilder =
                storageService.eventuallyConsistentMapBuilder();

        policyStore = policyMapBuilder
                .withName("policystore")
                .withSerializer(kryoBuilder)
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .build();

        cfgService.addListener(cfgListener);
        cfgService.registerConfigFactory(cfgFactory);

        processor = new InternalPacketProcessor();
        linkListener = new InternalLinkListener();
        deviceListener = new InternalDeviceListener();

        packetService.addProcessor(processor, PacketProcessor.director(2));
        linkService.addListener(linkListener);
        deviceService.addListener(deviceListener);

        cfgListener.configureNetwork();

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        cfgService.removeListener(cfgListener);
        cfgService.unregisterConfigFactory(cfgFactory);

        packetService.removeProcessor(processor);
        linkService.removeListener(linkListener);
        deviceService.removeListener(deviceListener);
        processor = null;
        linkListener = null;
        deviceService = null;

        groupHandlerMap.clear();

        log.info("Stopped");
    }


    @Override
    public List<Tunnel> getTunnels() {
        return tunnelHandler.getTunnels();
    }

    @Override
    public TunnelHandler.Result createTunnel(Tunnel tunnel) {
        return tunnelHandler.createTunnel(tunnel);
    }

    @Override
    public TunnelHandler.Result removeTunnel(Tunnel tunnel) {
        for (Policy policy: policyHandler.getPolicies()) {
            if (policy.type() == Policy.Type.TUNNEL_FLOW) {
                TunnelPolicy tunnelPolicy = (TunnelPolicy) policy;
                if (tunnelPolicy.tunnelId().equals(tunnel.id())) {
                    log.warn("Cannot remove the tunnel used by a policy");
                    return TunnelHandler.Result.TUNNEL_IN_USE;
                }
            }
        }
        return tunnelHandler.removeTunnel(tunnel);
    }

    @Override
    public PolicyHandler.Result removePolicy(Policy policy) {
        return policyHandler.removePolicy(policy);
    }

    @Override
    public PolicyHandler.Result createPolicy(Policy policy) {
        return policyHandler.createPolicy(policy);
    }

    @Override
    public List<Policy> getPolicies() {
        return policyHandler.getPolicies();
    }

    /**
     * Returns the tunnel object with the tunnel ID.
     *
     * @param tunnelId Tunnel ID
     * @return Tunnel reference
     */
    public Tunnel getTunnel(String tunnelId) {
        return tunnelHandler.getTunnel(tunnelId);
    }

    /**
     * Returns the GroupKey object for the device and the NeighborSet given.
     * XXX is this called
     *
     * @param ns NeightborSet object for the GroupKey
     * @return GroupKey object for the NeighborSet
     */
    public GroupKey getGroupKey(NeighborSet ns) {
        for (DefaultGroupHandler groupHandler : groupHandlerMap.values()) {
            return groupHandler.getGroupKey(ns);
        }

        return null;
    }

    /**
     * Returns the next objective ID for the NeighborSet given. If the nextObjectiveID does not exist,
     * a new one is created and returned.
     *
     * @param deviceId Device ID
     * @param ns NegighborSet
     * @return next objective ID
     */
    public int getNextObjectiveId(DeviceId deviceId, NeighborSet ns) {
        if (groupHandlerMap.get(deviceId) != null) {
            log.trace("getNextObjectiveId query in device {}", deviceId);
            return groupHandlerMap
                    .get(deviceId).getNextObjectiveId(ns);
        } else {
            log.warn("getNextObjectiveId query in device {} not found", deviceId);
            return -1;
        }
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
            if (event.type() == LinkEvent.Type.LINK_ADDED
                    || event.type() == LinkEvent.Type.LINK_REMOVED) {
                log.debug("Event {} received from Link Service", event.type());
                scheduleEventHandlerIfNotScheduled(event);
            }
        }
    }

    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            switch (event.type()) {
            case DEVICE_ADDED:
            case PORT_REMOVED:
            case DEVICE_UPDATED:
            case DEVICE_AVAILABILITY_CHANGED:
                log.debug("Event {} received from Device Service", event.type());
                scheduleEventHandlerIfNotScheduled(event);
                break;
            default:
            }
        }
    }

    private void scheduleEventHandlerIfNotScheduled(Event event) {
        synchronized (threadSchedulerLock) {
            eventQueue.add(event);
            numOfEventsQueued++;

            if ((numOfHandlerScheduled - numOfHandlerExecution) == 0) {
                //No pending scheduled event handling threads. So start a new one.
                eventHandlerFuture = executorService
                        .schedule(eventHandler, 100, TimeUnit.MILLISECONDS);
                numOfHandlerScheduled++;
            }
            log.trace("numOfEventsQueued {}, numOfEventHanlderScheduled {}",
                      numOfEventsQueued,
                      numOfHandlerScheduled);
        }
    }

    private class InternalEventHandler implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    Event event = null;
                    synchronized (threadSchedulerLock) {
                        if (!eventQueue.isEmpty()) {
                            event = eventQueue.poll();
                            numOfEventsExecuted++;
                        } else {
                            numOfHandlerExecution++;
                            log.debug("numOfHandlerExecution {} numOfEventsExecuted {}",
                                      numOfHandlerExecution, numOfEventsExecuted);
                            break;
                        }
                    }
                    if (event.type() == LinkEvent.Type.LINK_ADDED) {
                        processLinkAdded((Link) event.subject());
                    } else if (event.type() == LinkEvent.Type.LINK_REMOVED) {
                        processLinkRemoved((Link) event.subject());
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
            } catch (Exception e) {
                log.error("SegmentRouting event handler "
                        + "thread thrown an exception: {}", e);
            }
        }
    }

    private void processLinkAdded(Link link) {
        log.debug("A new link {} was added", link.toString());

        //Irrespective whether the local is a MASTER or not for this device,
        //create group handler instance and push default TTP flow rules.
        //Because in a multi-instance setup, instances can initiate
        //groups for any devices. Also the default TTP rules are needed
        //to be pushed before inserting any IP table entries for any device
        DefaultGroupHandler groupHandler = groupHandlerMap.get(link.src()
                .deviceId());
        if (groupHandler != null) {
            groupHandler.linkUp(link);
        } else {
            Device device = deviceService.getDevice(link.src().deviceId());
            if (device != null) {
                log.warn("processLinkAdded: Link Added "
                        + "Notification without Device Added "
                        + "event, still handling it");
                processDeviceAdded(device);
                groupHandler = groupHandlerMap.get(link.src()
                                                   .deviceId());
                groupHandler.linkUp(link);
            }
        }

        log.trace("Starting optimized route population process");
        defaultRoutingHandler.populateRoutingRulesForLinkStatusChange(null);
        //log.trace("processLinkAdded: re-starting route population process");
        //defaultRoutingHandler.startPopulationProcess();
    }

    private void processLinkRemoved(Link link) {
        log.debug("A link {} was removed", link.toString());
        DefaultGroupHandler groupHandler = groupHandlerMap.get(link.src().deviceId());
        if (groupHandler != null) {
            groupHandler.portDown(link.src().port());
        }
        log.trace("Starting optimized route population process");
        defaultRoutingHandler.populateRoutingRulesForLinkStatusChange(link);
        //log.trace("processLinkRemoved: re-starting route population process");
        //defaultRoutingHandler.startPopulationProcess();
    }

    private void processDeviceAdded(Device device) {
        log.debug("A new device with ID {} was added", device.id());
        //Irrespective whether the local is a MASTER or not for this device,
        //create group handler instance and push default TTP flow rules.
        //Because in a multi-instance setup, instances can initiate
        //groups for any devices. Also the default TTP rules are needed
        //to be pushed before inserting any IP table entries for any device
        DefaultGroupHandler dgh = DefaultGroupHandler.
                createGroupHandler(device.id(),
                                   appId,
                                   deviceConfiguration,
                                   linkService,
                                   flowObjectiveService,
                                   nsNextObjStore);
        groupHandlerMap.put(device.id(), dgh);
        defaultRoutingHandler.populatePortAddressingRules(device.id());
    }

    private void processPortRemoved(Device device, Port port) {
        log.debug("Port {} was removed", port.toString());
        DefaultGroupHandler groupHandler = groupHandlerMap.get(device.id());
        if (groupHandler != null) {
            groupHandler.portDown(port.number());
        }
    }

    private class InternalConfigListener implements NetworkConfigListener {
        SegmentRoutingManager segmentRoutingManager;

        public InternalConfigListener(SegmentRoutingManager srMgr) {
            this.segmentRoutingManager = srMgr;
        }

        public void configureNetwork() {
            deviceConfiguration = new DeviceConfiguration(segmentRoutingManager.cfgService);

            arpHandler = new ArpHandler(segmentRoutingManager);
            icmpHandler = new IcmpHandler(segmentRoutingManager);
            ipHandler = new IpHandler(segmentRoutingManager);
            routingRulePopulator = new RoutingRulePopulator(segmentRoutingManager);
            defaultRoutingHandler = new DefaultRoutingHandler(segmentRoutingManager);

            tunnelHandler = new TunnelHandler(linkService, deviceConfiguration,
                                              groupHandlerMap, tunnelStore);
            policyHandler = new PolicyHandler(appId, deviceConfiguration,
                                              flowObjectiveService,
                                              tunnelHandler, policyStore);

            for (Device device : deviceService.getDevices()) {
                //Irrespective whether the local is a MASTER or not for this device,
                //create group handler instance and push default TTP flow rules.
                //Because in a multi-instance setup, instances can initiate
                //groups for any devices. Also the default TTP rules are needed
                //to be pushed before inserting any IP table entries for any device
                DefaultGroupHandler groupHandler = DefaultGroupHandler
                        .createGroupHandler(device.id(), appId,
                                            deviceConfiguration, linkService,
                                            flowObjectiveService,
                                            nsNextObjStore);
                groupHandlerMap.put(device.id(), groupHandler);
                defaultRoutingHandler.populatePortAddressingRules(device.id());
            }

            defaultRoutingHandler.startPopulationProcess();
        }

        @Override
        public void event(NetworkConfigEvent event) {
            if (event.configClass().equals(SegmentRoutingConfig.class)) {
                if (event.type() == NetworkConfigEvent.Type.CONFIG_ADDED) {
                    log.info("Network configuration added.");
                    configureNetwork();
                }
                if (event.type() == NetworkConfigEvent.Type.CONFIG_UPDATED) {
                    log.info("Network configuration updated.");
                    // TODO support dynamic configuration
                }
            }
        }
    }
}
