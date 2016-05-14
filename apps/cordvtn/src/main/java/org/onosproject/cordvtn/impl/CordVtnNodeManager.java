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
package org.onosproject.cordvtn.impl;

import com.google.common.collect.Sets;
import com.jcraft.jsch.Session;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.IpAddress;
import org.onlab.util.ItemNotFoundException;
import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.cordvtn.api.ConnectionHandler;
import org.onosproject.cordvtn.api.CordVtnConfig;
import org.onosproject.cordvtn.api.CordVtnNode;
import org.onosproject.cordvtn.api.CordVtnNodeState;
import org.onosproject.cordvtn.api.CordVtnService;
import org.onosproject.cordvtn.api.NetworkAddress;
import org.onosproject.cordvtn.api.SshAccessInfo;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.Port;
import org.onosproject.net.behaviour.BridgeConfig;
import org.onosproject.net.behaviour.BridgeName;
import org.onosproject.net.behaviour.ControllerInfo;
import org.onosproject.net.behaviour.DefaultTunnelDescription;
import org.onosproject.net.behaviour.TunnelConfig;
import org.onosproject.net.behaviour.TunnelDescription;
import org.onosproject.net.behaviour.TunnelName;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.device.DeviceAdminService;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.group.GroupService;
import org.onosproject.net.host.HostService;
import org.onosproject.ovsdb.controller.OvsdbClientService;
import org.onosproject.ovsdb.controller.OvsdbController;
import org.onosproject.ovsdb.controller.OvsdbNodeId;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.cordvtn.impl.RemoteIpCommandUtil.*;
import static org.onosproject.net.Device.Type.SWITCH;
import static org.onosproject.net.behaviour.TunnelDescription.Type.VXLAN;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Reads node information from the network config file and handles the config
 * update events.
 * Only a leader controller performs the node addition or deletion.
 */
@Component(immediate = true)
@Service(value = CordVtnNodeManager.class)
public class CordVtnNodeManager {

    protected final Logger log = getLogger(getClass());

    private static final KryoNamespace.Builder NODE_SERIALIZER = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(CordVtnNode.class)
            .register(NodeState.class)
            .register(SshAccessInfo.class)
            .register(NetworkAddress.class);

    private static final String DEFAULT_BRIDGE = "br-int";
    private static final String DEFAULT_TUNNEL = "vxlan";
    private static final String VPORT_PREFIX = "tap";
    private static final String OK = "OK";
    private static final String NO = "NO";

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
    protected NetworkConfigRegistry configRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceAdminService adminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OvsdbController controller;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected GroupService groupService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CordVtnService cordVtnService;

    private final ExecutorService eventExecutor =
            newSingleThreadScheduledExecutor(groupedThreads("onos/cordvtncfg", "event-handler"));

    private final NetworkConfigListener configListener = new InternalConfigListener();
    private final DeviceListener deviceListener = new InternalDeviceListener();
    private final MapEventListener<String, CordVtnNode> nodeStoreListener = new InternalMapListener();

    private final OvsdbHandler ovsdbHandler = new OvsdbHandler();
    private final BridgeHandler bridgeHandler = new BridgeHandler();

    private ConsistentMap<String, CordVtnNode> nodeStore;
    private CordVtnRuleInstaller ruleInstaller;
    private ApplicationId appId;
    private NodeId localNodeId;

    private enum NodeState implements CordVtnNodeState {

        INIT {
            @Override
            public void process(CordVtnNodeManager nodeManager, CordVtnNode node) {
                if (!nodeManager.isOvsdbConnected(node)) {
                    nodeManager.connectOvsdb(node);
                } else {
                    nodeManager.createIntegrationBridge(node);
                }
            }
        },
        BRIDGE_CREATED {
            @Override
            public void process(CordVtnNodeManager nodeManager, CordVtnNode node) {
                if (!nodeManager.isOvsdbConnected(node)) {
                    nodeManager.connectOvsdb(node);
                } else {
                    nodeManager.createTunnelInterface(node);
                    nodeManager.addDataPlaneInterface(node);
                }
            }
        },
        PORTS_ADDED {
            @Override
            public void process(CordVtnNodeManager nodeManager, CordVtnNode node) {
                nodeManager.setIpAddress(node);
            }
        },
        COMPLETE {
            @Override
            public void process(CordVtnNodeManager nodeManager, CordVtnNode node) {
                nodeManager.postInit(node);
            }
        },
        INCOMPLETE {
            @Override
            public void process(CordVtnNodeManager nodeManager, CordVtnNode node) {
            }
        };

        public abstract void process(CordVtnNodeManager nodeManager, CordVtnNode node);
    }

    @Activate
    protected void activate() {
        appId = coreService.getAppId(CordVtnService.CORDVTN_APP_ID);
        localNodeId = clusterService.getLocalNode().id();
        leadershipService.runForLeadership(appId.name());

        nodeStore = storageService.<String, CordVtnNode>consistentMapBuilder()
                .withSerializer(Serializer.using(NODE_SERIALIZER.build()))
                .withName("cordvtn-nodestore")
                .withApplicationId(appId)
                .build();

        ruleInstaller = new CordVtnRuleInstaller(appId, flowRuleService,
                                                 deviceService,
                                                 groupService,
                                                 hostService,
                                                 configRegistry,
                                                 DEFAULT_TUNNEL);

        nodeStore.addListener(nodeStoreListener);
        deviceService.addListener(deviceListener);
        configService.addListener(configListener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        configService.removeListener(configListener);
        deviceService.removeListener(deviceListener);
        nodeStore.removeListener(nodeStoreListener);

        leadershipService.withdraw(appId.name());
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    /**
     * Adds or updates a new node to the service.
     *
     * @param node cordvtn node
     */
    public void addOrUpdateNode(CordVtnNode node) {
        checkNotNull(node);
        nodeStore.put(node.hostname(), CordVtnNode.getUpdatedNode(node, getNodeState(node)));
    }

    /**
     * Deletes a node from the service.
     *
     * @param node cordvtn node
     */
    public void deleteNode(CordVtnNode node) {
        checkNotNull(node);

        if (isOvsdbConnected(node)) {
            disconnectOvsdb(node);
        }

        nodeStore.remove(node.hostname());
    }

    /**
     * Initiates node to serve virtual tenant network.
     *
     * @param node cordvtn node
     */
    private void initNode(CordVtnNode node) {
        checkNotNull(node);

        NodeState state = (NodeState) node.state();
        log.debug("Processing node: {} state: {}", node.hostname(), state);

        state.process(this, node);
    }

    /**
     * Returns node initialization state.
     *
     * @param node cordvtn node
     * @return true if initial node setup is completed, otherwise false
     */
    public boolean isNodeInitComplete(CordVtnNode node) {
        checkNotNull(node);
        return nodeStore.containsKey(node.hostname()) && getNodeState(node).equals(NodeState.COMPLETE);
    }

    /**
     * Flush flows installed by cordvtn.
     */
    public void flushRules() {
        ruleInstaller.flushRules();
    }

    /**
     * Returns if current node state saved in nodeStore is COMPLETE or not.
     *
     * @param node cordvtn node
     * @return true if it's complete state, otherwise false
     */
    private boolean isNodeStateComplete(CordVtnNode node) {
        checkNotNull(node);

        // the state saved in nodeStore can be wrong if IP address settings are changed
        // after the node init has been completed since there's no way to detect it
        // getNodeState and checkNodeInitState always return correct answer but can be slow
        Versioned<CordVtnNode> versionedNode = nodeStore.get(node.hostname());
        CordVtnNodeState state = versionedNode.value().state();
        return state != null && state.equals(NodeState.COMPLETE);
    }

    /**
     * Returns detailed node initialization state.
     *
     * @param node cordvtn node
     * @return string including detailed node init state
     */
    public String checkNodeInitState(CordVtnNode node) {
        checkNotNull(node);

        if (!nodeStore.containsKey(node.hostname())) {
            log.warn("Node {} does not exist, add node first", node.hostname());
            return null;
        }

        Session session = connect(node.sshInfo());
        if (session == null) {
            log.debug("Failed to SSH to {}", node.hostname());
            return null;
        }

        Set<IpAddress> intBrIps = getCurrentIps(session, DEFAULT_BRIDGE);
        String result = String.format(
                "Current state : %s%n" +
                        "br-int created and connected to ONOS : %s (%s)%n" +
                        "VXLAN interface added to br-int : %s%n" +
                        "Data plane interface is added to br-int and enabled : %s (%s)%n" +
                        "IP flushed from data plane interface : %s (%s)%n" +
                        "Data plane IP added to br-int : %s (%s)%n" +
                        "Local management IP added to br-int : %s (%s)",
                node.state(),
                isBrIntCreated(node) ? OK : NO, node.intBrId(),
                isTunnelIntfCreated(node) ? OK : NO,
                isDataPlaneIntfAdded(node) ? OK : NO, node.dpIntf(),
                isInterfaceUp(session, node.dpIntf()) &&
                        getCurrentIps(session, node.dpIntf()).isEmpty() ? OK : NO, node.dpIntf(),
                intBrIps.contains(node.dpIp().ip()) ? OK : NO, node.dpIp().cidr(),
                intBrIps.contains(node.localMgmtIp().ip()) ? OK : NO, node.localMgmtIp().cidr());

        disconnect(session);

        return result;
    }

    /**
     * Returns the number of the nodes known to the service.
     *
     * @return number of nodes
     */
    public int getNodeCount() {
        return nodeStore.size();
    }

    /**
     * Returns all nodes known to the service.
     *
     * @return list of nodes
     */
    public List<CordVtnNode> getNodes() {
        return nodeStore.values().stream()
                .map(Versioned::value)
                .collect(Collectors.toList());
    }

    /**
     * Returns cordvtn node associated with a given OVSDB device.
     *
     * @param ovsdbId OVSDB device id
     * @return cordvtn node, null if it fails to find the node
     */
    private CordVtnNode getNodeByOvsdbId(DeviceId ovsdbId) {
        return getNodes().stream()
                .filter(node -> node.ovsdbId().equals(ovsdbId))
                .findFirst().orElse(null);
    }

    /**
     * Returns cordvtn node associated with a given integration bridge.
     *
     * @param bridgeId device id of integration bridge
     * @return cordvtn node, null if it fails to find the node
     */
    private CordVtnNode getNodeByBridgeId(DeviceId bridgeId) {
        return getNodes().stream()
                .filter(node -> node.intBrId().equals(bridgeId))
                .findFirst().orElse(null);
    }

    /**
     * Sets a new state for a given cordvtn node.
     *
     * @param node cordvtn node
     * @param newState new node state
     */
    private void setNodeState(CordVtnNode node, NodeState newState) {
        checkNotNull(node);

        log.debug("Changed {} state: {}", node.hostname(), newState);
        nodeStore.put(node.hostname(), CordVtnNode.getUpdatedNode(node, newState));
    }

    /**
     * Checks current state of a given cordvtn node and returns it.
     *
     * @param node cordvtn node
     * @return node state
     */
    private NodeState getNodeState(CordVtnNode node) {
        checkNotNull(node);

        if (isBrIntCreated(node) && isTunnelIntfCreated(node) &&
                isDataPlaneIntfAdded(node) && isIpAddressSet(node)) {
            return NodeState.COMPLETE;
        } else if (isDataPlaneIntfAdded(node) && isTunnelIntfCreated(node)) {
            return NodeState.PORTS_ADDED;
        } else if (isBrIntCreated(node)) {
            return NodeState.BRIDGE_CREATED;
        } else {
            return NodeState.INIT;
        }
    }

    /**
     * Performs tasks after node initialization.
     * It disconnects unnecessary OVSDB connection and installs initial flow
     * rules on the device.
     *
     * @param node cordvtn node
     */
    private void postInit(CordVtnNode node) {
        disconnectOvsdb(node);

        ruleInstaller.init(node.intBrId(), node.dpIntf(), node.dpIp().ip());

        // add existing hosts to the service
        deviceService.getPorts(node.intBrId()).stream()
                .filter(port -> getPortName(port).startsWith(VPORT_PREFIX) &&
                        port.isEnabled())
                .forEach(port -> cordVtnService.addServiceVm(node, getConnectPoint(port)));

        // remove stale hosts from the service
        hostService.getHosts().forEach(host -> {
            Port port = deviceService.getPort(host.location().deviceId(), host.location().port());
            if (port == null) {
                cordVtnService.removeServiceVm(getConnectPoint(host));
            }
        });

        log.info("Finished init {}", node.hostname());
    }

    /**
     * Returns port name.
     *
     * @param port port
     * @return port name
     */
    private String getPortName(Port port) {
        return port.annotations().value("portName");
    }

    /**
     * Returns connection state of OVSDB server for a given node.
     *
     * @param node cordvtn node
     * @return true if it is connected, false otherwise
     */
    private boolean isOvsdbConnected(CordVtnNode node) {
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
    private void connectOvsdb(CordVtnNode node) {
        checkNotNull(node);

        if (!nodeStore.containsKey(node.hostname())) {
            log.warn("Node {} does not exist", node.hostname());
            return;
        }

        if (!isOvsdbConnected(node)) {
            controller.connect(node.hostMgmtIp().ip(), node.ovsdbPort());
        }
    }

    /**
     * Disconnects OVSDB server for a given node.
     *
     * @param node cordvtn node
     */
    private void disconnectOvsdb(CordVtnNode node) {
        checkNotNull(node);

        if (!nodeStore.containsKey(node.hostname())) {
            log.warn("Node {} does not exist", node.hostname());
            return;
        }

        if (isOvsdbConnected(node)) {
            OvsdbClientService ovsdbClient = getOvsdbClient(node);
            ovsdbClient.disconnect();
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
                new OvsdbNodeId(node.hostMgmtIp().ip(), node.ovsdbPort().toInt()));
        if (ovsdbClient == null) {
            log.trace("Couldn't find OVSDB client for {}", node.hostname());
        }
        return ovsdbClient;
    }

    /**
     * Creates an integration bridge for a given node.
     *
     * @param node cordvtn node
     */
    private void createIntegrationBridge(CordVtnNode node) {
        if (isBrIntCreated(node)) {
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
            Device device = deviceService.getDevice(node.ovsdbId());
            if (device.is(BridgeConfig.class)) {
                BridgeConfig bridgeConfig =  device.as(BridgeConfig.class);
                bridgeConfig.addBridge(BridgeName.bridgeName(DEFAULT_BRIDGE), dpid, controllers);
            } else {
                log.warn("The bridging behaviour is not supported in device {}", device.id().toString());
            }
        } catch (ItemNotFoundException e) {
            log.warn("Failed to create integration bridge on {}", node.hostname());
        }
    }

    /**
     * Creates tunnel interface to the integration bridge for a given node.
     *
     * @param node cordvtn node
     */
    private void createTunnelInterface(CordVtnNode node) {
        if (isTunnelIntfCreated(node)) {
            return;
        }

        DefaultAnnotations.Builder optionBuilder = DefaultAnnotations.builder();
        for (String key : DEFAULT_TUNNEL_OPTIONS.keySet()) {
            optionBuilder.set(key, DEFAULT_TUNNEL_OPTIONS.get(key));
        }

        TunnelDescription description = new DefaultTunnelDescription(
                null, null, VXLAN, TunnelName.tunnelName(DEFAULT_TUNNEL),
                optionBuilder.build());

        try {
            Device device = deviceService.getDevice(node.ovsdbId());
            if (device.is(TunnelConfig.class)) {
                TunnelConfig tunnelConfig =  device.as(TunnelConfig.class);
                tunnelConfig.createTunnelInterface(BridgeName.bridgeName(DEFAULT_BRIDGE), description);
            } else {
                log.warn("The tunneling behaviour is not supported in device {}", device.id().toString());
            }
        } catch (ItemNotFoundException e) {
            log.warn("Failed to create tunnel interface on {}", node.hostname());
        }
    }

    /**
     * Adds data plane interface to a given node.
     *
     * @param node cordvtn node
     */
    private void addDataPlaneInterface(CordVtnNode node) {
        if (isDataPlaneIntfAdded(node)) {
            return;
        }

        Session session = connect(node.sshInfo());
        if (session == null) {
            log.debug("Failed to SSH to {}", node.hostname());
            return;
        }

        if (!isInterfaceUp(session, node.dpIntf())) {
            log.warn("Interface {} is not available", node.dpIntf());
            return;
        }
        disconnect(session);

        try {
            Device device = deviceService.getDevice(node.ovsdbId());
            if (device.is(BridgeConfig.class)) {
                BridgeConfig bridgeConfig =  device.as(BridgeConfig.class);
                bridgeConfig.addPort(BridgeName.bridgeName(DEFAULT_BRIDGE), node.dpIntf());
            } else {
                log.warn("The bridging behaviour is not supported in device {}", device.id().toString());
            }
        } catch (ItemNotFoundException e) {
            log.warn("Failed to add {} on {}", node.dpIntf(), node.hostname());
        }
    }

    /**
     * Flushes IP address from data plane interface and adds data plane IP address
     * to integration bridge.
     *
     * @param node cordvtn node
     */
    private void setIpAddress(CordVtnNode node) {
        Session session = connect(node.sshInfo());
        if (session == null) {
            log.debug("Failed to SSH to {}", node.hostname());
            return;
        }

        getCurrentIps(session, DEFAULT_BRIDGE).stream()
                .filter(ip -> !ip.equals(node.localMgmtIp().ip()))
                .filter(ip -> !ip.equals(node.dpIp().ip()))
                .forEach(ip -> deleteIp(session, ip, DEFAULT_BRIDGE));

        boolean result = flushIp(session, node.dpIntf()) &&
                setInterfaceUp(session, node.dpIntf()) &&
                addIp(session, node.dpIp(), DEFAULT_BRIDGE) &&
                addIp(session, node.localMgmtIp(), DEFAULT_BRIDGE) &&
                setInterfaceUp(session, DEFAULT_BRIDGE);

        disconnect(session);

        if (result) {
            setNodeState(node, NodeState.COMPLETE);
        }
    }

    /**
     * Checks if integration bridge exists and available.
     *
     * @param node cordvtn node
     * @return true if the bridge is available, false otherwise
     */
    private boolean isBrIntCreated(CordVtnNode node) {
        return (deviceService.getDevice(node.intBrId()) != null
                && deviceService.isAvailable(node.intBrId()));
    }

    /**
     * Checks if tunnel interface exists.
     *
     * @param node cordvtn node
     * @return true if the interface exists, false otherwise
     */
    private boolean isTunnelIntfCreated(CordVtnNode node) {
        return deviceService.getPorts(node.intBrId())
                    .stream()
                    .filter(p -> getPortName(p).contains(DEFAULT_TUNNEL) &&
                            p.isEnabled())
                    .findAny().isPresent();
    }

    /**
     * Checks if data plane interface exists.
     *
     * @param node cordvtn node
     * @return true if the interface exists, false otherwise
     */
    private boolean isDataPlaneIntfAdded(CordVtnNode node) {
        return deviceService.getPorts(node.intBrId())
                    .stream()
                    .filter(p -> getPortName(p).contains(node.dpIntf()) &&
                            p.isEnabled())
                    .findAny().isPresent();
    }

    /**
     * Checks if the IP addresses are correctly set.
     *
     * @param node cordvtn node
     * @return true if the IP is set, false otherwise
     */
    private boolean isIpAddressSet(CordVtnNode node) {
        Session session = connect(node.sshInfo());
        if (session == null) {
            log.debug("Failed to SSH to {}", node.hostname());
            return false;
        }

        Set<IpAddress> intBrIps = getCurrentIps(session, DEFAULT_BRIDGE);
        boolean result = getCurrentIps(session, node.dpIntf()).isEmpty() &&
                isInterfaceUp(session, node.dpIntf()) &&
                intBrIps.contains(node.dpIp().ip()) &&
                intBrIps.contains(node.localMgmtIp().ip()) &&
                isInterfaceUp(session, DEFAULT_BRIDGE);

        disconnect(session);
        return result;
    }

    /**
     * Returns connect point of a given port.
     *
     * @param port port
     * @return connect point
     */
    private ConnectPoint getConnectPoint(Port port) {
        return new ConnectPoint(port.element().id(), port.number());
    }

    /**
     * Returns connect point of a given host.
     *
     * @param host host
     * @return connect point
     */
    private ConnectPoint getConnectPoint(Host host) {
        return new ConnectPoint(host.location().deviceId(), host.location().port());
    }

    private class OvsdbHandler implements ConnectionHandler<Device> {

        @Override
        public void connected(Device device) {
            CordVtnNode node = getNodeByOvsdbId(device.id());
            if (node != null) {
                setNodeState(node, getNodeState(node));
            } else {
                log.debug("{} is detected on unregistered node, ignore it.", device.id());
            }
        }

        @Override
        public void disconnected(Device device) {
            if (!deviceService.isAvailable(device.id())) {
                log.debug("Device {} is disconnected", device.id());
                adminService.removeDevice(device.id());
            }
        }
    }

    private class BridgeHandler implements ConnectionHandler<Device> {

        @Override
        public void connected(Device device) {
            CordVtnNode node = getNodeByBridgeId(device.id());
            if (node != null) {
                setNodeState(node, getNodeState(node));
            } else {
                log.debug("{} is detected on unregistered node, ignore it.", device.id());
            }
        }

        @Override
        public void disconnected(Device device) {
            CordVtnNode node = getNodeByBridgeId(device.id());
            if (node != null) {
                log.debug("Integration Bridge is disconnected from {}", node.hostname());
                setNodeState(node, NodeState.INCOMPLETE);
            }
        }

        /**
         * Handles port added situation.
         * If the added port is tunnel or data plane interface, proceed to the remaining
         * node initialization. Otherwise, do nothing.
         *
         * @param port port
         */
        public void portAdded(Port port) {
            CordVtnNode node = getNodeByBridgeId((DeviceId) port.element().id());
            String portName = getPortName(port);

            if (node == null) {
                log.debug("{} is added to unregistered node, ignore it.", portName);
                return;
            }

            log.info("Port {} is added to {}", portName, node.hostname());

            if (portName.startsWith(VPORT_PREFIX)) {
                if (isNodeStateComplete(node)) {
                    cordVtnService.addServiceVm(node, getConnectPoint(port));
                } else {
                    log.debug("VM is detected on incomplete node, ignore it.", portName);
                }
            } else if (portName.contains(DEFAULT_TUNNEL) || portName.equals(node.dpIntf())) {
                setNodeState(node, getNodeState(node));
            }
        }

        /**
         * Handles port removed situation.
         * If the removed port is tunnel or data plane interface, proceed to the remaining
         * node initialization.Others, do nothing.
         *
         * @param port port
         */
        public void portRemoved(Port port) {
            CordVtnNode node = getNodeByBridgeId((DeviceId) port.element().id());
            String portName = getPortName(port);

            if (node == null) {
                return;
            }

            log.info("Port {} is removed from {}", portName, node.hostname());

            if (portName.startsWith(VPORT_PREFIX)) {
                if (isNodeStateComplete(node)) {
                    cordVtnService.removeServiceVm(getConnectPoint(port));
                } else {
                    log.debug("VM is vanished from incomplete node, ignore it.", portName);
                }
            } else if (portName.contains(DEFAULT_TUNNEL) || portName.equals(node.dpIntf())) {
                setNodeState(node, NodeState.INCOMPLETE);
            }
        }
    }

    private class InternalDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {

            NodeId leaderNodeId = leadershipService.getLeader(appId.name());
            if (!Objects.equals(localNodeId, leaderNodeId)) {
                // do not allow to proceed without leadership
                return;
            }

            Device device = event.subject();
            ConnectionHandler<Device> handler =
                    (device.type().equals(SWITCH) ? bridgeHandler : ovsdbHandler);

            switch (event.type()) {
                case PORT_ADDED:
                    eventExecutor.execute(() -> bridgeHandler.portAdded(event.port()));
                    break;
                case PORT_UPDATED:
                    if (!event.port().isEnabled()) {
                        eventExecutor.execute(() -> bridgeHandler.portRemoved(event.port()));
                    }
                    break;
                case DEVICE_ADDED:
                case DEVICE_AVAILABILITY_CHANGED:
                    if (deviceService.isAvailable(device.id())) {
                        eventExecutor.execute(() -> handler.connected(device));
                    } else {
                        eventExecutor.execute(() -> handler.disconnected(device));
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Reads cordvtn nodes from config file.
     */
    private void readConfiguration() {
        CordVtnConfig config = configRegistry.getConfig(appId, CordVtnConfig.class);
        if (config == null) {
            log.debug("No configuration found");
            return;
        }

        config.cordVtnNodes().forEach(this::addOrUpdateNode);
    }

    private class InternalConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            NodeId leaderNodeId = leadershipService.getLeader(appId.name());
            if (!Objects.equals(localNodeId, leaderNodeId)) {
                // do not allow to proceed without leadership
                return;
            }

            if (!event.configClass().equals(CordVtnConfig.class)) {
                return;
            }

            switch (event.type()) {
                case CONFIG_ADDED:
                case CONFIG_UPDATED:
                    eventExecutor.execute(CordVtnNodeManager.this::readConfiguration);
                    break;
                default:
                    break;
            }
        }
    }

    private class InternalMapListener implements MapEventListener<String, CordVtnNode> {

        @Override
        public void event(MapEvent<String, CordVtnNode> event) {
            NodeId leaderNodeId = leadershipService.getLeader(appId.name());
            if (!Objects.equals(localNodeId, leaderNodeId)) {
                // do not allow to proceed without leadership
                return;
            }

            CordVtnNode oldNode;
            CordVtnNode newNode;

            switch (event.type()) {
                case UPDATE:
                    oldNode = event.oldValue().value();
                    newNode = event.newValue().value();

                    log.info("Reloaded {}", newNode.hostname());
                    if (!newNode.equals(oldNode)) {
                        log.debug("New node: {}", newNode);
                    }
                    // performs init procedure even if the node is not changed
                    // for robustness since it's no harm to run init procedure
                    // multiple times
                    eventExecutor.execute(() -> initNode(newNode));
                    break;
                case INSERT:
                    newNode = event.newValue().value();
                    log.info("Added {}", newNode.hostname());
                    eventExecutor.execute(() -> initNode(newNode));
                    break;
                case REMOVE:
                    oldNode = event.oldValue().value();
                    log.info("Removed {}", oldNode.hostname());
                    break;
                default:
                    break;
            }
        }
    }
}
