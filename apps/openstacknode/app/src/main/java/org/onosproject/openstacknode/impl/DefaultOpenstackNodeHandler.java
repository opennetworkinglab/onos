/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.openstacknode.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.IpAddress;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.behaviour.BridgeConfig;
import org.onosproject.net.behaviour.BridgeDescription;
import org.onosproject.net.behaviour.BridgeName;
import org.onosproject.net.behaviour.ControllerInfo;
import org.onosproject.net.behaviour.DefaultBridgeDescription;
import org.onosproject.net.behaviour.DefaultPatchDescription;
import org.onosproject.net.behaviour.DefaultTunnelDescription;
import org.onosproject.net.behaviour.ExtensionTreatmentResolver;
import org.onosproject.net.behaviour.InterfaceConfig;
import org.onosproject.net.behaviour.PatchDescription;
import org.onosproject.net.behaviour.TunnelDescription;
import org.onosproject.net.behaviour.TunnelEndPoints;
import org.onosproject.net.behaviour.TunnelKeys;
import org.onosproject.net.device.DeviceAdminService;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.ExtensionPropertyException;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.group.DefaultGroupDescription;
import org.onosproject.net.group.Group;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupEvent;
import org.onosproject.net.group.GroupListener;
import org.onosproject.net.group.GroupService;
import org.onosproject.openstacknode.api.NodeState;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNode.NetworkMode;
import org.onosproject.openstacknode.api.OpenstackNodeAdminService;
import org.onosproject.openstacknode.api.OpenstackNodeEvent;
import org.onosproject.openstacknode.api.OpenstackNodeHandler;
import org.onosproject.openstacknode.api.OpenstackNodeListener;
import org.onosproject.openstacknode.api.OpenstackNodeService;
import org.onosproject.ovsdb.controller.OvsdbClientService;
import org.onosproject.ovsdb.controller.OvsdbController;
import org.onosproject.ovsdb.controller.OvsdbNodeId;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.util.Dictionary;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.packet.TpPort.tpPort;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.AnnotationKeys.PORT_NAME;
import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_TUNNEL_DST;
import static org.onosproject.net.group.DefaultGroupBucket.createSelectGroupBucket;
import static org.onosproject.openstacknode.api.Constants.*;
import static org.onosproject.openstacknode.api.Constants.PATCH_INTG_BRIDGE;
import static org.onosproject.openstacknode.api.NodeState.*;
import static org.onosproject.openstacknode.api.OpenstackNode.NetworkMode.VLAN;
import static org.onosproject.openstacknode.api.OpenstackNode.NetworkMode.VXLAN;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.COMPUTE;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.GATEWAY;
import static org.onosproject.openstacknode.api.OpenstackNodeService.APP_ID;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Service bootstraps openstack node based on its type.
 */
@Component(immediate = true)
public class DefaultOpenstackNodeHandler implements OpenstackNodeHandler {

    protected final Logger log = getLogger(getClass());

    private static final String OVSDB_PORT = "ovsdbPortNum";
    private static final int DEFAULT_OVSDB_PORT = 6640;
    private static final String DEFAULT_OF_PROTO = "tcp";
    private static final int DEFAULT_OFPORT = 6653;
    private static final int DPID_BEGIN = 3;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceAdminService deviceAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OvsdbController ovsdbController;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected GroupService groupService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackNodeService osNodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackNodeAdminService osNodeAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService componentConfigService;

    @Property(name = OVSDB_PORT, intValue = DEFAULT_OVSDB_PORT,
            label = "OVSDB server listen port")
    private int ovsdbPort = DEFAULT_OVSDB_PORT;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

    private final DeviceListener ovsdbListener = new InternalOvsdbListener();
    private final DeviceListener bridgeListener = new InternalBridgeListener();
    private final GroupListener groupListener = new InternalGroupListener();
    private final OpenstackNodeListener osNodeListener = new InternalOpenstackNodeListener();

    private ApplicationId appId;
    private NodeId localNode;

    @Activate
    protected void activate() {
        appId = coreService.getAppId(APP_ID);
        localNode = clusterService.getLocalNode().id();

        componentConfigService.registerProperties(getClass());
        leadershipService.runForLeadership(appId.name());
        groupService.addListener(groupListener);
        deviceService.addListener(ovsdbListener);
        deviceService.addListener(bridgeListener);
        osNodeService.addListener(osNodeListener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        osNodeService.removeListener(osNodeListener);
        deviceService.removeListener(bridgeListener);
        deviceService.removeListener(ovsdbListener);
        groupService.removeListener(groupListener);
        componentConfigService.unregisterProperties(getClass(), false);
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
    public void processInitState(OpenstackNode osNode) {
        if (!isOvsdbConnected(osNode)) {
            ovsdbController.connect(osNode.managementIp(), tpPort(ovsdbPort));
            return;
        }
        if (!deviceService.isAvailable(osNode.intgBridge())) {
            createBridge(osNode, INTEGRATION_BRIDGE, osNode.intgBridge());
        }
        if (osNode.type() == GATEWAY &&
                !isBridgeCreated(osNode.ovsdb(), ROUTER_BRIDGE)) {
            createBridge(osNode, ROUTER_BRIDGE, osNode.routerBridge());
        }
    }

    @Override
    public void processDeviceCreatedState(OpenstackNode osNode) {
        if (!isOvsdbConnected(osNode)) {
            ovsdbController.connect(osNode.managementIp(), tpPort(ovsdbPort));
            return;
        }
        if (osNode.type() == GATEWAY && (
                !isIntfEnabled(osNode, PATCH_INTG_BRIDGE) ||
                        !isIntfCreated(osNode, PATCH_ROUT_BRIDGE)
        )) {
            createPatchInterface(osNode);
        }
        if (osNode.dataIp() != null &&
                !isIntfEnabled(osNode, DEFAULT_TUNNEL)) {
            createTunnelInterface(osNode);
        }
        if (osNode.vlanIntf() != null &&
                !isIntfEnabled(osNode, osNode.vlanIntf())) {
            addSystemInterface(osNode, INTEGRATION_BRIDGE, osNode.vlanIntf());
        }
    }

    @Override
    public void processPortCreatedState(OpenstackNode osNode) {
        switch (osNode.type()) {
            case COMPUTE:
                if (osNode.dataIp() != null) {
                    addOrUpdateGatewayGroup(osNode,
                            osNodeService.completeNodes(GATEWAY),
                            VXLAN);
                }
                if (osNode.vlanIntf() != null) {
                    addOrUpdateGatewayGroup(osNode,
                            osNodeService.completeNodes(GATEWAY),
                            VLAN);
                }
                break;
            case GATEWAY:
                Set<OpenstackNode> gateways =
                        Sets.newHashSet(osNodeService.completeNodes(GATEWAY));
                gateways.add(osNode);
                osNodeService.completeNodes(COMPUTE).forEach(n -> {
                    if (n.dataIp() != null) {
                        addOrUpdateGatewayGroup(n, gateways, VXLAN);
                    }
                    if (n.vlanIntf() != null) {
                        addOrUpdateGatewayGroup(n, gateways, VLAN);
                    }
                });
                break;
            default:
                break;
        }
    }

    @Override
    public void processCompleteState(OpenstackNode osNode) {
        OvsdbClientService ovsdbClient = ovsdbController.getOvsdbClient(
                new OvsdbNodeId(osNode.managementIp(), DEFAULT_OVSDB_PORT));
        if (ovsdbClient != null && ovsdbClient.isConnected()) {
            ovsdbClient.disconnect();
        }
    }

    @Override
    public void processIncompleteState(OpenstackNode osNode) {
        if (osNode.type() == COMPUTE) {
            if (osNode.dataIp() != null) {
                groupService.removeGroup(osNode.intgBridge(), osNode.gatewayGroupKey(VXLAN), appId);
            }
            if (osNode.vlanIntf() != null) {
                groupService.removeGroup(osNode.intgBridge(), osNode.gatewayGroupKey(VLAN), appId);
            }
        }
        if (osNode.type() == GATEWAY) {
            osNodeService.completeNodes(COMPUTE).forEach(n -> {
                if (n.dataIp() != null) {
                    addOrUpdateGatewayGroup(n,
                            osNodeService.completeNodes(GATEWAY),
                            VXLAN);
                }
                if (n.vlanIntf() != null) {
                    addOrUpdateGatewayGroup(n,
                            osNodeService.completeNodes(GATEWAY),
                            VLAN);
                }
            });
        }
    }

    private boolean isOvsdbConnected(OpenstackNode osNode) {
        OvsdbNodeId ovsdb = new OvsdbNodeId(osNode.managementIp(), ovsdbPort);
        OvsdbClientService client = ovsdbController.getOvsdbClient(ovsdb);
        return deviceService.isAvailable(osNode.ovsdb()) &&
                client != null &&
                client.isConnected();
    }

    private void createBridge(OpenstackNode osNode, String bridgeName, DeviceId deviceId) {
        Device device = deviceService.getDevice(osNode.ovsdb());
        if (device == null || !device.is(BridgeConfig.class)) {
            log.error("Failed to create integration bridge on {}", osNode.ovsdb());
            return;
        }

        // TODO fix this when we use single ONOS cluster for both openstackNode and vRouter
        Set<IpAddress> controllerIps;
        if (bridgeName.equals(ROUTER_BRIDGE)) {
            // TODO checks if empty controller does not break anything
            controllerIps = ImmutableSet.of();
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

    private void addSystemInterface(OpenstackNode osNode, String bridgeName, String intfName) {
        Device device = deviceService.getDevice(osNode.ovsdb());
        if (device == null || !device.is(BridgeConfig.class)) {
            return;
        }
        BridgeConfig bridgeConfig =  device.as(BridgeConfig.class);
        bridgeConfig.addPort(BridgeName.bridgeName(bridgeName), intfName);
    }

    private void createTunnelInterface(OpenstackNode osNode) {
        if (isIntfEnabled(osNode, DEFAULT_TUNNEL)) {
            return;
        }

        Device device = deviceService.getDevice(osNode.ovsdb());
        if (device == null || !device.is(InterfaceConfig.class)) {
            log.error("Failed to create tunnel interface on {}", osNode.ovsdb());
            return;
        }

        TunnelDescription tunnelDesc = DefaultTunnelDescription.builder()
                .deviceId(INTEGRATION_BRIDGE)
                .ifaceName(DEFAULT_TUNNEL)
                .type(TunnelDescription.Type.VXLAN)
                .remote(TunnelEndPoints.flowTunnelEndpoint())
                .key(TunnelKeys.flowTunnelKey())
                .build();

        InterfaceConfig ifaceConfig = device.as(InterfaceConfig.class);
        ifaceConfig.addTunnelMode(DEFAULT_TUNNEL, tunnelDesc);
    }

    private void createPatchInterface(OpenstackNode osNode) {
        checkArgument(osNode.type().equals(OpenstackNode.NodeType.GATEWAY));
        if (isIntfEnabled(osNode, PATCH_INTG_BRIDGE) &&
                isIntfCreated(osNode, PATCH_ROUT_BRIDGE)) {
            return;
        }

        Device device = deviceService.getDevice(osNode.ovsdb());
        if (device == null || !device.is(InterfaceConfig.class)) {
            log.error("Failed to create patch interfaces on {}", osNode.hostname());
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

    private void addOrUpdateGatewayGroup(OpenstackNode osNode,
                                         Set<OpenstackNode> gatewayNodes,
                                         NetworkMode mode) {
        GroupBuckets buckets = gatewayGroupBuckets(osNode, gatewayNodes, mode);
        if (groupService.getGroup(osNode.intgBridge(), osNode.gatewayGroupKey(mode)) == null) {
            GroupDescription groupDescription = new DefaultGroupDescription(
                    osNode.intgBridge(),
                    GroupDescription.Type.SELECT,
                    buckets,
                    osNode.gatewayGroupKey(mode),
                    osNode.gatewayGroupId(mode).id(),
                    appId);
            groupService.addGroup(groupDescription);
            log.debug("Created gateway group for {}", osNode.hostname());
        } else {
            groupService.setBucketsForGroup(
                    osNode.intgBridge(),
                    osNode.gatewayGroupKey(mode),
                    buckets,
                    osNode.gatewayGroupKey(mode),
                    appId);
            log.debug("Updated gateway group for {}", osNode.hostname());
        }
    }

    private GroupBuckets gatewayGroupBuckets(OpenstackNode osNode,
                                             Set<OpenstackNode> gatewayNodes,
                                             NetworkMode mode) {
        List<GroupBucket> bucketList = Lists.newArrayList();
        switch (mode) {
            case VXLAN:
                gatewayNodes.stream().filter(n -> n.dataIp() != null).forEach(n -> {
                    TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                            .extension(tunnelDstTreatment(osNode.intgBridge(),
                                    n.dataIp()),
                                    osNode.intgBridge())
                            .setOutput(osNode.tunnelPortNum())
                            .build();
                    bucketList.add(createSelectGroupBucket(treatment));
                });
                return new GroupBuckets(bucketList);
            case VLAN:
                gatewayNodes.stream().filter(n -> n.vlanIntf() != null).forEach(n -> {
                    TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                            .setEthDst(n.vlanPortMac())
                            .setOutput(osNode.vlanPortNum())
                            .build();
                    bucketList.add(createSelectGroupBucket(treatment));
                });
                return new GroupBuckets(bucketList);
            default:
                return null;
        }
    }

    private ExtensionTreatment tunnelDstTreatment(DeviceId deviceId, IpAddress remoteIp) {
        Device device = deviceService.getDevice(deviceId);
        if (device != null && !device.is(ExtensionTreatmentResolver.class)) {
            log.error("The extension treatment is not supported");
            return null;
        }

        ExtensionTreatmentResolver resolver = device.as(ExtensionTreatmentResolver.class);
        ExtensionTreatment treatment = resolver.getExtensionInstruction(NICIRA_SET_TUNNEL_DST.type());
        try {
            treatment.setPropertyValue("tunnelDst", remoteIp.getIp4Address());
            return treatment;
        } catch (ExtensionPropertyException e) {
            log.warn("Failed to get tunnelDst extension treatment for {}", deviceId);
            return null;
        }
    }

    private boolean isBridgeCreated(DeviceId ovsdbId, String bridgeName) {
        Device device = deviceService.getDevice(ovsdbId);
        if (device == null || !deviceService.isAvailable(device.id()) ||
                !device.is(BridgeConfig.class)) {
            return false;
        }
        BridgeConfig bridgeConfig = device.as(BridgeConfig.class);
        return bridgeConfig.getBridges().stream()
                .anyMatch(bridge -> bridge.name().equals(bridgeName));
    }

    private boolean isIntfEnabled(OpenstackNode osNode, String intf) {
        if (!deviceService.isAvailable(osNode.intgBridge())) {
            return false;
        }
        return deviceService.getPorts(osNode.intgBridge()).stream()
                .anyMatch(port -> Objects.equals(
                        port.annotations().value(PORT_NAME), intf) &&
                        port.isEnabled());
    }

    private boolean isIntfCreated(OpenstackNode osNode, String intf) {
        Device device = deviceService.getDevice(osNode.ovsdb());
        if (device == null || !deviceService.isAvailable(osNode.ovsdb()) ||
                !device.is(BridgeConfig.class)) {
            return false;
        }

        BridgeConfig bridgeConfig =  device.as(BridgeConfig.class);
        return bridgeConfig.getPorts().stream()
                .anyMatch(port -> port.annotations().value(PORT_NAME).equals(intf));
    }

    private boolean isGroupCreated(OpenstackNode osNode) {
        for (OpenstackNode gNode : osNodeService.completeNodes(GATEWAY)) {
            if (!isGatewayBucketAdded(osNode, gNode)) {
                return false;
            }
        }
        return true;
    }

    private boolean isGatewayBucketAdded(OpenstackNode cNode, OpenstackNode gNode) {
        if (cNode.dataIp() != null) {
            Group osGroup = groupService.getGroup(cNode.intgBridge(),
                    cNode.gatewayGroupKey(VXLAN));
            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .extension(tunnelDstTreatment(gNode.intgBridge(),
                            gNode.dataIp()),
                            cNode.intgBridge())
                    .setOutput(cNode.tunnelPortNum())
                    .build();
            GroupBucket bucket = createSelectGroupBucket(treatment);
            if (osGroup == null || !osGroup.buckets().buckets().contains(bucket)) {
                return false;
            }
        }
        if (cNode.vlanIntf() != null) {
            Group osGroup = groupService.getGroup(cNode.intgBridge(),
                    cNode.gatewayGroupKey(VLAN));
            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setEthDst(gNode.vlanPortMac())
                    .setOutput(cNode.vlanPortNum())
                    .build();
            GroupBucket bucket = createSelectGroupBucket(treatment);
            if (osGroup == null || !osGroup.buckets().buckets().contains(bucket)) {
                return false;
            }
        }
        return true;
    }

    private boolean isCurrentStateDone(OpenstackNode osNode) {
        switch (osNode.state()) {
            case INIT:
                if (!deviceService.isAvailable(osNode.intgBridge())) {
                    return false;
                }
                if (osNode.type() == GATEWAY &&
                        !isBridgeCreated(osNode.ovsdb(), ROUTER_BRIDGE)) {
                    return false;
                }
                return true;
            case DEVICE_CREATED:
                if (osNode.dataIp() != null &&
                        !isIntfEnabled(osNode, DEFAULT_TUNNEL)) {
                    return false;
                }
                if (osNode.vlanIntf() != null &&
                        !isIntfEnabled(osNode, osNode.vlanIntf())) {
                    return false;
                }
                if (osNode.type() == GATEWAY && (
                        !isIntfEnabled(osNode, PATCH_INTG_BRIDGE) ||
                        !isIntfCreated(osNode, PATCH_ROUT_BRIDGE))) {
                    return false;
                }
                return true;
            case PORT_CREATED:
                if (osNode.type() == COMPUTE) {
                    return isGroupCreated(osNode);
                } else {
                    for (OpenstackNode cNode : osNodeService.completeNodes(COMPUTE)) {
                        if (!isGatewayBucketAdded(cNode, osNode)) {
                            return false;
                        }
                    }
                    return true;
                }
            case COMPLETE:
                return false;
            case INCOMPLETE:
                // always return false
                // run init CLI to re-trigger node bootstrap
                return false;
            default:
                return true;
        }
    }

    private void setState(OpenstackNode osNode, NodeState newState) {
        if (osNode.state() == newState) {
            return;
        }
        OpenstackNode updated = osNode.updateState(newState);
        osNodeAdminService.updateNode(updated);
        log.info("Changed {} state: {}", osNode.hostname(), newState);
    }

    private void bootstrapNode(OpenstackNode osNode) {
        if (isCurrentStateDone(osNode)) {
            setState(osNode, osNode.state().nextState());
        } else {
            log.trace("Processing {} state for {}", osNode.state(), osNode.hostname());
            osNode.state().process(this, osNode);
        }
    }

    private class InternalOvsdbListener implements DeviceListener {

        @Override
        public boolean isRelevant(DeviceEvent event) {
            NodeId leader = leadershipService.getLeader(appId.name());
            return Objects.equals(localNode, leader) &&
                    event.subject().type() == Device.Type.CONTROLLER &&
                    osNodeService.node(event.subject().id()) != null;
        }

        @Override
        public void event(DeviceEvent event) {
            Device device = event.subject();
            OpenstackNode osNode = osNodeService.node(device.id());

            switch (event.type()) {
                case DEVICE_AVAILABILITY_CHANGED:
                case DEVICE_ADDED:
                    eventExecutor.execute(() -> {
                        if (deviceService.isAvailable(device.id())) {
                            log.debug("OVSDB {} detected", device.id());
                            bootstrapNode(osNode);
                        } else if (osNode.state() == COMPLETE) {
                            log.debug("Removing OVSDB {}", device.id());
                            deviceAdminService.removeDevice(device.id());
                        }
                    });
                    break;
                case PORT_ADDED:
                case PORT_REMOVED:
                case DEVICE_REMOVED:
                default:
                    // do nothing
                    break;
            }
        }
    }

    private class InternalBridgeListener implements DeviceListener {

        @Override
        public boolean isRelevant(DeviceEvent event) {
            NodeId leader = leadershipService.getLeader(appId.name());
            return Objects.equals(localNode, leader) &&
                    event.subject().type() == Device.Type.SWITCH &&
                    osNodeService.node(event.subject().id()) != null;
        }

        @Override
        public void event(DeviceEvent event) {
            Device device = event.subject();
            OpenstackNode osNode = osNodeService.node(device.id());

            switch (event.type()) {
                case DEVICE_AVAILABILITY_CHANGED:
                case DEVICE_ADDED:
                    eventExecutor.execute(() -> {
                        if (deviceService.isAvailable(device.id())) {
                            log.debug("Integration bridge created on {}", osNode.hostname());
                            bootstrapNode(osNode);
                        } else if (osNode.state() == COMPLETE) {
                            log.warn("Device {} disconnected", device.id());
                            setState(osNode, INCOMPLETE);
                        }
                    });
                    break;
                case PORT_ADDED:
                    eventExecutor.execute(() -> {
                        Port port = event.port();
                        String portName = port.annotations().value(PORT_NAME);
                        if (osNode.state() == DEVICE_CREATED && (
                                Objects.equals(portName, DEFAULT_TUNNEL) ||
                                Objects.equals(portName, osNode.vlanIntf()) ||
                                Objects.equals(portName, PATCH_INTG_BRIDGE) ||
                                Objects.equals(portName, PATCH_ROUT_BRIDGE))) {
                            // FIXME we never gets PATCH_ROUTE_BRIDGE port added events as of now
                            log.debug("Interface {} added to {}", portName, event.subject().id());
                            bootstrapNode(osNode);
                        }
                    });
                    break;
                case PORT_REMOVED:
                    eventExecutor.execute(() -> {
                        Port port = event.port();
                        String portName = port.annotations().value(PORT_NAME);
                        if (osNode.state() == COMPLETE && (
                                Objects.equals(portName, DEFAULT_TUNNEL) ||
                                Objects.equals(portName, osNode.vlanIntf()) ||
                                Objects.equals(portName, PATCH_INTG_BRIDGE) ||
                                Objects.equals(portName, PATCH_ROUT_BRIDGE))) {
                            log.warn("Interface {} removed from {}", portName, event.subject().id());
                            setState(osNode, INCOMPLETE);
                        }
                    });
                    break;
                case PORT_UPDATED:
                case DEVICE_REMOVED:
                default:
                    // do nothing
                    break;
            }
        }
    }

    private class InternalGroupListener implements GroupListener {

        @Override
        public boolean isRelevant(GroupEvent event) {
            NodeId leader = leadershipService.getLeader(appId.name());
            return Objects.equals(localNode, leader);
        }

        @Override
        public void event(GroupEvent event) {
            switch (event.type()) {
                case GROUP_ADDED:
                    eventExecutor.execute(() -> {
                        log.trace("Group added, ID:{} state:{}", event.subject().id(),
                                event.subject().state());
                        processGroup(event.subject());
                    });
                    break;
                case GROUP_UPDATED:
                    eventExecutor.execute(() -> {
                        log.trace("Group updated, ID:{} state:{}", event.subject().id(),
                                event.subject().state());
                        processGroup(event.subject());
                    });
                    break;
                case GROUP_REMOVED:
                    // TODO handle group removed
                    break;
                default:
                    break;
            }
        }

        private void processGroup(Group group) {
            OpenstackNode osNode = osNodeService.nodes(COMPUTE).stream()
                    .filter(n -> n.state() == PORT_CREATED &&
                            (n.gatewayGroupId(VXLAN).equals(group.id()) ||
                            n.gatewayGroupId(VLAN).equals(group.id())))
                    .findAny().orElse(null);
            if (osNode != null) {
                bootstrapNode(osNode);
            }
            osNodeService.nodes(GATEWAY).stream()
                    .filter(gNode -> gNode.state() == PORT_CREATED)
                    .forEach(DefaultOpenstackNodeHandler.this::bootstrapNode);
        }
    }

    private class InternalOpenstackNodeListener implements OpenstackNodeListener {

        @Override
        public boolean isRelevant(OpenstackNodeEvent event) {
            NodeId leader = leadershipService.getLeader(appId.name());
            return Objects.equals(localNode, leader);
        }

        @Override
        public void event(OpenstackNodeEvent event) {
            switch (event.type()) {
                case OPENSTACK_NODE_CREATED:
                case OPENSTACK_NODE_UPDATED:
                    eventExecutor.execute(() -> {
                        bootstrapNode(event.subject());
                    });
                    break;
                case OPENSTACK_NODE_COMPLETE:
                    break;
                case OPENSTACK_NODE_REMOVED:
                    break;
                default:
                    break;
            }
        }
    }
}
