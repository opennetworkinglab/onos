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
package org.onosproject.openstacknode;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.ItemNotFoundException;
import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.behaviour.BridgeConfig;
import org.onosproject.net.behaviour.BridgeDescription;
import org.onosproject.net.behaviour.BridgeName;
import org.onosproject.net.behaviour.ControllerInfo;
import org.onosproject.net.behaviour.DefaultBridgeDescription;
import org.onosproject.net.behaviour.DefaultTunnelDescription;
import org.onosproject.net.behaviour.TunnelConfig;
import org.onosproject.net.behaviour.TunnelDescription;
import org.onosproject.net.behaviour.TunnelName;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.net.device.DeviceAdminService;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.ovsdb.controller.OvsdbClientService;
import org.onosproject.ovsdb.controller.OvsdbController;
import org.onosproject.ovsdb.controller.OvsdbNodeId;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.Device.Type.SWITCH;
import static org.onosproject.net.behaviour.TunnelDescription.Type.VXLAN;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 * Initializes devices in compute/gateway nodes according to there type.
 */
@Component(immediate = true)
@Service
public class OpenstackNodeManager implements OpenstackNodeService {
    protected final Logger log = getLogger(getClass());
    private static final KryoNamespace.Builder NODE_SERIALIZER = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(OpenstackNode.class)
            .register(OpenstackNodeType.class)
            .register(NodeState.class);
    private static final String DEFAULT_BRIDGE = "br-int";
    private static final String DEFAULT_TUNNEL = "vxlan";
    private static final String PORT_NAME = "portName";
    private static final String OPENSTACK_NODESTORE = "openstacknode-nodestore";
    private static final String OPENSTACK_NODEMANAGER_ID = "org.onosproject.openstacknode";

    private static final Map<String, String> DEFAULT_TUNNEL_OPTIONS
            = ImmutableMap.of("key", "flow", "remote_ip", "flow");

    private static final int DPID_BEGIN = 3;
    private static final int OFPORT = 6653;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OvsdbController controller;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceAdminService adminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry configRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LeadershipService leadershipService;

    private final OvsdbHandler ovsdbHandler = new OvsdbHandler();
    private final BridgeHandler bridgeHandler = new BridgeHandler();
    private final NetworkConfigListener configListener = new InternalConfigListener();
    private final ConfigFactory configFactory =
            new ConfigFactory(SubjectFactories.APP_SUBJECT_FACTORY, OpenstackNodeConfig.class, "openstacknode") {
                @Override
                public OpenstackNodeConfig createConfig() {
                    return new OpenstackNodeConfig();
                }
            };

    private final ExecutorService eventExecutor =
            newSingleThreadScheduledExecutor(groupedThreads("onos/openstacknode", "event-handler", log));


    private final DeviceListener deviceListener = new InternalDeviceListener();

    private ApplicationId appId;
    private ConsistentMap<OpenstackNode, NodeState> nodeStore;
    private NodeId localNodeId;

    private enum NodeState {

        INIT {
            @Override
            public void process(OpenstackNodeManager openstackNodeManager, OpenstackNode node) {
                openstackNodeManager.connect(node);
            }
        },
        OVSDB_CONNECTED {
            @Override
            public void process(OpenstackNodeManager openstackNodeManager, OpenstackNode node) {
                if (!openstackNodeManager.getOvsdbConnectionState(node)) {
                    openstackNodeManager.connect(node);
                } else {
                    openstackNodeManager.createIntegrationBridge(node);
                }
            }
        },
        BRIDGE_CREATED {
            @Override
            public void process(OpenstackNodeManager openstackNodeManager, OpenstackNode node) {
                if (!openstackNodeManager.getOvsdbConnectionState(node)) {
                    openstackNodeManager.connect(node);
                } else {
                    openstackNodeManager.createTunnelInterface(node);
                }
            }
        },
        COMPLETE {
            @Override
            public void process(OpenstackNodeManager openstackNodeManager, OpenstackNode node) {
                openstackNodeManager.postInit(node);
            }
        },
        INCOMPLETE {
            @Override
            public void process(OpenstackNodeManager openstackNodeManager, OpenstackNode node) {
            }
        };

        public abstract void process(OpenstackNodeManager openstackNodeManager, OpenstackNode node);
    }

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(OPENSTACK_NODEMANAGER_ID);
        localNodeId = clusterService.getLocalNode().id();
        leadershipService.runForLeadership(appId.name());

        nodeStore = storageService.<OpenstackNode, NodeState>consistentMapBuilder()
                .withSerializer(Serializer.using(NODE_SERIALIZER.build()))
                .withName(OPENSTACK_NODESTORE)
                .withApplicationId(appId)
                .build();

        deviceService.addListener(deviceListener);
        configRegistry.registerConfigFactory(configFactory);
        configService.addListener(configListener);
        readConfiguration();

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        deviceService.removeListener(deviceListener);
        eventExecutor.shutdown();
        nodeStore.clear();

        configRegistry.unregisterConfigFactory(configFactory);
        configService.removeListener(configListener);
        leadershipService.withdraw(appId.name());

        log.info("Stopped");
    }


    @Override
    public void addNode(OpenstackNode node) {
        checkNotNull(node, "Node cannot be null");

        NodeId leaderNodeId = leadershipService.getLeader(appId.name());
        log.debug("Node init requested, localNodeId: {}, leaderNodeId: {}", localNodeId, leaderNodeId);

        //TODO: Fix any node can engage this operation.
        if (!localNodeId.equals(leaderNodeId)) {
            log.debug("Only the leaderNode can perform addNode operation");
            return;
        }
        nodeStore.putIfAbsent(node, checkNodeState(node));
        NodeState state = checkNodeState(node);
        state.process(this, node);
    }

    @Override
    public void deleteNode(OpenstackNode node) {
        checkNotNull(node, "Node cannot be null");

        if (getOvsdbConnectionState(node)) {
            disconnect(node);
        }

        nodeStore.remove(node);
    }

    @Override
    public List<OpenstackNode> getNodes(OpenstackNodeType openstackNodeType) {
        List<OpenstackNode> nodes = new ArrayList<>();
        nodes.addAll(nodeStore.keySet().stream().filter(node -> node.openstackNodeType()
                .equals(openstackNodeType)).collect(Collectors.toList()));
        return nodes;
    }

    private List<OpenstackNode> getNodesAll() {
        List<OpenstackNode> nodes = new ArrayList<>();
        nodes.addAll(nodeStore.keySet());
        return nodes;
    }

    @Override
    public boolean isComplete(OpenstackNode node) {
        checkNotNull(node, "Node cannot be null");

        if (!nodeStore.containsKey(node)) {
            log.warn("Node {} does not exist", node.hostName());
            return false;
        } else if (nodeStore.get(node).equals(NodeState.COMPLETE)) {
            return true;
        }
        return false;
    }

    /**
     * Checks current state of a given openstack node and returns it.
     *
     * @param node openstack node
     * @return node state
     */
    private NodeState checkNodeState(OpenstackNode node) {
        checkNotNull(node, "Node cannot be null");

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
     * Checks if integration bridge exists and available.
     *
     * @param node openstack node
     * @return true if the bridge is available, false otherwise
     */
    private boolean checkIntegrationBridge(OpenstackNode node) {
        return (deviceService.getDevice(node.intBrId()) != null
                && deviceService.isAvailable(node.intBrId()));
    }
    /**
     * Checks if tunnel interface exists.
     *
     * @param node openstack node
     * @return true if the interface exists, false otherwise
     */
    private boolean checkTunnelInterface(OpenstackNode node) {
        checkNotNull(node, "Node cannot be null");
        return deviceService.getPorts(node.intBrId())
                    .stream()
                    .filter(p -> p.annotations().value(PORT_NAME).contains(DEFAULT_TUNNEL) && p.isEnabled())
                    .findAny().isPresent();
    }

    /**
     * Returns connection state of OVSDB server for a given node.
     *
     * @param node openstack node
     * @return true if it is connected, false otherwise
     */
    private boolean getOvsdbConnectionState(OpenstackNode node) {
        checkNotNull(node, "Node cannot be null");

        OvsdbClientService ovsdbClient = getOvsdbClient(node);
        return deviceService.isAvailable(node.ovsdbId()) &&
                ovsdbClient != null && ovsdbClient.isConnected();
    }

    /**
     * Returns OVSDB client for a given node.
     *
     * @param node openstack node
     * @return OVSDB client, or null if it fails to get OVSDB client
     */
    private OvsdbClientService getOvsdbClient(OpenstackNode node) {
        checkNotNull(node, "Node cannot be null");

        OvsdbClientService ovsdbClient = controller.getOvsdbClient(
                new OvsdbNodeId(node.ovsdbIp(), node.ovsdbPort().toInt()));
        if (ovsdbClient == null) {
            log.debug("Couldn't find OVSDB client for {}", node.hostName());
        }
        return ovsdbClient;
    }

    /**
     * Connects to OVSDB server for a given node.
     *
     * @param node openstack node
     */
    private void connect(OpenstackNode node) {
        checkNotNull(node, "Node cannot be null");

        if (!nodeStore.containsKey(node)) {
            log.warn("Node {} does not exist", node.hostName());
            return;
        }

        if (!getOvsdbConnectionState(node)) {
            controller.connect(node.ovsdbIp(), node.ovsdbPort());
        }
    }

    /**
     * Creates an integration bridge for a given node.
     *
     * @param node openstack node
     */
    private void createIntegrationBridge(OpenstackNode node) {
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

            BridgeDescription bridgeDesc = DefaultBridgeDescription.builder()
                    .name(DEFAULT_BRIDGE)
                    .failMode(BridgeDescription.FailMode.SECURE)
                    .datapathId(dpid)
                    .disableInBand()
                    .controllers(controllers)
                    .build();

            bridgeConfig.addBridge(bridgeDesc);
        } catch (ItemNotFoundException e) {
            log.warn("Failed to create integration bridge on {}", node.ovsdbId());
        }
    }

    /**
     * Creates tunnel interface to the integration bridge for a given node.
     *
     * @param node openstack node
     */
    private void createTunnelInterface(OpenstackNode node) {
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
     * Performs tasks after node initialization.
     * First disconnect unnecessary OVSDB connection and then installs flow rules
     * for existing VMs if there are any.
     *
     * @param node openstack node
     */
    private void postInit(OpenstackNode node) {
        disconnect(node);
        log.info("Finished initializing {}", node.hostName());
    }

    /**
     * Sets a new state for a given openstack node.
     *
     * @param node openstack node
     * @param newState new node state
     */
    private void setNodeState(OpenstackNode node, NodeState newState) {
        checkNotNull(node, "Node cannot be null");

        log.debug("Changed {} state: {}", node.hostName(), newState.toString());

        nodeStore.put(node, newState);
        newState.process(this, node);
    }

    /**
     * Returns openstack node associated with a given OVSDB device.
     *
     * @param ovsdbId OVSDB device id
     * @return openstack node, null if it fails to find the node
     */
    private OpenstackNode getNodeByOvsdbId(DeviceId ovsdbId) {

        return getNodesAll().stream()
                .filter(node -> node.ovsdbId().equals(ovsdbId))
                .findFirst().orElse(null);
    }

    /**
     * Returns openstack node associated with a given integration bridge.
     *
     * @param bridgeId device id of integration bridge
     * @return openstack node, null if it fails to find the node
     */
    private OpenstackNode getNodeByBridgeId(DeviceId bridgeId) {
        return getNodesAll().stream()
                .filter(node -> node.intBrId().equals(bridgeId))
                .findFirst().orElse(null);
    }
    /**
     * Disconnects OVSDB server for a given node.
     *
     * @param node openstack node
     */
    private void disconnect(OpenstackNode node) {
        checkNotNull(node, "Node cannot be null");

        if (!nodeStore.containsKey(node)) {
            log.warn("Node {} does not exist", node.hostName());
            return;
        }

        if (getOvsdbConnectionState(node)) {
            OvsdbClientService ovsdbClient = getOvsdbClient(node);
            ovsdbClient.disconnect();
        }
    }

    private class InternalDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {
            NodeId leaderNodeId = leadershipService.getLeader(appId.name());

            //TODO: Fix any node can engage this operation.
            if (!localNodeId.equals(leaderNodeId)) {
                log.debug("Only the leaderNode can process events");
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
                    log.debug("Unsupported event type {}", event.type().toString());
                    break;
            }
        }
    }

    private class OvsdbHandler implements ConnectionHandler<Device> {

        @Override
        public void connected(Device device) {
            OpenstackNode node = getNodeByOvsdbId(device.id());
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
            OpenstackNode node = getNodeByBridgeId(device.id());
            if (node != null) {
                setNodeState(node, checkNodeState(node));
            }
        }

        @Override
        public void disconnected(Device device) {
            OpenstackNode node = getNodeByBridgeId(device.id());
            if (node != null) {
                log.debug("Integration Bridge is disconnected from {}", node.hostName());
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
            if (!port.annotations().value(PORT_NAME).contains(DEFAULT_TUNNEL)) {
                return;
            }

            OpenstackNode node = getNodeByBridgeId((DeviceId) port.element().id());
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
            if (!port.annotations().value(PORT_NAME).contains(DEFAULT_TUNNEL)) {
                return;
            }

            OpenstackNode node = getNodeByBridgeId((DeviceId) port.element().id());
            if (node != null) {
                log.info("Tunnel interface is removed from {}", node.hostName());
                setNodeState(node, NodeState.INCOMPLETE);
            }
        }
    }


    private void readConfiguration() {
        OpenstackNodeConfig config =
                configService.getConfig(appId, OpenstackNodeConfig.class);

        if (config == null) {
            log.error("No configuration found");
            return;
        }

        config.openstackNodes().stream().forEach(node -> addNode(node));
        log.info("Node configured");
    }

    private class InternalConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            if (!event.configClass().equals(OpenstackNodeConfig.class)) {
                return;
            }

            switch (event.type()) {
                case CONFIG_ADDED:
                case CONFIG_UPDATED:
                    eventExecutor.execute(OpenstackNodeManager.this::readConfiguration);
                    break;
                default:
                    break;
            }
        }
    }


}

