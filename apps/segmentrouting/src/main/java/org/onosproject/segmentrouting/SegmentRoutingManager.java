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
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.Event;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveContext;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.segmentrouting.config.DeviceConfigNotFoundException;
import org.onosproject.segmentrouting.config.DeviceConfiguration;
import org.onosproject.segmentrouting.config.SegmentRoutingConfig;
import org.onosproject.segmentrouting.grouphandler.DefaultGroupHandler;
import org.onosproject.segmentrouting.grouphandler.NeighborSet;
import org.onosproject.segmentrouting.grouphandler.NeighborSetNextObjectiveStoreKey;
import org.onosproject.segmentrouting.grouphandler.PortNextObjectiveStoreKey;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flowobjective.FlowObjectiveService;
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
import org.onosproject.segmentrouting.grouphandler.SubnetNextObjectiveStoreKey;
import org.onosproject.store.service.EventuallyConsistentMap;
import org.onosproject.store.service.EventuallyConsistentMapBuilder;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.WallClockTimestamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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

    @SuppressWarnings("unused")
    private static ScheduledFuture<?> eventHandlerFuture = null;
    @SuppressWarnings("rawtypes")
    private ConcurrentLinkedQueue<Event> eventQueue = new ConcurrentLinkedQueue<Event>();
    private Map<DeviceId, DefaultGroupHandler> groupHandlerMap =
            new ConcurrentHashMap<DeviceId, DefaultGroupHandler>();
    // Per device next objective ID store with (device id + neighbor set) as key
    private EventuallyConsistentMap<NeighborSetNextObjectiveStoreKey, Integer>
            nsNextObjStore = null;
    // Per device next objective ID store with (device id + subnet) as key
    private EventuallyConsistentMap<SubnetNextObjectiveStoreKey, Integer>
            subnetNextObjStore = null;
    // Per device next objective ID store with (device id + port) as key
    private EventuallyConsistentMap<PortNextObjectiveStoreKey, Integer>
            portNextObjStore = null;
    // Per device, per-subnet assigned-vlans store, with (device id + subnet
    // IPv4 prefix) as key
    private EventuallyConsistentMap<SubnetAssignedVidStoreKey, VlanId>
        subnetVidStore = null;
    private EventuallyConsistentMap<String, Tunnel> tunnelStore = null;
    private EventuallyConsistentMap<String, Policy> policyStore = null;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry cfgService;

    private final InternalConfigListener cfgListener =
            new InternalConfigListener(this);

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private final ConfigFactory cfgFactory =
            new ConfigFactory(SubjectFactories.DEVICE_SUBJECT_FACTORY,
                              SegmentRoutingConfig.class,
                              "segmentrouting") {
                @Override
                public SegmentRoutingConfig createConfig() {
                    return new SegmentRoutingConfig();
                }
            };

    private final InternalHostListener hostListener = new InternalHostListener();

    private Object threadSchedulerLock = new Object();
    private static int numOfEventsQueued = 0;
    private static int numOfEventsExecuted = 0;
    private static int numOfHandlerExecution = 0;
    private static int numOfHandlerScheduled = 0;

    private KryoNamespace.Builder kryoBuilder = null;

    private static final short ASSIGNED_VLAN_START = 4093;
    public static final short ASSIGNED_VLAN_NO_SUBNET = 4094;

    @Activate
    protected void activate() {
        appId = coreService
                .registerApplication("org.onosproject.segmentrouting");

        kryoBuilder = new KryoNamespace.Builder()
            .register(NeighborSetNextObjectiveStoreKey.class,
                    SubnetNextObjectiveStoreKey.class,
                    SubnetAssignedVidStoreKey.class,
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
                    Policy.Type.class,
                    VlanId.class,
                    Ip4Address.class,
                    Ip4Prefix.class,
                    IpAddress.Version.class,
                    ConnectPoint.class
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

        log.debug("Creating EC map subnetnextobjectivestore");
        EventuallyConsistentMapBuilder<SubnetNextObjectiveStoreKey, Integer>
                subnetNextObjMapBuilder = storageService.eventuallyConsistentMapBuilder();
        subnetNextObjStore = subnetNextObjMapBuilder
                .withName("subnetnextobjectivestore")
                .withSerializer(kryoBuilder)
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .build();

        log.debug("Creating EC map subnetnextobjectivestore");
        EventuallyConsistentMapBuilder<PortNextObjectiveStoreKey, Integer>
                portNextObjMapBuilder = storageService.eventuallyConsistentMapBuilder();
        portNextObjStore = portNextObjMapBuilder
                .withName("portnextobjectivestore")
                .withSerializer(kryoBuilder)
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .build();

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

        EventuallyConsistentMapBuilder<SubnetAssignedVidStoreKey, VlanId>
            subnetVidStoreMapBuilder = storageService.eventuallyConsistentMapBuilder();
        subnetVidStore = subnetVidStoreMapBuilder
                .withName("subnetvidstore")
                .withSerializer(kryoBuilder)
                .withTimestampProvider((k, v) -> new WallClockTimestamp())
                .build();

        cfgService.addListener(cfgListener);
        cfgService.registerConfigFactory(cfgFactory);

        hostService.addListener(hostListener);

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
     * Returns the vlan-id assigned to the subnet configured for a device.
     * If no vlan-id has been assigned, a new one is assigned out of a pool of ids,
     * if and only if this controller instance is the master for the device.
     * <p>
     * USAGE: The assigned vlans are meant to be applied to untagged packets on those
     * switches/pipelines that need this functionality. These vids are meant
     * to be used internally within a switch, and thus need to be unique only
     * on a switch level. Note that packets never go out on the wire with these
     * vlans. Currently, vlan ids are assigned from value 4093 down.
     * Vlan id 4094 expected to be used for all ports that are not assigned subnets.
     * Vlan id 4095 is reserved and unused. Only a single vlan id is assigned
     * per subnet.
     * XXX This method should avoid any vlans configured on the ports, but
     *     currently the app works only on untagged packets and as a result
     *     ignores any vlan configuration.
     *
     * @param deviceId switch dpid
     * @param subnet IPv4 prefix for which assigned vlan is desired
     * @return VlanId assigned for the subnet on the device, or
     *         null if no vlan assignment was found and this instance is not
     *         the master for the device.
     */
    public VlanId getSubnetAssignedVlanId(DeviceId deviceId, Ip4Prefix subnet) {
        VlanId assignedVid = subnetVidStore.get(new SubnetAssignedVidStoreKey(
                                                        deviceId, subnet));
        if (assignedVid != null) {
            log.debug("Query for subnet:{} on device:{} returned assigned-vlan "
                    + "{}", subnet, deviceId, assignedVid);
            return assignedVid;
        }
        //check mastership for the right to assign a vlan
        if (!mastershipService.isLocalMaster(deviceId)) {
            log.warn("This controller instance is not the master for device {}. "
                    + "Cannot assign vlan-id for subnet {}", deviceId, subnet);
            return null;
        }
        // vlan assignment is expensive but done only once
        Set<Ip4Prefix> configuredSubnets = deviceConfiguration.getSubnets(deviceId);
        Set<Short> assignedVlans = new HashSet<>();
        Set<Ip4Prefix> unassignedSubnets = new HashSet<>();
        for (Ip4Prefix sub : configuredSubnets) {
            VlanId v = subnetVidStore.get(new SubnetAssignedVidStoreKey(deviceId,
                                                                        sub));
            if (v != null) {
                assignedVlans.add(v.toShort());
            } else {
                unassignedSubnets.add(sub);
            }
        }
        short nextAssignedVlan = ASSIGNED_VLAN_START;
        if (!assignedVlans.isEmpty()) {
            nextAssignedVlan = (short) (Collections.min(assignedVlans) - 1);
        }
        for (Ip4Prefix unsub : unassignedSubnets) {
            subnetVidStore.put(new SubnetAssignedVidStoreKey(deviceId, unsub),
                               VlanId.vlanId(nextAssignedVlan--));
            log.info("Assigned vlan: {} to subnet: {} on device: {}",
                      nextAssignedVlan + 1, unsub, deviceId);
        }

        return subnetVidStore.get(new SubnetAssignedVidStoreKey(deviceId, subnet));
    }

    /**
     * Returns the next objective ID for the given NeighborSet.
     * If the nextObjective does not exist, a new one is created and
     * its id is returned.
     *
     * @param deviceId Device ID
     * @param ns NegighborSet
     * @param meta metadata passed into the creation of a Next Objective
     * @return next objective ID or -1 if an error was encountered during the
     *         creation of the nextObjective
     */
    public int getNextObjectiveId(DeviceId deviceId, NeighborSet ns,
                                  TrafficSelector meta) {
        if (groupHandlerMap.get(deviceId) != null) {
            log.trace("getNextObjectiveId query in device {}", deviceId);
            return groupHandlerMap
                    .get(deviceId).getNextObjectiveId(ns, meta);
        } else {
            log.warn("getNextObjectiveId query - groupHandler for device {} "
                    + "not found", deviceId);
            return -1;
        }
    }

    /**
     * Returns the next objective ID for the given subnet prefix. It is expected
     * that the next-objective has been pre-created from configuration.
     *
     * @param deviceId Device ID
     * @param prefix Subnet
     * @return next objective ID or -1 if it was not found
     */
    public int getSubnetNextObjectiveId(DeviceId deviceId, IpPrefix prefix) {
        if (groupHandlerMap.get(deviceId) != null) {
            log.trace("getSubnetNextObjectiveId query in device {}", deviceId);
            return groupHandlerMap
                    .get(deviceId).getSubnetNextObjectiveId(prefix);
        } else {
            log.warn("getSubnetNextObjectiveId query - groupHandler for "
                    + "device {} not found", deviceId);
            return -1;
        }
    }

    /**
     * Returns the next objective ID for the given portNumber, given the treatment.
     * There could be multiple different treatments to the same outport, which
     * would result in different objectives. If the next object
     * does not exist, a new one is created and its id is returned.
     *
     * @param deviceId Device ID
     * @param portNum port number on device for which NextObjective is queried
     * @param treatment the actions to apply on the packets (should include outport)
     * @param meta metadata passed into the creation of a Next Objective if necessary
     * @return next objective ID or -1 if it was not found
     */
    public int getPortNextObjectiveId(DeviceId deviceId, PortNumber portNum,
                                      TrafficTreatment treatment,
                                      TrafficSelector meta) {
        DefaultGroupHandler ghdlr = groupHandlerMap.get(deviceId);
        if (ghdlr != null) {
            return ghdlr.getPortNextObjectiveId(portNum, treatment, meta);
        } else {
            log.warn("getPortNextObjectiveId query -  groupHandler for device {}"
                    + " not found", deviceId);
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
            log.trace("Rcvd pktin: {}", ethernet);
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

    @SuppressWarnings("rawtypes")
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
                    @SuppressWarnings("rawtypes")
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
                        DeviceId deviceId = ((Device) event.subject()).id();
                        if (deviceService.isAvailable(deviceId)) {
                            log.info("Processing device event {} for available device {}",
                                     event.type(), ((Device) event.subject()).id());
                            processDeviceAdded((Device) event.subject());
                        } /* else {
                            if (event.type() == DeviceEvent.Type.DEVICE_AVAILABILITY_CHANGED) {
                                // availability changed and not available - dev gone
                                DefaultGroupHandler groupHandler = groupHandlerMap.get(deviceId);
                                if (groupHandler != null) {
                                    groupHandler.removeAllGroups();
                                }
                            }
                        }*/
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
        if (!deviceConfiguration.isConfigured(link.src().deviceId())) {
            log.warn("Source device of this link is not configured.");
            return;
        }
        //Irrespective whether the local is a MASTER or not for this device,
        //create group handler instance and push default TTP flow rules.
        //Because in a multi-instance setup, instances can initiate
        //groups for any devices. Also the default TTP rules are needed
        //to be pushed before inserting any IP table entries for any device
        DefaultGroupHandler groupHandler = groupHandlerMap.get(link.src()
                .deviceId());
        if (groupHandler != null) {
            groupHandler.linkUp(link, mastershipService.isLocalMaster(
                                           link.src().deviceId()));
        } else {
            Device device = deviceService.getDevice(link.src().deviceId());
            if (device != null) {
                log.warn("processLinkAdded: Link Added "
                        + "Notification without Device Added "
                        + "event, still handling it");
                processDeviceAdded(device);
                groupHandler = groupHandlerMap.get(link.src()
                                                   .deviceId());
                groupHandler.linkUp(link, mastershipService.isLocalMaster(device.id()));
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
            groupHandler.portDown(link.src().port(),
                                  mastershipService.isLocalMaster(link.src().deviceId()));
        }
        log.trace("Starting optimized route population process");
        defaultRoutingHandler.populateRoutingRulesForLinkStatusChange(link);
        //log.trace("processLinkRemoved: re-starting route population process");
        //defaultRoutingHandler.startPopulationProcess();
    }

    private void processDeviceAdded(Device device) {
        log.debug("A new device with ID {} was added", device.id());
        if (deviceConfiguration == null || !deviceConfiguration.isConfigured(device.id())) {
            log.warn("Device configuration uploading. Device {} will be "
                    + "processed after config completes.", device.id());
            return;
        }
        // Irrespective of whether the local is a MASTER or not for this device,
        // we need to create a SR-group-handler instance. This is because in a
        // multi-instance setup, any instance can initiate forwarding/next-objectives
        // for any switch (even if this instance is a SLAVE or not even connected
        // to the switch). To handle this, a default-group-handler instance is necessary
        // per switch.
        if (groupHandlerMap.get(device.id()) == null) {
            DefaultGroupHandler groupHandler;
            try {
                groupHandler = DefaultGroupHandler.
                        createGroupHandler(device.id(),
                                           appId,
                                           deviceConfiguration,
                                           linkService,
                                           flowObjectiveService,
                                           nsNextObjStore,
                                           subnetNextObjStore,
                                           portNextObjStore);
            } catch (DeviceConfigNotFoundException e) {
                log.warn(e.getMessage() + " Aborting processDeviceAdded.");
                return;
            }
            groupHandlerMap.put(device.id(), groupHandler);
            // Also, in some cases, drivers may need extra
            // information to process rules (eg. Router IP/MAC); and so, we send
            // port addressing rules to the driver as well irrespective of whether
            // this instance is the master or not.
            defaultRoutingHandler.populatePortAddressingRules(device.id());
            hostListener.readInitialHosts();
        }
        if (mastershipService.isLocalMaster(device.id())) {
            DefaultGroupHandler groupHandler = groupHandlerMap.get(device.id());
            groupHandler.createGroupsFromSubnetConfig();
            routingRulePopulator.populateSubnetBroadcastRule(device.id());
        }
    }

    private void processPortRemoved(Device device, Port port) {
        log.debug("Port {} was removed", port.toString());
        DefaultGroupHandler groupHandler = groupHandlerMap.get(device.id());
        if (groupHandler != null) {
            groupHandler.portDown(port.number(),
                                  mastershipService.isLocalMaster(device.id()));
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
                // Irrespective of whether the local is a MASTER or not for this device,
                // we need to create a SR-group-handler instance. This is because in a
                // multi-instance setup, any instance can initiate forwarding/next-objectives
                // for any switch (even if this instance is a SLAVE or not even connected
                // to the switch). To handle this, a default-group-handler instance is necessary
                // per switch.
                if (groupHandlerMap.get(device.id()) == null) {
                    DefaultGroupHandler groupHandler;
                    try {
                        groupHandler = DefaultGroupHandler.
                                createGroupHandler(device.id(),
                                                   appId,
                                                   deviceConfiguration,
                                                   linkService,
                                                   flowObjectiveService,
                                                   nsNextObjStore,
                                                   subnetNextObjStore,
                                                   portNextObjStore);
                    } catch (DeviceConfigNotFoundException e) {
                        log.warn(e.getMessage() + " Aborting configureNetwork.");
                        return;
                    }
                    groupHandlerMap.put(device.id(), groupHandler);

                    // Also, in some cases, drivers may need extra
                    // information to process rules (eg. Router IP/MAC); and so, we send
                    // port addressing rules to the driver as well, irrespective of whether
                    // this instance is the master or not.
                    defaultRoutingHandler.populatePortAddressingRules(device.id());
                    hostListener.readInitialHosts();
                }
                if (mastershipService.isLocalMaster(device.id())) {
                    DefaultGroupHandler groupHandler = groupHandlerMap.get(device.id());
                    groupHandler.createGroupsFromSubnetConfig();
                    routingRulePopulator.populateSubnetBroadcastRule(device.id());
                }
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

    // TODO Move bridging table population to a separate class
    private class InternalHostListener implements HostListener {
        private void readInitialHosts() {
            hostService.getHosts().forEach(host -> {
                MacAddress mac = host.mac();
                VlanId vlanId = host.vlan();
                DeviceId deviceId = host.location().deviceId();
                PortNumber port = host.location().port();
                Set<IpAddress> ips = host.ipAddresses();
                log.debug("Host {}/{} is added at {}:{}", mac, vlanId, deviceId, port);

                // Populate bridging table entry
                ForwardingObjective.Builder fob =
                        getForwardingObjectiveBuilder(deviceId, mac, vlanId, port);
                flowObjectiveService.forward(deviceId, fob.add(
                        new BridgingTableObjectiveContext(mac, vlanId)
                ));

                // Populate IP table entry
                ips.forEach(ip -> {
                    if (ip.isIp4()) {
                        routingRulePopulator.populateIpRuleForHost(
                                deviceId, ip.getIp4Address(), mac, port);
                    }
                });
            });
        }

        private ForwardingObjective.Builder getForwardingObjectiveBuilder(
                     DeviceId deviceId, MacAddress mac, VlanId vlanId,
                     PortNumber outport) {
            // match rule
            TrafficSelector.Builder sbuilder = DefaultTrafficSelector.builder();
            sbuilder.matchEthDst(mac);
            sbuilder.matchVlanId(vlanId);

            TrafficTreatment.Builder tbuilder = DefaultTrafficTreatment.builder();
            tbuilder.immediate().popVlan();
            tbuilder.immediate().setOutput(outport);

            // for switch pipelines that need it, provide outgoing vlan as metadata
            VlanId outvlan = null;
            Ip4Prefix subnet = deviceConfiguration.getPortSubnet(deviceId, outport);
            if (subnet == null) {
                outvlan = VlanId.vlanId(ASSIGNED_VLAN_NO_SUBNET);
            } else {
                outvlan = getSubnetAssignedVlanId(deviceId, subnet);
            }
            TrafficSelector meta = DefaultTrafficSelector.builder()
                                        .matchVlanId(outvlan).build();

            // All forwarding is via Groups. Drivers can re-purpose to flow-actions if needed.
            int portNextObjId = getPortNextObjectiveId(deviceId, outport,
                                                       tbuilder.build(),
                                                       meta);

            return DefaultForwardingObjective.builder()
                    .withFlag(ForwardingObjective.Flag.SPECIFIC)
                    .withSelector(sbuilder.build())
                    .nextStep(portNextObjId)
                    .withPriority(100)
                    .fromApp(appId)
                    .makePermanent();
        }

        private void processHostAddedEvent(HostEvent event) {
            MacAddress mac = event.subject().mac();
            VlanId vlanId = event.subject().vlan();
            DeviceId deviceId = event.subject().location().deviceId();
            PortNumber port = event.subject().location().port();
            Set<IpAddress> ips = event.subject().ipAddresses();
            log.info("Host {}/{} is added at {}:{}", mac, vlanId, deviceId, port);

            // Populate bridging table entry
            log.debug("Populate L2 table entry for host {} at {}:{}",
                      mac, deviceId, port);
            ForwardingObjective.Builder fob =
                    getForwardingObjectiveBuilder(deviceId, mac, vlanId, port);
            flowObjectiveService.forward(deviceId, fob.add(
                    new BridgingTableObjectiveContext(mac, vlanId)
            ));

            // Populate IP table entry
            ips.forEach(ip -> {
                if (ip.isIp4()) {
                    routingRulePopulator.populateIpRuleForHost(
                            deviceId, ip.getIp4Address(), mac, port);
                }
            });
        }

        private void processHostRemoveEvent(HostEvent event) {
            MacAddress mac = event.subject().mac();
            VlanId vlanId = event.subject().vlan();
            DeviceId deviceId = event.subject().location().deviceId();
            PortNumber port = event.subject().location().port();
            Set<IpAddress> ips = event.subject().ipAddresses();
            log.debug("Host {}/{} is removed from {}:{}", mac, vlanId, deviceId, port);

            // Revoke bridging table entry
            ForwardingObjective.Builder fob =
                    getForwardingObjectiveBuilder(deviceId, mac, vlanId, port);
            flowObjectiveService.forward(deviceId, fob.remove(
                    new BridgingTableObjectiveContext(mac, vlanId)
            ));

            // Revoke IP table entry
            ips.forEach(ip -> {
                if (ip.isIp4()) {
                    routingRulePopulator.revokeIpRuleForHost(
                            deviceId, ip.getIp4Address(), mac, port);
                }
            });
        }

        private void processHostMovedEvent(HostEvent event) {
            MacAddress mac = event.subject().mac();
            VlanId vlanId = event.subject().vlan();
            DeviceId prevDeviceId = event.prevSubject().location().deviceId();
            PortNumber prevPort = event.prevSubject().location().port();
            Set<IpAddress> prevIps = event.prevSubject().ipAddresses();
            DeviceId newDeviceId = event.subject().location().deviceId();
            PortNumber newPort = event.subject().location().port();
            Set<IpAddress> newIps = event.subject().ipAddresses();
            log.debug("Host {}/{} is moved from {}:{} to {}:{}",
                    mac, vlanId, prevDeviceId, prevPort, newDeviceId, newPort);

            // Revoke previous bridging table entry
            ForwardingObjective.Builder prevFob =
                    getForwardingObjectiveBuilder(prevDeviceId, mac, vlanId, prevPort);
            flowObjectiveService.forward(prevDeviceId, prevFob.remove(
                    new BridgingTableObjectiveContext(mac, vlanId)
            ));

            // Revoke previous IP table entry
            prevIps.forEach(ip -> {
                if (ip.isIp4()) {
                    routingRulePopulator.revokeIpRuleForHost(
                            prevDeviceId, ip.getIp4Address(), mac, prevPort);
                }
            });

            // Populate new bridging table entry
            ForwardingObjective.Builder newFob =
                    getForwardingObjectiveBuilder(newDeviceId, mac, vlanId, newPort);
            flowObjectiveService.forward(newDeviceId, newFob.add(
                    new BridgingTableObjectiveContext(mac, vlanId)
            ));

            // Populate new IP table entry
            newIps.forEach(ip -> {
                if (ip.isIp4()) {
                    routingRulePopulator.populateIpRuleForHost(
                            newDeviceId, ip.getIp4Address(), mac, newPort);
                }
            });
        }

        private void processHostUpdatedEvent(HostEvent event) {
            MacAddress mac = event.subject().mac();
            VlanId vlanId = event.subject().vlan();
            DeviceId prevDeviceId = event.prevSubject().location().deviceId();
            PortNumber prevPort = event.prevSubject().location().port();
            Set<IpAddress> prevIps = event.prevSubject().ipAddresses();
            DeviceId newDeviceId = event.subject().location().deviceId();
            PortNumber newPort = event.subject().location().port();
            Set<IpAddress> newIps = event.subject().ipAddresses();
            log.debug("Host {}/{} is updated", mac, vlanId);

            // Revoke previous IP table entry
            prevIps.forEach(ip -> {
                if (ip.isIp4()) {
                    routingRulePopulator.revokeIpRuleForHost(
                            prevDeviceId, ip.getIp4Address(), mac, prevPort);
                }
            });

            // Populate new IP table entry
            newIps.forEach(ip -> {
                if (ip.isIp4()) {
                    routingRulePopulator.populateIpRuleForHost(
                            newDeviceId, ip.getIp4Address(), mac, newPort);
                }
            });
        }

        @Override
        public void event(HostEvent event) {
            // Do not proceed without mastership
            DeviceId deviceId = event.subject().location().deviceId();
            if (!mastershipService.isLocalMaster(deviceId)) {
                return;
            }

            switch (event.type()) {
                case HOST_ADDED:
                    processHostAddedEvent(event);
                    break;
                case HOST_MOVED:
                    processHostMovedEvent(event);
                    break;
                case HOST_REMOVED:
                    processHostRemoveEvent(event);
                    break;
                case HOST_UPDATED:
                    processHostUpdatedEvent(event);
                    break;
                default:
                    log.warn("Unsupported host event type: {}", event.type());
                    break;
            }
        }
    }

    private static class BridgingTableObjectiveContext implements ObjectiveContext {
        final MacAddress mac;
        final VlanId vlanId;

        BridgingTableObjectiveContext(MacAddress mac, VlanId vlanId) {
            this.mac = mac;
            this.vlanId = vlanId;
        }

        @Override
        public void onSuccess(Objective objective) {
            if (objective.op() == Objective.Operation.ADD) {
                log.debug("Successfully populate bridging table entry for {}/{}",
                        mac, vlanId);
            } else {
                log.debug("Successfully revoke bridging table entry for {}/{}",
                        mac, vlanId);
            }
        }

        @Override
        public void onError(Objective objective, ObjectiveError error) {
            if (objective.op() == Objective.Operation.ADD) {
                log.debug("Fail to populate bridging table entry for {}/{}. {}",
                        mac, vlanId, error);
            } else {
                log.debug("Fail to revoke bridging table entry for {}/{}. {}",
                         mac, vlanId, error);
            }
        }
    }
}
