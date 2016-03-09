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
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.group.GroupService;
import org.onosproject.net.host.HostService;
import org.onosproject.ovsdb.controller.OvsdbClientService;
import org.onosproject.ovsdb.controller.OvsdbController;
import org.onosproject.ovsdb.controller.OvsdbNodeId;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
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
            .register(KryoNamespaces.MISC)
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
    protected DriverService driverService;

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
    protected void active() {
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
                                                 driverService,
                                                 groupService,
                                                 configRegistry,
                                                 DEFAULT_TUNNEL);

        deviceService.addListener(deviceListener);
        configService.addListener(configListener);
    }

    @Deactivate
    protected void deactivate() {
        configService.removeListener(configListener);
        deviceService.removeListener(deviceListener);

        eventExecutor.shutdown();
        nodeStore.clear();
        leadershipService.withdraw(appId.name());
    }

    /**
     * Adds a new node to the service.
     *
     * @param node cordvtn node
     */
    public void addNode(CordVtnNode node) {
        checkNotNull(node);

        // allow update node attributes
        nodeStore.put(node.hostname(), CordVtnNode.getUpdatedNode(node, getNodeState(node)));
        initNode(node);
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
    public void initNode(CordVtnNode node) {
        checkNotNull(node);

        if (!nodeStore.containsKey(node.hostname())) {
            log.warn("Node {} does not exist, add node first", node.hostname());
            return;
        }

        NodeId leaderNodeId = leadershipService.getLeader(appId.name());
        log.debug("Node init requested, local: {} leader: {}", localNodeId, leaderNodeId);
        if (!Objects.equals(localNodeId, leaderNodeId)) {
            // only the leader performs node init
            return;
        }

        NodeState state = getNodeState(node);
        log.debug("Init node: {} state: {}", node.hostname(), state.toString());
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

        Session session = RemoteIpCommandUtil.connect(node.sshInfo());
        if (session == null) {
            log.debug("Failed to SSH to {}", node.hostname());
            return null;
        }

        Set<IpAddress> intBrIps = RemoteIpCommandUtil.getCurrentIps(session, DEFAULT_BRIDGE);
        String result = String.format(
                "br-int created and connected : %s (%s)%n" +
                        "VXLAN interface created : %s%n" +
                        "Data plane interface added : %s (%s)%n" +
                        "IP flushed from %s : %s%n" +
                        "Data plane IP added to br-int : %s (%s)%n" +
                        "Local management IP added to br-int : %s (%s)",
                isBrIntCreated(node) ? OK : NO, node.intBrId(),
                isTunnelIntfCreated(node) ? OK : NO,
                isDataPlaneIntfAdded(node) ? OK : NO, node.dpIntf(),
                node.dpIntf(),
                RemoteIpCommandUtil.getCurrentIps(session, node.dpIntf()).isEmpty() ? OK : NO,
                intBrIps.contains(node.dpIp().ip()) ? OK : NO, node.dpIp().cidr(),
                intBrIps.contains(node.localMgmtIp().ip()) ? OK : NO, node.localMgmtIp().cidr());

        RemoteIpCommandUtil.disconnect(session);

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

        log.debug("Changed {} state: {}", node.hostname(), newState.toString());
        nodeStore.put(node.hostname(), CordVtnNode.getUpdatedNode(node, newState));
        newState.process(this, node);
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
            DriverHandler handler = driverService.createHandler(node.ovsdbId());
            BridgeConfig bridgeConfig =  handler.behaviour(BridgeConfig.class);
            bridgeConfig.addBridge(BridgeName.bridgeName(DEFAULT_BRIDGE), dpid, controllers);
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
            DriverHandler handler = driverService.createHandler(node.ovsdbId());
            TunnelConfig tunnelConfig =  handler.behaviour(TunnelConfig.class);
            tunnelConfig.createTunnelInterface(BridgeName.bridgeName(DEFAULT_BRIDGE), description);
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

        try {
            DriverHandler handler = driverService.createHandler(node.ovsdbId());
            BridgeConfig bridgeConfig =  handler.behaviour(BridgeConfig.class);
            bridgeConfig.addPort(BridgeName.bridgeName(DEFAULT_BRIDGE), node.dpIntf());
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
        Session session = RemoteIpCommandUtil.connect(node.sshInfo());
        if (session == null) {
            log.debug("Failed to SSH to {}", node.hostname());
            return;
        }

        RemoteIpCommandUtil.getCurrentIps(session, DEFAULT_BRIDGE).stream()
                .filter(ip -> !ip.equals(node.localMgmtIp().ip()))
                .filter(ip -> !ip.equals(node.dpIp().ip()))
                .forEach(ip -> RemoteIpCommandUtil.deleteIp(session, ip, DEFAULT_BRIDGE));

        boolean result = RemoteIpCommandUtil.flushIp(session, node.dpIntf()) &&
                RemoteIpCommandUtil.setInterfaceUp(session, node.dpIntf()) &&
                RemoteIpCommandUtil.addIp(session, node.dpIp(), DEFAULT_BRIDGE) &&
                RemoteIpCommandUtil.addIp(session, node.localMgmtIp(), DEFAULT_BRIDGE) &&
                RemoteIpCommandUtil.setInterfaceUp(session, DEFAULT_BRIDGE);

        RemoteIpCommandUtil.disconnect(session);

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
        Session session = RemoteIpCommandUtil.connect(node.sshInfo());
        if (session == null) {
            log.debug("Failed to SSH to {}", node.hostname());
            return false;
        }

        Set<IpAddress> intBrIps = RemoteIpCommandUtil.getCurrentIps(session, DEFAULT_BRIDGE);
        boolean result = RemoteIpCommandUtil.getCurrentIps(session, node.dpIntf()).isEmpty() &&
                RemoteIpCommandUtil.isInterfaceUp(session, node.dpIntf()) &&
                intBrIps.contains(node.dpIp().ip()) &&
                intBrIps.contains(node.localMgmtIp().ip()) &&
                RemoteIpCommandUtil.isInterfaceUp(session, DEFAULT_BRIDGE);

        RemoteIpCommandUtil.disconnect(session);
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

            log.debug("Port {} is added to {}", portName, node.hostname());

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

            log.debug("Port {} is removed from {}", portName, node.hostname());

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
                // only the leader processes events
                return;
            }

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

    /**
     * Reads cordvtn nodes from config file.
     */
    private void readConfiguration() {
        CordVtnConfig config = configRegistry.getConfig(appId, CordVtnConfig.class);
        if (config == null) {
            log.debug("No configuration found");
            return;
        }

        config.cordVtnNodes().forEach(this::addNode);
        // TODO remove nodes if needed
    }

    private class InternalConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
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
}
