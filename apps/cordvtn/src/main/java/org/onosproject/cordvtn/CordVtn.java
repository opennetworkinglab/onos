/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.cordvtn;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onlab.util.ItemNotFoundException;
import org.onlab.packet.IpAddress;
import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.ClusterService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.BridgeConfig;
import org.onosproject.net.behaviour.BridgeName;
import org.onosproject.net.behaviour.ControllerInfo;
import org.onosproject.net.behaviour.DefaultTunnelDescription;
import org.onosproject.net.behaviour.TunnelConfig;
import org.onosproject.net.behaviour.TunnelDescription;
import org.onosproject.net.behaviour.TunnelName;
import org.onosproject.net.device.DeviceAdminService;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.group.GroupService;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.openstackswitching.OpenstackNetwork;
import org.onosproject.openstackswitching.OpenstackPort;
import org.onosproject.openstackswitching.OpenstackSubnet;
import org.onosproject.openstackswitching.OpenstackSwitchingService;
import org.onosproject.ovsdb.controller.OvsdbClientService;
import org.onosproject.ovsdb.controller.OvsdbController;
import org.onosproject.ovsdb.controller.OvsdbNodeId;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.Device.Type.SWITCH;
import static org.onosproject.net.behaviour.TunnelDescription.Type.VXLAN;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provisions virtual tenant networks with service chaining capability
 * in OpenStack environment.
 */
@Component(immediate = true)
@Service
public class CordVtn implements CordVtnService {

    protected final Logger log = getLogger(getClass());

    private static final int NUM_THREADS = 1;
    private static final KryoNamespace.Builder NODE_SERIALIZER = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(CordVtnNode.class)
            .register(NodeState.class);
    private static final String DEFAULT_BRIDGE = "br-int";
    private static final String VPORT_PREFIX = "tap";
    private static final String DEFAULT_TUNNEL = "vxlan";
    private static final Map<String, String> DEFAULT_TUNNEL_OPTIONS = new HashMap<String, String>() {
        {
            put("key", "flow");
            put("remote_ip", "flow");
        }
    };
    private static final int DPID_BEGIN = 3;
    private static final int OFPORT = 6653;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceAdminService adminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OvsdbController controller;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected GroupService groupService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackSwitchingService openstackService;

    private final ExecutorService eventExecutor = Executors
            .newFixedThreadPool(NUM_THREADS, groupedThreads("onos/cordvtn", "event-handler"));

    private final DeviceListener deviceListener = new InternalDeviceListener();
    private final HostListener hostListener = new InternalHostListener();
    private final PacketProcessor packetProcessor = new InternalPacketProcessor();

    private final OvsdbHandler ovsdbHandler = new OvsdbHandler();
    private final BridgeHandler bridgeHandler = new BridgeHandler();
    private final VmHandler vmHandler = new VmHandler();

    private ApplicationId appId;
    private ConsistentMap<CordVtnNode, NodeState> nodeStore;
    private Map<HostId, OpenstackNetwork> hostNetMap = Maps.newHashMap();
    private CordVtnRuleInstaller ruleInstaller;
    private CordVtnArpProxy arpProxy;

    private enum NodeState {

        INIT {
            @Override
            public void process(CordVtn cordVtn, CordVtnNode node) {
                cordVtn.connect(node);
            }
        },
        OVSDB_CONNECTED {
            @Override
            public void process(CordVtn cordVtn, CordVtnNode node) {
                if (!cordVtn.getOvsdbConnectionState(node)) {
                    cordVtn.connect(node);
                } else {
                    cordVtn.createIntegrationBridge(node);
                }
            }
        },
        BRIDGE_CREATED {
            @Override
            public void process(CordVtn cordVtn, CordVtnNode node) {
                if (!cordVtn.getOvsdbConnectionState(node)) {
                    cordVtn.connect(node);
                } else {
                    cordVtn.createTunnelInterface(node);
                }
            }
        },
        COMPLETE {
            @Override
            public void process(CordVtn cordVtn, CordVtnNode node) {
                cordVtn.postInit(node);
            }
        },
        INCOMPLETE {
            @Override
            public void process(CordVtn cordVtn, CordVtnNode node) {
            }
        };

        public abstract void process(CordVtn cordVtn, CordVtnNode node);
    }

    @Activate
    protected void activate() {
        appId = coreService.registerApplication("org.onosproject.cordvtn");
        nodeStore = storageService.<CordVtnNode, NodeState>consistentMapBuilder()
                .withSerializer(Serializer.using(NODE_SERIALIZER.build()))
                .withName("cordvtn-nodestore")
                .withApplicationId(appId)
                .build();

        ruleInstaller = new CordVtnRuleInstaller(appId, flowRuleService,
                                                 deviceService,
                                                 driverService,
                                                 groupService,
                                                 mastershipService,
                                                 DEFAULT_TUNNEL);

        arpProxy = new CordVtnArpProxy(appId, packetService);
        packetService.addProcessor(packetProcessor, PacketProcessor.director(0));
        arpProxy.requestPacket();

        deviceService.addListener(deviceListener);
        hostService.addListener(hostListener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        deviceService.removeListener(deviceListener);
        hostService.removeListener(hostListener);
        packetService.removeProcessor(packetProcessor);

        eventExecutor.shutdown();
        nodeStore.clear();

        log.info("Stopped");
    }

    @Override
    public void addNode(CordVtnNode node) {
        checkNotNull(node);

        nodeStore.putIfAbsent(node, checkNodeState(node));
        initNode(node);
    }

    @Override
    public void deleteNode(CordVtnNode node) {
        checkNotNull(node);

        if (getOvsdbConnectionState(node)) {
            disconnect(node);
        }

        nodeStore.remove(node);
    }

    @Override
    public int getNodeCount() {
        return nodeStore.size();
    }

    @Override
    public List<CordVtnNode> getNodes() {
        List<CordVtnNode> nodes = new ArrayList<>();
        nodes.addAll(nodeStore.keySet());
        return nodes;
    }

    @Override
    public void initNode(CordVtnNode node) {
        checkNotNull(node);

        if (!nodeStore.containsKey(node)) {
            log.warn("Node {} does not exist, add node first", node.hostname());
            return;
        }

        NodeState state = checkNodeState(node);
        state.process(this, node);
    }

    @Override
    public boolean getNodeInitState(CordVtnNode node) {
        checkNotNull(node);

        NodeState state = getNodeState(node);
        return state != null && state.equals(NodeState.COMPLETE);
    }

    @Override
    public void createServiceDependency(CordServiceId tServiceId, CordServiceId pServiceId) {
        CordService tService = getCordService(tServiceId);
        CordService pService = getCordService(pServiceId);

        if (tService == null || pService == null) {
            log.error("Failed to create CordService for {}", tServiceId.id());
            return;
        }

        ruleInstaller.populateServiceDependencyRules(tService, pService);
    }

    @Override
    public void removeServiceDependency(CordServiceId tServiceId, CordServiceId pServiceId) {
        CordService tService = getCordService(tServiceId);
        CordService pService = getCordService(pServiceId);

        if (tService == null || pService == null) {
            log.error("Failed to create CordService for {}", tServiceId.id());
            return;
        }

        ruleInstaller.removeServiceDependencyRules(tService, pService);
    }

    /**
     * Returns state of a given cordvtn node.
     *
     * @param node cordvtn node
     * @return node state, or null if no such node exists
     */
    private NodeState getNodeState(CordVtnNode node) {
        checkNotNull(node);

        try {
            return nodeStore.get(node).value();
        } catch (NullPointerException e) {
            log.error("Failed to get state of {}", node.hostname());
            return null;
        }
    }

    /**
     * Sets a new state for a given cordvtn node.
     *
     * @param node cordvtn node
     * @param newState new node state
     */
    private void setNodeState(CordVtnNode node, NodeState newState) {
        checkNotNull(node);

        log.info("Changed {} state: {}", node.hostname(), newState.toString());

        nodeStore.put(node, newState);
        newState.process(this, node);
    }

    /**
     * Checks current state of a given cordvtn node and returns it.
     *
     * @param node cordvtn node
     * @return node state
     */
    private NodeState checkNodeState(CordVtnNode node) {
        checkNotNull(node);

        if (checkIntegrationBridge(node) && checkTunnelInterface(node)) {
            return NodeState.COMPLETE;
        } else if (checkIntegrationBridge(node)) {
            return NodeState.BRIDGE_CREATED;
        } else if (getOvsdbConnectionState(node)) {
            return NodeState.OVSDB_CONNECTED;
        } else {
            return NodeState.INIT;
        }
    }

    /**
     * Performs tasks after node initialization.
     * First disconnect unnecessary OVSDB connection and then installs flow rules
     * for existing VMs if there are any.
     *
     * @param node cordvtn node
     */
    private void postInit(CordVtnNode node) {
        disconnect(node);

        ruleInstaller.init(node.intBrId(), getTunnelPort(node.intBrId()));
        hostService.getConnectedHosts(node.intBrId())
                .stream()
                .forEach(vmHandler::connected);

        log.info("Finished initializing {}", node.hostname());
    }

    /**
     * Returns connection state of OVSDB server for a given node.
     *
     * @param node cordvtn node
     * @return true if it is connected, false otherwise
     */
    private boolean getOvsdbConnectionState(CordVtnNode node) {
        checkNotNull(node);

        OvsdbClientService ovsdbClient = getOvsdbClient(node);
        return deviceService.isAvailable(node.ovsdbId()) &&
                ovsdbClient != null && ovsdbClient.isConnected();
    }

    /**
     * Connects to OVSDB server for a given node.
     *
     * @param node cordvtn node
     */
    private void connect(CordVtnNode node) {
        checkNotNull(node);

        if (!nodeStore.containsKey(node)) {
            log.warn("Node {} does not exist", node.hostname());
            return;
        }

        if (!getOvsdbConnectionState(node)) {
            controller.connect(node.ovsdbIp(), node.ovsdbPort());
        }
    }

    /**
     * Disconnects OVSDB server for a given node.
     *
     * @param node cordvtn node
     */
    private void disconnect(CordVtnNode node) {
        checkNotNull(node);

        if (!nodeStore.containsKey(node)) {
            log.warn("Node {} does not exist", node.hostname());
            return;
        }

        if (getOvsdbConnectionState(node)) {
            OvsdbClientService ovsdbClient = getOvsdbClient(node);
            ovsdbClient.disconnect();
        }
    }

    /**
     * Returns cordvtn node associated with a given OVSDB device.
     *
     * @param ovsdbId OVSDB device id
     * @return cordvtn node, null if it fails to find the node
     */
    private CordVtnNode getNodeByOvsdbId(DeviceId ovsdbId) {
        try {
            return getNodes().stream()
                    .filter(node -> node.ovsdbId().equals(ovsdbId))
                    .findFirst().get();
        } catch (NoSuchElementException e) {
            log.debug("Couldn't find node information for {}", ovsdbId);
            return null;
        }
    }

    /**
     * Returns cordvtn node associated with a given integration bridge.
     *
     * @param bridgeId device id of integration bridge
     * @return cordvtn node, null if it fails to find the node
     */
    private CordVtnNode getNodeByBridgeId(DeviceId bridgeId) {
        try {
            return getNodes().stream()
                    .filter(node -> node.intBrId().equals(bridgeId))
                    .findFirst().get();
        } catch (NoSuchElementException e) {
            log.debug("Couldn't find node information for {}", bridgeId);
            return null;
        }
    }

    /**
     * Returns OVSDB client for a given node.
     *
     * @param node cordvtn node
     * @return OVSDB client, or null if it fails to get OVSDB client
     */
    private OvsdbClientService getOvsdbClient(CordVtnNode node) {
        checkNotNull(node);

        OvsdbClientService ovsdbClient = controller.getOvsdbClient(
                new OvsdbNodeId(node.ovsdbIp(), node.ovsdbPort().toInt()));
        if (ovsdbClient == null) {
            log.debug("Couldn't find OVSDB client for {}", node.hostname());
        }
        return ovsdbClient;
    }

    /**
     * Creates an integration bridge for a given node.
     *
     * @param node cordvtn node
     */
    private void createIntegrationBridge(CordVtnNode node) {
        if (checkIntegrationBridge(node)) {
            return;
        }

        List<ControllerInfo> controllers = new ArrayList<>();
        Sets.newHashSet(clusterService.getNodes()).stream()
                .forEach(controller -> {
                    ControllerInfo ctrlInfo = new ControllerInfo(controller.ip(), OFPORT, "tcp");
                    controllers.add(ctrlInfo);
                });
        String dpid = node.intBrId().toString().substring(DPID_BEGIN);

        try {
            DriverHandler handler = driverService.createHandler(node.ovsdbId());
            BridgeConfig bridgeConfig =  handler.behaviour(BridgeConfig.class);
            bridgeConfig.addBridge(BridgeName.bridgeName(DEFAULT_BRIDGE), dpid, controllers);
        } catch (ItemNotFoundException e) {
            log.warn("Failed to create integration bridge on {}", node.ovsdbId());
        }
    }

    /**
     * Creates tunnel interface to the integration bridge for a given node.
     *
     * @param node cordvtn node
     */
    private void createTunnelInterface(CordVtnNode node) {
        if (checkTunnelInterface(node)) {
            return;
        }

        DefaultAnnotations.Builder optionBuilder = DefaultAnnotations.builder();
        for (String key : DEFAULT_TUNNEL_OPTIONS.keySet()) {
            optionBuilder.set(key, DEFAULT_TUNNEL_OPTIONS.get(key));
        }
        TunnelDescription description =
                new DefaultTunnelDescription(null, null, VXLAN, TunnelName.tunnelName(DEFAULT_TUNNEL),
                                             optionBuilder.build());
        try {
            DriverHandler handler = driverService.createHandler(node.ovsdbId());
            TunnelConfig tunnelConfig =  handler.behaviour(TunnelConfig.class);
            tunnelConfig.createTunnelInterface(BridgeName.bridgeName(DEFAULT_BRIDGE), description);
        } catch (ItemNotFoundException e) {
            log.warn("Failed to create tunnel interface on {}", node.ovsdbId());
        }
    }

    /**
     * Checks if integration bridge exists and available.
     *
     * @param node cordvtn node
     * @return true if the bridge is available, false otherwise
     */
    private boolean checkIntegrationBridge(CordVtnNode node) {
        return (deviceService.getDevice(node.intBrId()) != null
                && deviceService.isAvailable(node.intBrId()));
    }

    /**
     * Checks if tunnel interface exists.
     *
     * @param node cordvtn node
     * @return true if the interface exists, false otherwise
     */
    private boolean checkTunnelInterface(CordVtnNode node) {
        try {
            deviceService.getPorts(node.intBrId())
                    .stream()
                    .filter(p -> p.annotations().value("portName").contains(DEFAULT_TUNNEL)
                            && p.isEnabled())
                    .findAny().get();
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    /**
     * Returns tunnel port of the device.
     *
     * @param bridgeId device id
     * @return port number, null if no tunnel port exists on a given device
     */
    private PortNumber getTunnelPort(DeviceId bridgeId) {
        try {
            return deviceService.getPorts(bridgeId).stream()
                    .filter(p -> p.annotations().value("portName").contains(DEFAULT_TUNNEL)
                            && p.isEnabled())
                    .findFirst().get().number();
        } catch (NoSuchElementException e) {
            return null;
        }
    }

    /**
     * Returns remote ip address for tunneling.
     *
     * @param bridgeId device id
     * @return ip address, null if no such device exists
     */
    private Ip4Address getRemoteIp(DeviceId bridgeId) {
        CordVtnNode node = getNodeByBridgeId(bridgeId);
        if (node != null) {
            // TODO get data plane IP for tunneling
            return node.ovsdbIp().getIp4Address();
        } else {
            return null;
        }
    }

    /**
     * Returns OpenStack port associated with a given host.
     *
     * @param host host
     * @return OpenStack port, or null if no port has been found
     */
    private OpenstackPort getOpenstackPortByHost(Host host) {
        Port port = deviceService.getPort(host.location().deviceId(),
                                          host.location().port());
        if (port == null) {
            log.debug("Failed to get port for {}", host.id());
            return null;
        }
        return openstackService.port(port);
    }

    /**
     * Returns OpenStack network associated with a given host.
     *
     * @param host host
     * @return OpenStack network, or null if no network has been found
     */
    private OpenstackNetwork getOpenstackNetworkByHost(Host host) {
        OpenstackPort vPort = getOpenstackPortByHost(host);
        if (vPort != null) {
            return openstackService.network(vPort.networkId());
        } else {
            return null;
        }
    }

    /**
     * Returns hosts associated with a given OpenStack network.
     *
     * @param vNet openstack network
     * @return set of hosts
     */
    private Set<Host> getHostsWithOpenstackNetwork(OpenstackNetwork vNet) {
        checkNotNull(vNet);

        Set<Host> hosts = openstackService.ports(vNet.id()).stream()
                .filter(port -> port.deviceOwner().contains("compute"))
                .map(port -> hostService.getHostsByMac(port.macAddress())
                        .stream()
                        .findFirst()
                        .orElse(null))
                .collect(Collectors.toSet());

        hosts.remove(null);
        return hosts;
    }

    /**
     * Returns host IP assigned by OpenStack.
     *
     * @param host host
     * @return IPv4 prefix, or null if it fails to get IP from OpenStack
     */
    private IpAddress getHostIpFromOpenstack(Host host) {
        OpenstackPort vPort = getOpenstackPortByHost(host);

        if (vPort == null || vPort.fixedIps().isEmpty()) {
            log.error("Failed to get VM IP for {}", host.id());
            return null;
        }
        // Assumes there's only one fixed IP is assigned to a port
        return (Ip4Address) vPort.fixedIps().values()
                .stream()
                .findFirst()
                .orElse(null);
    }

    /**
     * Returns port name with OpenStack port information.
     *
     * @param vPort OpenStack port
     * @return port name
     */
    private String getPortName(OpenstackPort vPort) {
        checkNotNull(vPort);
        return VPORT_PREFIX + vPort.id().substring(0, 10);
    }

    /**
     * Returns if the host is VM or not.
     *
     * @param host host
     * @return true if the host is a VM.
     */
    private boolean isVm(Host host) {
        Port port = deviceService.getPort(host.location().deviceId(),
                                          host.location().port());
        return port.annotations().value("portName").contains(VPORT_PREFIX);
    }

    /**
     * Returns CordService by service ID.
     *
     * @param serviceId service id
     * @return cord service, or null if it fails to get network from OpenStack
     */
    private CordService getCordService(CordServiceId serviceId) {
        OpenstackNetwork vNet = openstackService.network(serviceId.id());
        if (vNet == null) {
            log.warn("Couldn't find OpenStack network for service {}", serviceId.id());
            return null;
        }

        OpenstackSubnet subnet = vNet.subnets().stream()
                .findFirst()
                .orElse(null);
        if (subnet == null) {
            log.warn("Couldn't find OpenStack subnet for service {}", serviceId.id());
            return null;
        }

        Set<CordServiceId> tServices = Sets.newHashSet();
        // TODO get tenant services from XOS

        Map<Host, IpAddress> hosts = getHostsWithOpenstackNetwork(vNet)
                .stream()
                .collect(Collectors.toMap(host -> host,
                                          host -> getRemoteIp(host.location().deviceId())));

        return new CordService(vNet, subnet, hosts, tServices);
    }

    /**
     * Returns CordService by OpenStack network.
     *
     * @param vNet OpenStack network
     * @return cord service
     */
    private CordService getCordService(OpenstackNetwork vNet) {
        checkNotNull(vNet);

        CordServiceId serviceId = CordServiceId.of(vNet.id());
        OpenstackSubnet subnet = vNet.subnets().stream()
                .findFirst()
                .orElse(null);
        if (subnet == null) {
            log.warn("Couldn't find OpenStack subnet for service {}", serviceId);
            return null;
        }

        Set<CordServiceId> tServices = Sets.newHashSet();
        // TODO get tenant services from XOS

        Map<Host, IpAddress> hosts = getHostsWithOpenstackNetwork(vNet)
                .stream()
                .collect(Collectors.toMap(host -> host,
                                          host -> getRemoteIp(host.location().deviceId())));

        return new CordService(vNet, subnet, hosts, tServices);
    }

    private class InternalDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {

            Device device = event.subject();
            ConnectionHandler<Device> handler =
                    (device.type().equals(SWITCH) ? bridgeHandler : ovsdbHandler);

            switch (event.type()) {
                case PORT_ADDED:
                    eventExecutor.submit(() -> bridgeHandler.portAdded(event.port()));
                    break;
                case PORT_UPDATED:
                    if (!event.port().isEnabled()) {
                        eventExecutor.submit(() -> bridgeHandler.portRemoved(event.port()));
                    }
                    break;
                case DEVICE_ADDED:
                case DEVICE_AVAILABILITY_CHANGED:
                    if (deviceService.isAvailable(device.id())) {
                        eventExecutor.submit(() -> handler.connected(device));
                    } else {
                        eventExecutor.submit(() -> handler.disconnected(device));
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private class InternalHostListener implements HostListener {

        @Override
        public void event(HostEvent event) {
            Host vm = event.subject();

            switch (event.type()) {
                case HOST_ADDED:
                    eventExecutor.submit(() -> vmHandler.connected(vm));
                    break;
                case HOST_REMOVED:
                    eventExecutor.submit(() -> vmHandler.disconnected(vm));
                    break;
                default:
                    break;
            }
        }
    }

    private class OvsdbHandler implements ConnectionHandler<Device> {

        @Override
        public void connected(Device device) {
            CordVtnNode node = getNodeByOvsdbId(device.id());
            if (node != null) {
                setNodeState(node, checkNodeState(node));
            }
        }

        @Override
        public void disconnected(Device device) {
            if (!deviceService.isAvailable(device.id())) {
                adminService.removeDevice(device.id());
            }
        }
    }

    private class BridgeHandler implements ConnectionHandler<Device> {

        @Override
        public void connected(Device device) {
            CordVtnNode node = getNodeByBridgeId(device.id());
            if (node != null) {
                setNodeState(node, checkNodeState(node));
            }
        }

        @Override
        public void disconnected(Device device) {
            CordVtnNode node = getNodeByBridgeId(device.id());
            if (node != null) {
                log.info("Integration Bridge is disconnected from {}", node.hostname());
                setNodeState(node, NodeState.INCOMPLETE);
            }
        }

        /**
         * Handles port added situation.
         * If the added port is tunnel port, proceed remaining node initialization.
         * Otherwise, do nothing.
         *
         * @param port port
         */
        public void portAdded(Port port) {
            // TODO add host by updating network config
            if (!port.annotations().value("portName").contains(DEFAULT_TUNNEL)) {
                return;
            }

            CordVtnNode node = getNodeByBridgeId((DeviceId) port.element().id());
            if (node != null) {
                setNodeState(node, checkNodeState(node));
            }
        }

        /**
         * Handles port removed situation.
         * If the removed port is tunnel port, proceed remaining node initialization.
         * Others, do nothing.
         *
         * @param port port
         */
        public void portRemoved(Port port) {
            // TODO remove host by updating network config
            if (!port.annotations().value("portName").contains(DEFAULT_TUNNEL)) {
                return;
            }

            CordVtnNode node = getNodeByBridgeId((DeviceId) port.element().id());
            if (node != null) {
                log.info("Tunnel interface is removed from {}", node.hostname());
                setNodeState(node, NodeState.INCOMPLETE);
            }
        }
    }

    private class VmHandler implements ConnectionHandler<Host> {

        @Override
        public void connected(Host host) {
            // TODO remove check VM here after applying network config host provider
            if (!isVm(host)) {
                log.debug("Host {} is not a VM, ignore it.", host.id());
                return;
            }

            CordVtnNode node = getNodeByBridgeId(host.location().deviceId());
            if (node == null || !Objects.equals(getNodeState(node), NodeState.COMPLETE)) {
                log.debug("VM {} is detected unknown or incomplete device, ignore it.", host.id());
                return;
            }

            OpenstackNetwork vNet = getOpenstackNetworkByHost(host);
            if (vNet == null) {
                log.debug("Failed to get OpenStack network for VM {}, ignore it.", host.id());
                return;
            }

            // TODO host ip should be set in host information after applying network config host provider
            IpAddress hostIp = getHostIpFromOpenstack(host);
            if (hostIp == null) {
                log.debug("Failed to get host IP of {}, ignore it.", host.id());
                return;
            }

            log.info("VM {} is detected", host.id());
            hostNetMap.put(host.id(), vNet);

            CordService service = getCordService(vNet);
            if (service != null) {
                // TODO check if the service needs an update on its group buckets after done CORD-433
                ruleInstaller.updateServiceGroup(service);
                arpProxy.addServiceIp(service.serviceIp());
            }

            ruleInstaller.populateBasicConnectionRules(
                    host,
                    hostIp,
                    checkNotNull(getRemoteIp(host.location().deviceId())).getIp4Address(),
                    vNet);
        }

        @Override
        public void disconnected(Host host) {
            CordVtnNode node = getNodeByBridgeId(host.location().deviceId());
            if (node == null || !Objects.equals(getNodeState(node), NodeState.COMPLETE)) {
                // do nothing for the host on unregistered or unprepared device
                return;
            }

            OpenstackNetwork vNet = hostNetMap.get(host.id());
            if (vNet == null) {
                return;
            }

            log.info("VM {} is vanished", host.id());
            ruleInstaller.removeBasicConnectionRules(host);

            CordService service = getCordService(vNet);
            if (service != null) {
                // TODO check if the service needs an update on its group buckets after done CORD-433
                ruleInstaller.updateServiceGroup(service);

                if (getHostsWithOpenstackNetwork(vNet).isEmpty()) {
                    arpProxy.removeServiceIp(service.serviceIp());
                }
            }

            hostNetMap.remove(host.id());
        }
    }

    private class InternalPacketProcessor implements PacketProcessor {

        @Override
        public void process(PacketContext context) {
            if (context.isHandled()) {
                return;
            }

            Ethernet ethPacket = context.inPacket().parsed();
            if (ethPacket == null) {
                return;
            }

            if (ethPacket.getEtherType() != Ethernet.TYPE_ARP) {
                return;
            }

            arpProxy.processArpPacket(context, ethPacket);
        }
    }
}
