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

import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.IpAddress;
import org.onlab.packet.TpPort;
import org.onlab.util.KryoNamespace;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.ListenerRegistry;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.BridgeConfig;
import org.onosproject.net.behaviour.BridgeDescription;
import org.onosproject.net.behaviour.BridgeName;
import org.onosproject.net.behaviour.ControllerInfo;
import org.onosproject.net.behaviour.DefaultBridgeDescription;
import org.onosproject.net.behaviour.DefaultPatchDescription;
import org.onosproject.net.behaviour.DefaultTunnelDescription;
import org.onosproject.net.behaviour.InterfaceConfig;
import org.onosproject.net.behaviour.PatchDescription;
import org.onosproject.net.behaviour.TunnelDescription;
import org.onosproject.net.behaviour.TunnelEndPoints;
import org.onosproject.net.behaviour.TunnelKeys;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.openstacknode.OpenstackNodeEvent.NodeState;
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
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.AnnotationKeys.PORT_NAME;
import static org.onosproject.net.Device.Type.SWITCH;
import static org.onosproject.net.behaviour.TunnelDescription.Type.VXLAN;
import static org.onosproject.openstacknode.Constants.*;
import static org.onosproject.openstacknode.OpenstackNodeEvent.NodeState.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Initializes devices in compute/gateway nodes according to there type.
 */
@Component(immediate = true)
@Service
public final class OpenstackNodeManager extends ListenerRegistry<OpenstackNodeEvent, OpenstackNodeListener>
        implements OpenstackNodeService {
    private final Logger log = getLogger(getClass());

    private static final KryoNamespace.Builder NODE_SERIALIZER = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(OpenstackNode.class)
            .register(NodeType.class)
            .register(NodeState.class);

    private static final String OVSDB_PORT = "ovsdbPort";
    private static final int DPID_BEGIN = 3;

    private static final String APP_ID = "org.onosproject.openstacknode";
    private static final Class<OpenstackNodeConfig> CONFIG_CLASS = OpenstackNodeConfig.class;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OvsdbController ovsdbController;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService componentConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry configRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LeadershipService leadershipService;

    @Property(name = OVSDB_PORT, intValue = DEFAULT_OVSDB_PORT,
            label = "OVSDB server listen port")
    private int ovsdbPort = DEFAULT_OVSDB_PORT;

    private final ExecutorService eventExecutor =
            newSingleThreadScheduledExecutor(groupedThreads("onos/openstack-node", "event-handler", log));

    private final ConfigFactory configFactory =
            new ConfigFactory<ApplicationId, OpenstackNodeConfig>(
                    SubjectFactories.APP_SUBJECT_FACTORY, CONFIG_CLASS, "openstacknode") {
                @Override
                public OpenstackNodeConfig createConfig() {
                    return new OpenstackNodeConfig();
                }
            };

    private final NetworkConfigListener configListener = new InternalConfigListener();
    private final DeviceListener deviceListener = new InternalDeviceListener();
    private final MapEventListener<String, OpenstackNode> nodeStoreListener = new InternalMapListener();

    private final OvsdbHandler ovsdbHandler = new OvsdbHandler();
    private final BridgeHandler bridgeHandler = new BridgeHandler();

    private ConsistentMap<String, OpenstackNode> nodeStore;

    private ApplicationId appId;
    private NodeId localNodeId;

    @Activate
    protected void activate() {
        appId = coreService.getAppId(APP_ID);

        localNodeId = clusterService.getLocalNode().id();
        leadershipService.runForLeadership(appId.name());

        nodeStore = storageService.<String, OpenstackNode>consistentMapBuilder()
                .withSerializer(Serializer.using(NODE_SERIALIZER.build()))
                .withName("openstack-nodestore")
                .withApplicationId(appId)
                .build();

        nodeStore.addListener(nodeStoreListener);
        deviceService.addListener(deviceListener);

        configRegistry.registerConfigFactory(configFactory);
        configRegistry.addListener(configListener);
        componentConfigService.registerProperties(getClass());

        readConfiguration();
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        configRegistry.removeListener(configListener);
        deviceService.removeListener(deviceListener);
        nodeStore.removeListener(nodeStoreListener);

        componentConfigService.unregisterProperties(getClass(), false);
        configRegistry.unregisterConfigFactory(configFactory);

        leadershipService.withdraw(appId.name());
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    @Modified
    protected void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        int updatedOvsdbPort = Tools.getIntegerProperty(properties, OVSDB_PORT);
        if (!Objects.equals(updatedOvsdbPort, ovsdbPort)) {
            ovsdbPort = updatedOvsdbPort;
        }

        log.info("Modified");
    }

    @Override
    public void addOrUpdateNode(OpenstackNode node) {
        nodeStore.put(node.hostname(),
                OpenstackNode.getUpdatedNode(node, nodeState(node)));
    }

    @Override
    public void deleteNode(OpenstackNode node) {
        nodeStore.remove(node.hostname());
        process(new OpenstackNodeEvent(INCOMPLETE, node));
    }

    @Override
    public void processInitState(OpenstackNode node) {
        // make sure there is OVSDB connection
        if (!isOvsdbConnected(node)) {
            connectOvsdb(node);
            return;
        }
        process(new OpenstackNodeEvent(INIT, node));

        createBridge(node, INTEGRATION_BRIDGE, node.intBridge());
        if (node.type().equals(NodeType.GATEWAY)) {
            createBridge(node, ROUTER_BRIDGE, node.routerBridge().get());
            // TODO remove this when OVSDB provides port event
            setNodeState(node, nodeState(node));
        }
    }

    @Override
    public void processDeviceCreatedState(OpenstackNode node) {
        // make sure there is OVSDB connection
        if (!isOvsdbConnected(node)) {
            connectOvsdb(node);
            return;
        }
        process(new OpenstackNodeEvent(DEVICE_CREATED, node));

        createTunnelInterface(node);
        if (node.type().equals(NodeType.GATEWAY)) {
            createPatchInterface(node);
            addUplink(node);
            // TODO remove this when OVSDB provides port event
            setNodeState(node, nodeState(node));
        }
    }

    @Override
    public void processCompleteState(OpenstackNode node) {
        process(new OpenstackNodeEvent(COMPLETE, node));
        log.info("Finished init {}", node.hostname());
    }

    @Override
    public void processIncompleteState(OpenstackNode node) {
        process(new OpenstackNodeEvent(INCOMPLETE, node));
    }

    @Override
    public List<OpenstackNode> nodes() {
        return nodeStore.values().stream().map(Versioned::value).collect(Collectors.toList());
    }

    @Override
    public Set<OpenstackNode> completeNodes() {
        return nodeStore.values().stream().map(Versioned::value)
                .filter(node -> node.state().equals(COMPLETE))
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<IpAddress> dataIp(DeviceId deviceId) {
        OpenstackNode node = nodeByDeviceId(deviceId);
        if (node == null) {
            log.warn("Failed to get node for {}", deviceId);
            return Optional.empty();
        }
        return Optional.of(node.dataIp());
    }

    @Override
    public Optional<PortNumber> tunnelPort(DeviceId deviceId) {
        return deviceService.getPorts(deviceId).stream()
                .filter(p -> p.annotations().value(PORT_NAME).equals(DEFAULT_TUNNEL) &&
                        p.isEnabled())
                .map(Port::number).findFirst();
    }

    @Override
    public Optional<DeviceId> routerBridge(DeviceId intBridgeId) {
        OpenstackNode node = nodeByDeviceId(intBridgeId);
        if (node == null || node.type().equals(NodeType.COMPUTE)) {
            log.warn("Failed to find router bridge connected to {}", intBridgeId);
            return Optional.empty();
        }
        return node.routerBridge();
    }

    @Override
    public Optional<PortNumber> externalPort(DeviceId intBridgeId) {
        return deviceService.getPorts(intBridgeId).stream()
                .filter(p -> p.annotations().value(PORT_NAME).equals(PATCH_INTG_BRIDGE) &&
                        p.isEnabled())
                .map(Port::number).findFirst();
    }

    private void initNode(OpenstackNode node) {
        NodeState state = node.state();
        state.process(this, node);
        log.debug("Processing node: {} state: {}", node.hostname(), state);
    }

    private void setNodeState(OpenstackNode node, NodeState newState) {
        if (node.state() != newState) {
            log.debug("Changed {} state: {}", node.hostname(), newState);
            nodeStore.put(node.hostname(), OpenstackNode.getUpdatedNode(node, newState));
        }
    }

    private NodeState nodeState(OpenstackNode node) {
        if (!isOvsdbConnected(node) || !deviceService.isAvailable(node.intBridge())) {
            return INIT;
        }

        // TODO use device service when we use single ONOS cluster for both openstackNode and vRouter
        if (node.type().equals(NodeType.GATEWAY) &&
                !isBridgeCreated(node.ovsdbId(), ROUTER_BRIDGE)) {
            return INIT;
        }

        if (!isIfaceCreated(node.ovsdbId(), DEFAULT_TUNNEL)) {
            return DEVICE_CREATED;
        }

        if (node.type().equals(NodeType.GATEWAY) && (
                !isIfaceCreated(node.ovsdbId(), PATCH_ROUT_BRIDGE) ||
                !isIfaceCreated(node.ovsdbId(), PATCH_INTG_BRIDGE) ||
                !isIfaceCreated(node.ovsdbId(), node.uplink().get()))) {
            return DEVICE_CREATED;
        }
        return COMPLETE;
    }

    private boolean isIfaceCreated(DeviceId deviceId, String ifaceName) {
        Device device = deviceService.getDevice(deviceId);
        if (device == null || !device.is(BridgeConfig.class)) {
            return false;
        }

        BridgeConfig bridgeConfig =  device.as(BridgeConfig.class);
        return bridgeConfig.getPorts().stream()
                .filter(port -> port.annotations().value(PORT_NAME).equals(ifaceName))
                .findAny()
                .isPresent();
    }

    private boolean isBridgeCreated(DeviceId deviceId, String bridgeName) {
        Device device = deviceService.getDevice(deviceId);
        if (device == null || !device.is(BridgeConfig.class)) {
            return false;
        }

        BridgeConfig bridgeConfig =  device.as(BridgeConfig.class);
        return bridgeConfig.getBridges().stream()
                .filter(bridge -> bridge.name().equals(bridgeName))
                .findAny()
                .isPresent();
    }

    private void createBridge(OpenstackNode node, String bridgeName, DeviceId deviceId) {
        Device device = deviceService.getDevice(node.ovsdbId());
        if (device == null || !device.is(BridgeConfig.class)) {
            log.error("Failed to create integration bridge on {}", node.ovsdbId());
            return;
        }

        // TODO fix this when we use single ONOS cluster for both openstackNode and vRouter
        Set<IpAddress> controllerIps;
        if (bridgeName.equals(ROUTER_BRIDGE)) {
            controllerIps = Sets.newHashSet(node.routerController().get());
        } else {
            controllerIps = clusterService.getNodes().stream()
                    .map(ControllerNode::ip)
                    .collect(Collectors.toSet());
        }

        List<ControllerInfo> controllers = controllerIps.stream()
                    .map(ip -> new ControllerInfo(ip, DEFAULT_OFPORT, DEFAULT_OF_PROTO))
                    .collect(Collectors.toList());

        String dpid = deviceId.toString().substring(DPID_BEGIN);
        BridgeDescription bridgeDesc = DefaultBridgeDescription.builder()
                .name(bridgeName)
                .failMode(BridgeDescription.FailMode.SECURE)
                .datapathId(dpid)
                .disableInBand()
                .controllers(controllers)
                .build();

        BridgeConfig bridgeConfig = device.as(BridgeConfig.class);
        bridgeConfig.addBridge(bridgeDesc);
    }

    private void createTunnelInterface(OpenstackNode node) {
        if (isIfaceCreated(node.ovsdbId(), DEFAULT_TUNNEL)) {
            return;
        }

        Device device = deviceService.getDevice(node.ovsdbId());
        if (device == null || !device.is(InterfaceConfig.class)) {
            log.error("Failed to create tunnel interface on {}", node.ovsdbId());
            return;
        }

        TunnelDescription tunnelDesc = DefaultTunnelDescription.builder()
                .deviceId(INTEGRATION_BRIDGE)
                .ifaceName(DEFAULT_TUNNEL)
                .type(VXLAN)
                .remote(TunnelEndPoints.flowTunnelEndpoint())
                .key(TunnelKeys.flowTunnelKey())
                .build();

        InterfaceConfig ifaceConfig = device.as(InterfaceConfig.class);
        ifaceConfig.addTunnelMode(DEFAULT_TUNNEL, tunnelDesc);
    }

    private void createPatchInterface(OpenstackNode node) {
        checkArgument(node.type().equals(NodeType.GATEWAY));
        if (isIfaceCreated(node.ovsdbId(), PATCH_INTG_BRIDGE) &&
                isIfaceCreated(node.ovsdbId(), PATCH_ROUT_BRIDGE)) {
            return;
        }

        Device device = deviceService.getDevice(node.ovsdbId());
        if (device == null || !device.is(InterfaceConfig.class)) {
            log.error("Failed to create patch interfaces on {}", node.hostname());
            return;
        }

        PatchDescription patchIntg = DefaultPatchDescription.builder()
                .deviceId(INTEGRATION_BRIDGE)
                .ifaceName(PATCH_INTG_BRIDGE)
                .peer(PATCH_ROUT_BRIDGE)
                .build();

        PatchDescription patchRout = DefaultPatchDescription.builder()
                .deviceId(ROUTER_BRIDGE)
                .ifaceName(PATCH_ROUT_BRIDGE)
                .peer(PATCH_INTG_BRIDGE)
                .build();

        InterfaceConfig ifaceConfig = device.as(InterfaceConfig.class);
        ifaceConfig.addPatchMode(PATCH_INTG_BRIDGE, patchIntg);
        ifaceConfig.addPatchMode(PATCH_ROUT_BRIDGE, patchRout);
    }

    private void addUplink(OpenstackNode node) {
        checkArgument(node.type().equals(NodeType.GATEWAY));
        if (isIfaceCreated(node.ovsdbId(), node.uplink().get())) {
            return;
        }

        Device device = deviceService.getDevice(node.ovsdbId());
        if (device == null || !device.is(BridgeConfig.class)) {
            log.error("Failed to add port {} on {}", node.uplink().get(), node.ovsdbId());
            return;
        }

        BridgeConfig bridgeConfig =  device.as(BridgeConfig.class);
        bridgeConfig.addPort(BridgeName.bridgeName(ROUTER_BRIDGE),
                             node.uplink().get());
    }

    private boolean isOvsdbConnected(OpenstackNode node) {
        OvsdbNodeId ovsdb = new OvsdbNodeId(node.managementIp(), ovsdbPort);
        OvsdbClientService client = ovsdbController.getOvsdbClient(ovsdb);
        return deviceService.isAvailable(node.ovsdbId()) &&
                client != null &&
                client.isConnected();
    }

    private void connectOvsdb(OpenstackNode node) {
        ovsdbController.connect(node.managementIp(), TpPort.tpPort(ovsdbPort));
    }

    private Set<String> systemIfaces(OpenstackNode node) {
        Set<String> ifaces = Sets.newHashSet(DEFAULT_TUNNEL);
        if (node.type().equals(NodeType.GATEWAY)) {
            ifaces.add(PATCH_INTG_BRIDGE);
            ifaces.add(PATCH_ROUT_BRIDGE);
            ifaces.add(node.uplink().get());
        }
        return ifaces;
    }

    private OpenstackNode nodeByDeviceId(DeviceId deviceId) {
        OpenstackNode node = nodes().stream()
                .filter(n -> n.intBridge().equals(deviceId))
                .findFirst().orElseGet(() -> nodes().stream()
                .filter(n -> n.routerBridge().isPresent())
                .filter(n -> n.routerBridge().get().equals(deviceId))
                .findFirst().orElse(null));

        return node;
    }

    private class OvsdbHandler implements ConnectionHandler<Device> {

        @Override
        public void connected(Device device) {
            OpenstackNode node = nodes().stream()
                    .filter(n -> n.ovsdbId().equals(device.id()))
                    .findFirst()
                    .orElse(null);
            if (node != null) {
                setNodeState(node, nodeState(node));
            } else {
                log.debug("{} is detected on unregistered node, ignore it.", device.id());
            }
        }

        @Override
        public void disconnected(Device device) {
            OpenstackNode node = nodeByDeviceId(device.id());
            if (node != null) {
                log.warn("Device {} is disconnected", device.id());
                setNodeState(node, NodeState.INCOMPLETE);
            }
        }
    }

    private class BridgeHandler implements ConnectionHandler<Device> {

        @Override
        public void connected(Device device) {
            OpenstackNode node = nodeByDeviceId(device.id());
            if (node != null) {
                setNodeState(node, nodeState(node));
            } else {
                log.debug("{} is detected on unregistered node, ignore it.", device.id());
            }
        }

        @Override
        public void disconnected(Device device) {
            OpenstackNode node = nodeByDeviceId(device.id());
            if (node != null) {
                log.warn("Device {} is disconnected", device.id());
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
            OpenstackNode node = nodeByDeviceId((DeviceId) port.element().id());
            String portName = port.annotations().value(PORT_NAME);
            if (node == null) {
                log.debug("{} is added to unregistered node, ignore it.", portName);
                return;
            }

            log.info("Port {} is added to {}", portName, node.hostname());
            if (systemIfaces(node).contains(portName)) {
                setNodeState(node, nodeState(node));
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
            OpenstackNode node = nodeByDeviceId((DeviceId) port.element().id());
            String portName = port.annotations().value(PORT_NAME);

            if (node == null) {
                return;
            }

            log.info("Port {} is removed from {}", portName, node.hostname());
            if (systemIfaces(node).contains(portName)) {
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
                // TODO implement OVSDB port event so that we can handle updates on the OVSDB
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

    private void readConfiguration() {
        OpenstackNodeConfig config = configRegistry.getConfig(appId, CONFIG_CLASS);
        if (config == null) {
            log.debug("No configuration found");
            return;
        }

        Map<String, OpenstackNode> prevNodeMap = new HashMap(nodeStore.asJavaMap());
        config.openstackNodes().forEach(node -> {
            prevNodeMap.remove(node.hostname());
            addOrUpdateNode(node);
        });
        prevNodeMap.values().stream().forEach(this::deleteNode);
    }

    private class InternalConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            NodeId leaderNodeId = leadershipService.getLeader(appId.name());
            if (!Objects.equals(localNodeId, leaderNodeId)) {
                // do not allow to proceed without leadership
                return;
            }

            if (!event.configClass().equals(CONFIG_CLASS)) {
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

    private class InternalMapListener implements MapEventListener<String, OpenstackNode> {

        @Override
        public void event(MapEvent<String, OpenstackNode> event) {
            NodeId leaderNodeId = leadershipService.getLeader(appId.name());
            if (!Objects.equals(localNodeId, leaderNodeId)) {
                // do not allow to proceed without leadership
                return;
            }

            OpenstackNode oldNode;
            OpenstackNode newNode;

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
