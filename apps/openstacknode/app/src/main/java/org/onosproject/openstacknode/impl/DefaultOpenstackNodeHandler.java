/*
 * Copyright 2017-present Open Networking Foundation
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

import com.google.common.collect.Lists;
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
import org.onosproject.net.behaviour.InterfaceConfig;
import org.onosproject.net.behaviour.PatchDescription;
import org.onosproject.net.behaviour.TunnelDescription;
import org.onosproject.net.behaviour.TunnelEndPoints;
import org.onosproject.net.behaviour.TunnelKeys;
import org.onosproject.net.device.DeviceAdminService;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.openstacknode.api.DpdkInterface;
import org.onosproject.openstacknode.api.NodeState;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeAdminService;
import org.onosproject.openstacknode.api.OpenstackNodeEvent;
import org.onosproject.openstacknode.api.OpenstackNodeHandler;
import org.onosproject.openstacknode.api.OpenstackNodeListener;
import org.onosproject.openstacknode.api.OpenstackNodeService;
import org.onosproject.openstacknode.api.OpenstackPhyInterface;
import org.onosproject.ovsdb.controller.OvsdbClientService;
import org.onosproject.ovsdb.controller.OvsdbController;
import org.onosproject.ovsdb.controller.OvsdbPort;
import org.onosproject.ovsdb.rfc.notation.OvsdbMap;
import org.onosproject.ovsdb.rfc.notation.OvsdbSet;
import org.onosproject.ovsdb.rfc.table.Interface;
import org.openstack4j.api.OSClient;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Dictionary;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.packet.TpPort.tpPort;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.AnnotationKeys.PORT_NAME;
import static org.onosproject.openstacknode.api.Constants.BRIDGE_PREFIX;
import static org.onosproject.openstacknode.api.Constants.GENEVE;
import static org.onosproject.openstacknode.api.Constants.GENEVE_TUNNEL;
import static org.onosproject.openstacknode.api.Constants.GRE;
import static org.onosproject.openstacknode.api.Constants.GRE_TUNNEL;
import static org.onosproject.openstacknode.api.Constants.INTEGRATION_BRIDGE;
import static org.onosproject.openstacknode.api.Constants.INTEGRATION_TO_PHYSICAL_PREFIX;
import static org.onosproject.openstacknode.api.Constants.PHYSICAL_TO_INTEGRATION_SUFFIX;
import static org.onosproject.openstacknode.api.Constants.TUNNEL_BRIDGE;
import static org.onosproject.openstacknode.api.Constants.VXLAN;
import static org.onosproject.openstacknode.api.Constants.VXLAN_TUNNEL;
import static org.onosproject.openstacknode.api.DpdkConfig.DatapathType.NETDEV;
import static org.onosproject.openstacknode.api.NodeState.COMPLETE;
import static org.onosproject.openstacknode.api.NodeState.DEVICE_CREATED;
import static org.onosproject.openstacknode.api.NodeState.INCOMPLETE;
import static org.onosproject.openstacknode.api.NodeState.INIT;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.CONTROLLER;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.GATEWAY;
import static org.onosproject.openstacknode.api.OpenstackNodeService.APP_ID;
import static org.onosproject.openstacknode.impl.OsgiPropertyConstants.AUTO_RECOVERY;
import static org.onosproject.openstacknode.impl.OsgiPropertyConstants.AUTO_RECOVERY_DEFAULT;
import static org.onosproject.openstacknode.impl.OsgiPropertyConstants.OVSDB_PORT;
import static org.onosproject.openstacknode.impl.OsgiPropertyConstants.OVSDB_PORT_NUM_DEFAULT;
import static org.onosproject.openstacknode.util.OpenstackNodeUtil.addOrRemoveDpdkInterface;
import static org.onosproject.openstacknode.util.OpenstackNodeUtil.addOrRemoveSystemInterface;
import static org.onosproject.openstacknode.util.OpenstackNodeUtil.getBooleanProperty;
import static org.onosproject.openstacknode.util.OpenstackNodeUtil.getConnectedClient;
import static org.onosproject.openstacknode.util.OpenstackNodeUtil.getOvsdbClient;
import static org.onosproject.openstacknode.util.OpenstackNodeUtil.isOvsdbConnected;
import static org.onosproject.openstacknode.util.OpenstackNodeUtil.structurePortName;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Service bootstraps openstack node based on its type.
 */
@Component(immediate = true,
    property = {
        OVSDB_PORT + ":Integer=" + OVSDB_PORT_NUM_DEFAULT,
        AUTO_RECOVERY + ":Boolean=" + AUTO_RECOVERY_DEFAULT
    }
)
public class DefaultOpenstackNodeHandler implements OpenstackNodeHandler {

    private final Logger log = getLogger(getClass());

    private static final String DEFAULT_OF_PROTO = "tcp";
    private static final String NO_OVSDB_CLIENT_MSG = "Failed to get ovsdb client";
    private static final int DEFAULT_OFPORT = 6653;
    private static final int DPID_BEGIN = 3;
    private static final int NETWORK_BEGIN = 3;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceAdminService deviceAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OvsdbController ovsdbController;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackNodeService osNodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackNodeAdminService osNodeAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService componentConfigService;

    /** OVSDB server listen port. */
    private int ovsdbPortNum = OVSDB_PORT_NUM_DEFAULT;

    /** Indicates whether auto-recover openstack node status on switch re-conn event. */
    private boolean autoRecovery = AUTO_RECOVERY_DEFAULT;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

    private final DeviceListener ovsdbListener = new InternalOvsdbListener();
    private final DeviceListener bridgeListener = new InternalBridgeListener();
    private final OpenstackNodeListener osNodeListener = new InternalOpenstackNodeListener();

    private ApplicationId appId;
    private NodeId localNode;

    @Activate
    protected void activate() {
        appId = coreService.getAppId(APP_ID);
        localNode = clusterService.getLocalNode().id();

        componentConfigService.registerProperties(getClass());
        leadershipService.runForLeadership(appId.name());
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
        componentConfigService.unregisterProperties(getClass(), false);
        leadershipService.withdraw(appId.name());
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    @Modified
    protected void modified(ComponentContext context) {
        readComponentConfiguration(context);

        log.info("Modified");
    }

    @Override
    public void processInitState(OpenstackNode osNode) {
        if (!isOvsdbConnected(osNode, ovsdbPortNum, ovsdbController, deviceService)) {
            ovsdbController.connect(osNode.managementIp(), tpPort(ovsdbPortNum));
            return;
        }

        if (!deviceService.isAvailable(osNode.intgBridge())) {
            createBridge(osNode, INTEGRATION_BRIDGE, osNode.intgBridge());
        }

        if (hasDpdkTunnelBridge(osNode)) {
            createDpdkTunnelBridge(osNode);
        }
    }

    @Override
    public void processDeviceCreatedState(OpenstackNode osNode) {
        try {
            if (!isOvsdbConnected(osNode, ovsdbPortNum, ovsdbController, deviceService)) {
                ovsdbController.connect(osNode.managementIp(), tpPort(ovsdbPortNum));
                return;
            }

            if (osNode.type() == GATEWAY) {
                addOrRemoveSystemInterface(osNode, INTEGRATION_BRIDGE,
                                        osNode.uplinkPort(), deviceService, true);
            }

            if (osNode.dataIp() != null &&
                    !isIntfEnabled(osNode, VXLAN_TUNNEL)) {
                createVxlanTunnelInterface(osNode);
            }

            if (osNode.dataIp() != null &&
                    !isIntfEnabled(osNode, GRE_TUNNEL)) {
                createGreTunnelInterface(osNode);
            }

            if (osNode.dataIp() != null &&
                    !isIntfEnabled(osNode, GENEVE_TUNNEL)) {
                createGeneveTunnelInterface(osNode);
            }

            if (osNode.dpdkConfig() != null && osNode.dpdkConfig().dpdkIntfs() != null) {
                osNode.dpdkConfig().dpdkIntfs().stream()
                        .filter(dpdkintf -> dpdkintf.deviceName().equals(TUNNEL_BRIDGE))
                        .forEach(dpdkintf -> addOrRemoveDpdkInterface(
                                osNode, dpdkintf, ovsdbPortNum, ovsdbController, true));

                osNode.dpdkConfig().dpdkIntfs().stream()
                        .filter(dpdkintf -> dpdkintf.deviceName().equals(INTEGRATION_BRIDGE))
                        .forEach(dpdkintf -> addOrRemoveDpdkInterface(
                                osNode, dpdkintf, ovsdbPortNum, ovsdbController, true));
            }

            // provision new physical interfaces on the given node
            // this includes creating physical bridge, attaching physical port
            // to physical bridge, adding patch ports to both physical bridge and br-int

            provisionPhysicalInterfaces(osNode);

            if (osNode.vlanIntf() != null &&
                    !isIntfEnabled(osNode, osNode.vlanIntf())) {
                addOrRemoveSystemInterface(osNode, INTEGRATION_BRIDGE,
                            osNode.vlanIntf(), deviceService, true);
            }
        } catch (Exception e) {
            log.error("Exception occurred because of {}", e);
        }
    }

    @Override
    public void processCompleteState(OpenstackNode osNode) {
        //Do something if needed
    }

    @Override
    public void processIncompleteState(OpenstackNode osNode) {
        //Do nothing for now
    }

    private boolean hasDpdkTunnelBridge(OpenstackNode osNode) {
        if (osNode.dpdkConfig() != null && osNode.dpdkConfig().dpdkIntfs() != null) {
            return osNode.dpdkConfig().dpdkIntfs().stream()
                    .anyMatch(intf -> intf.deviceName().equals(TUNNEL_BRIDGE));
        }
        return false;
    }

    private boolean dpdkTunnelBridgeCreated(OpenstackNode osNode) {

        OvsdbClientService client = getOvsdbClient(osNode, ovsdbPortNum, ovsdbController);
        if (client == null) {
            log.info(NO_OVSDB_CLIENT_MSG);
            return false;
        }

        return client.getBridges().stream()
                .anyMatch(bridge -> bridge.name().equals(TUNNEL_BRIDGE));
    }

    /**
     * Creates a bridge with a given name on a given openstack node.
     *
     * @param osNode openstack node
     * @param bridgeName bridge name
     * @param deviceId device identifier
     */
    private void createBridge(OpenstackNode osNode, String bridgeName, DeviceId deviceId) {
        Device device = deviceService.getDevice(osNode.ovsdb());

        List<ControllerInfo> controllers;

        if (osNode.controllers() != null && osNode.controllers().size() > 0) {
            controllers = (List<ControllerInfo>) osNode.controllers();
        } else {
            Set<IpAddress> controllerIps = clusterService.getNodes().stream()
                    .map(ControllerNode::ip)
                    .collect(Collectors.toSet());

            controllers = controllerIps.stream()
                    .map(ip -> new ControllerInfo(ip, DEFAULT_OFPORT, DEFAULT_OF_PROTO))
                    .collect(Collectors.toList());
        }

        String dpid = deviceId.toString().substring(DPID_BEGIN);

        BridgeDescription.Builder builder = DefaultBridgeDescription.builder()
                .name(bridgeName)
                .failMode(BridgeDescription.FailMode.SECURE)
                .datapathId(dpid)
                .disableInBand()
                .mcastSnoopingEnable()
                .controllers(controllers);

        if (osNode.datapathType().equals(NETDEV)) {
            builder.datapathType(NETDEV.name().toLowerCase());
        }

        BridgeConfig bridgeConfig = device.as(BridgeConfig.class);
        bridgeConfig.addBridge(builder.build());
    }

    private void createDpdkTunnelBridge(OpenstackNode osNode) {
        Device device = deviceService.getDevice(osNode.ovsdb());

        BridgeDescription.Builder builder = DefaultBridgeDescription.builder()
                .name(TUNNEL_BRIDGE)
                .datapathType(NETDEV.name().toLowerCase());

        BridgeConfig bridgeConfig = device.as(BridgeConfig.class);
        bridgeConfig.addBridge(builder.build());
    }

    private void provisionPhysicalInterfaces(OpenstackNode osNode) {
        osNode.phyIntfs().forEach(pi -> {
            String bridgeName = BRIDGE_PREFIX + pi.network();
            String patchPortName =
                    structurePortName(INTEGRATION_TO_PHYSICAL_PREFIX + pi.network());

            if (!hasPhyBridge(osNode, bridgeName)) {
                createPhysicalBridge(osNode, pi);
                createPhysicalPatchPorts(osNode, pi);
                attachPhysicalPort(osNode, pi);
            } else {
                // in case physical bridge exists, but patch port is missing on br-int,
                // we will add patch port to connect br-int with physical bridge
                if (!hasPhyPatchPort(osNode, patchPortName)) {
                    createPhysicalPatchPorts(osNode, pi);
                }
            }
        });
    }

    private void cleanPhysicalInterfaces(OpenstackNode osNode) {
        Device device = deviceService.getDevice(osNode.ovsdb());

        BridgeConfig bridgeConfig = device.as(BridgeConfig.class);

        Set<String> bridgeNames = bridgeConfig.getBridges().stream()
                .map(BridgeDescription::name).collect(Collectors.toSet());

        Set<String> phyNetworkNames = osNode.phyIntfs().stream()
                .map(pi -> BRIDGE_PREFIX + pi.network()).collect(Collectors.toSet());

        // we remove existing physical bridges and patch ports, if the physical
        // bridges are not defined in openstack node
        bridgeNames.forEach(brName -> {
            if (!phyNetworkNames.contains(brName) && !brName.equals(INTEGRATION_BRIDGE)) {
                removePhysicalPatchPorts(osNode, brName.substring(NETWORK_BEGIN));
                removePhysicalBridge(osNode, brName.substring(NETWORK_BEGIN));
            }
        });
    }

    private void unprovisionPhysicalInterfaces(OpenstackNode osNode) {
        osNode.phyIntfs().forEach(pi -> {
            detachPhysicalPort(osNode, pi.network(), pi.intf());
            removePhysicalPatchPorts(osNode, pi.network());
            removePhysicalBridge(osNode, pi.network());
        });
    }

    private void createPhysicalBridge(OpenstackNode osNode,
                                      OpenstackPhyInterface phyInterface) {
        Device device = deviceService.getDevice(osNode.ovsdb());

        String bridgeName = BRIDGE_PREFIX + phyInterface.network();

        BridgeDescription.Builder builder = DefaultBridgeDescription.builder()
                .name(bridgeName)
                .mcastSnoopingEnable();

        BridgeConfig bridgeConfig = device.as(BridgeConfig.class);
        bridgeConfig.addBridge(builder.build());
    }

    private void removePhysicalBridge(OpenstackNode osNode, String network) {
        Device device = deviceService.getDevice(osNode.ovsdb());

        BridgeName bridgeName = BridgeName.bridgeName(BRIDGE_PREFIX + network);

        BridgeConfig bridgeConfig = device.as(BridgeConfig.class);
        bridgeConfig.deleteBridge(bridgeName);
    }

    private void createPhysicalPatchPorts(OpenstackNode osNode,
                                          OpenstackPhyInterface phyInterface) {
        Device device = deviceService.getDevice(osNode.ovsdb());

        if (device == null || !device.is(InterfaceConfig.class)) {
            log.error("Failed to create patch interface on {}", osNode.ovsdb());
            return;
        }

        String physicalDeviceId = BRIDGE_PREFIX + phyInterface.network();

        String intToPhyPatchPort = structurePortName(
                INTEGRATION_TO_PHYSICAL_PREFIX + phyInterface.network());
        String phyToIntPatchPort = structurePortName(
                phyInterface.network() + PHYSICAL_TO_INTEGRATION_SUFFIX);

        // integration bridge -> physical bridge
        PatchDescription intToPhyPatchDesc =
                DefaultPatchDescription.builder()
                        .deviceId(INTEGRATION_BRIDGE)
                        .ifaceName(intToPhyPatchPort)
                        .peer(phyToIntPatchPort)
                        .build();

        // physical bridge -> integration bridge
        PatchDescription phyToIntPatchDesc =
                DefaultPatchDescription.builder()
                        .deviceId(physicalDeviceId)
                        .ifaceName(phyToIntPatchPort)
                        .peer(intToPhyPatchPort)
                        .build();

        InterfaceConfig ifaceConfig = device.as(InterfaceConfig.class);
        ifaceConfig.addPatchMode(INTEGRATION_TO_PHYSICAL_PREFIX +
                phyInterface.network(), intToPhyPatchDesc);
        ifaceConfig.addPatchMode(phyInterface.network() +
                PHYSICAL_TO_INTEGRATION_SUFFIX, phyToIntPatchDesc);

        addOrRemoveSystemInterface(osNode, physicalDeviceId,
                phyInterface.intf(), deviceService, true);
    }

    private void removePhysicalPatchPorts(OpenstackNode osNode, String network) {
        Device device = deviceService.getDevice(osNode.ovsdb());

        if (device == null || !device.is(InterfaceConfig.class)) {
            log.error("Failed to remove patch interface on {}", osNode.ovsdb());
            return;
        }

        String intToPhyPatchPort = structurePortName(
                INTEGRATION_TO_PHYSICAL_PREFIX + network);

        InterfaceConfig ifaceConfig = device.as(InterfaceConfig.class);
        ifaceConfig.removePatchMode(intToPhyPatchPort);
    }

    private void attachPhysicalPort(OpenstackNode osNode,
                                    OpenstackPhyInterface phyInterface) {

        String physicalDeviceId = BRIDGE_PREFIX + phyInterface.network();

        addOrRemoveSystemInterface(osNode, physicalDeviceId,
                phyInterface.intf(), deviceService, true);
    }

    private void detachPhysicalPort(OpenstackNode osNode, String network, String portName) {
        String physicalDeviceId = BRIDGE_PREFIX + network;

        addOrRemoveSystemInterface(osNode, physicalDeviceId, portName, deviceService, false);
    }

    /**
     * Creates a VXLAN tunnel interface in a given openstack node.
     *
     * @param osNode openstack node
     */
    private void createVxlanTunnelInterface(OpenstackNode osNode) {
        createTunnelInterface(osNode, VXLAN, VXLAN_TUNNEL);
    }

    /**
     * Creates a GRE tunnel interface in a given openstack node.
     *
     * @param osNode openstack node
     */
    private void createGreTunnelInterface(OpenstackNode osNode) {
        createTunnelInterface(osNode, GRE, GRE_TUNNEL);
    }

    /**
     * Creates a GENEVE tunnel interface in a given openstack node.
     *
     * @param osNode openstack node
     */
    private void createGeneveTunnelInterface(OpenstackNode osNode) {
        createTunnelInterface(osNode, GENEVE, GENEVE_TUNNEL);
    }

    /**
     * Creates a tunnel interface in a given openstack node.
     *
     * @param osNode openstack node
     */
    private void createTunnelInterface(OpenstackNode osNode,
                                       String type, String intfName) {
        if (isIntfEnabled(osNode, intfName)) {
            return;
        }

        Device device = deviceService.getDevice(osNode.ovsdb());
        if (device == null || !device.is(InterfaceConfig.class)) {
            log.error("Failed to create tunnel interface on {}", osNode.ovsdb());
            return;
        }

        TunnelDescription tunnelDesc = buildTunnelDesc(type, intfName);

        InterfaceConfig ifaceConfig = device.as(InterfaceConfig.class);
        ifaceConfig.addTunnelMode(intfName, tunnelDesc);
    }

    /**
     * Builds tunnel description according to the network type.
     *
     * @param type network type
     * @return tunnel description
     */
    private TunnelDescription buildTunnelDesc(String type, String intfName) {
        if (VXLAN.equals(type) || GRE.equals(type) || GENEVE.equals(type)) {
            TunnelDescription.Builder tdBuilder =
                    DefaultTunnelDescription.builder()
                    .deviceId(INTEGRATION_BRIDGE)
                    .ifaceName(intfName)
                    .remote(TunnelEndPoints.flowTunnelEndpoint())
                    .key(TunnelKeys.flowTunnelKey());

            switch (type) {
                case VXLAN:
                    tdBuilder.type(TunnelDescription.Type.VXLAN);
                    break;
                case GRE:
                    tdBuilder.type(TunnelDescription.Type.GRE);
                    break;
                case GENEVE:
                    tdBuilder.type(TunnelDescription.Type.GENEVE);
                    break;
                default:
                    return null;
            }

            return tdBuilder.build();
        }
        return null;
    }

    /**
     * Checks whether a given network interface in a given openstack node
     * is enabled or not.
     *
     * @param osNode openstack node
     * @param intf network interface name
     * @return true if the given interface is enabled, false otherwise
     */
    private boolean isIntfEnabled(OpenstackNode osNode, String intf) {
        return deviceService.isAvailable(osNode.intgBridge()) &&
                deviceService.getPorts(osNode.intgBridge()).stream()
                        .anyMatch(port -> Objects.equals(
                                port.annotations().value(PORT_NAME), intf) &&
                                port.isEnabled());
    }

    private boolean hasPhyBridge(OpenstackNode osNode, String bridgeName) {
        BridgeConfig bridgeConfig = deviceService.getDevice(osNode.ovsdb()).as(BridgeConfig.class);
        return bridgeConfig.getBridges().stream().anyMatch(br -> br.name().equals(bridgeName));
    }

    private boolean hasPhyPatchPort(OpenstackNode osNode, String patchPortName) {
        List<Port> ports = deviceService.getPorts(osNode.intgBridge());
        return ports.stream().anyMatch(p -> p.annotations().value(PORT_NAME).equals(patchPortName));
    }

    private boolean hasPhyIntf(OpenstackNode osNode, String intfName) {
        BridgeConfig bridgeConfig = deviceService.getDevice(osNode.ovsdb()).as(BridgeConfig.class);
        return bridgeConfig.getPorts().stream().anyMatch(p -> p.annotations().value(PORT_NAME).equals(intfName));
    }

    private boolean initStateDone(OpenstackNode osNode) {
        if (!isOvsdbConnected(osNode, ovsdbPortNum, ovsdbController, deviceService)) {
            return false;
        }

        boolean initStateDone = deviceService.isAvailable(osNode.intgBridge());
        if (hasDpdkTunnelBridge(osNode)) {
            initStateDone = initStateDone && dpdkTunnelBridgeCreated(osNode);
        }

        cleanPhysicalInterfaces(osNode);

        return initStateDone;
    }

    private boolean deviceCreatedStateDone(OpenstackNode osNode) {
        if (osNode.dataIp() != null &&
                !isIntfEnabled(osNode, VXLAN_TUNNEL)) {
            return false;
        }
        if (osNode.dataIp() != null &&
                !isIntfEnabled(osNode, GRE_TUNNEL)) {
            return false;
        }
        if (osNode.dataIp() != null &&
                !isIntfEnabled(osNode, GENEVE_TUNNEL)) {
            return false;
        }
        if (osNode.vlanIntf() != null &&
                !isIntfEnabled(osNode, osNode.vlanIntf())) {
            return false;
        }
        if (osNode.type() == GATEWAY &&
                !isIntfEnabled(osNode, osNode.uplinkPort())) {
            return false;
        }
        if (osNode.dpdkConfig() != null &&
                osNode.dpdkConfig().dpdkIntfs() != null &&
                !isDpdkIntfsCreated(osNode, osNode.dpdkConfig().dpdkIntfs())) {
            return false;
        }

        for (OpenstackPhyInterface phyIntf : osNode.phyIntfs()) {
            if (phyIntf == null) {
                return false;
            }

            String bridgeName = BRIDGE_PREFIX + phyIntf.network();
            String patchPortName = structurePortName(
                    INTEGRATION_TO_PHYSICAL_PREFIX + phyIntf.network());

            if (!(hasPhyBridge(osNode, bridgeName) &&
                    hasPhyPatchPort(osNode, patchPortName) &&
                    hasPhyIntf(osNode, phyIntf.intf()))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks whether all requirements for this state are fulfilled or not.
     *
     * @param osNode openstack node
     * @return true if all requirements are fulfilled, false otherwise
     */
    private boolean isCurrentStateDone(OpenstackNode osNode) {
        switch (osNode.state()) {
            case INIT:
                return initStateDone(osNode);
            case DEVICE_CREATED:
                return deviceCreatedStateDone(osNode);
            case COMPLETE:
            case INCOMPLETE:
                // always return false
                // run init CLI to re-trigger node bootstrap
                return false;
            default:
                return true;
        }
    }

    private boolean isDpdkIntfsCreated(OpenstackNode osNode,
                                       Collection<DpdkInterface> dpdkInterfaces) {
        OvsdbClientService client = getOvsdbClient(osNode, ovsdbPortNum, ovsdbController);
        if (client == null) {
            log.info("Failed to get ovsdb client");
            return false;
        }

        Set<OvsdbPort> ports = client.getPorts();

        for (DpdkInterface dpdkIntf : dpdkInterfaces) {
            Optional<OvsdbPort> port = ports.stream()
                    .filter(ovsdbPort -> ovsdbPort.portName().value().equals(dpdkIntf.intf()))
                    .findAny();

            if (!port.isPresent()) {
                return false;
            }
            Interface intf = client.getInterface(dpdkIntf.intf());
            if (intf == null) {
                return false;
            }

            OvsdbSet mtu = (OvsdbSet) intf.getMtuColumn().data();
            if (mtu == null) {
                return false;
            }

            OvsdbMap option = (OvsdbMap) intf.getOptionsColumn().data();
            if (option == null) {
                return false;
            }

            if (!mtu.set().contains(dpdkIntf.mtu().intValue()) ||
                    !option.toString().contains(dpdkIntf.pciAddress())) {
                log.trace("The dpdk interface {} was created but mtu or " +
                          "pci address is different from the config.");
                return false;
            }
        }
        return true;
    }

    /**
     * Configures the openstack node with new state.
     *
     * @param osNode openstack node
     * @param newState a new state
     */
    private void setState(OpenstackNode osNode, NodeState newState) {
        if (osNode.state() == newState) {
            return;
        }
        OpenstackNode updated = osNode.updateState(newState);
        osNodeAdminService.updateNode(updated);
        log.info("Changed {} state: {}", osNode.hostname(), newState);
    }

    /**
     * Bootstraps a new openstack node.
     *
     * @param osNode openstack node
     */
    private void bootstrapNode(OpenstackNode osNode) {
        if (osNode.type() == CONTROLLER) {
            if (osNode.state() == INIT && checkEndpoint(osNode)) {
                setState(osNode, COMPLETE);
            }
        } else {
            if (isCurrentStateDone(osNode)) {
                setState(osNode, osNode.state().nextState());
            } else {
                log.trace("Processing {} state for {}", osNode.state(),
                                                        osNode.hostname());
                osNode.state().process(this, osNode);
            }
        }
    }

    private void removeVlanInterface(OpenstackNode osNode) {
        if (osNode.vlanIntf() != null) {
            Optional<DpdkInterface> dpdkIntf =
                                dpdkInterfaceByIntfName(osNode, osNode.vlanIntf());

            removeInterfaceOnIntegrationBridge(osNode, osNode.vlanIntf(), dpdkIntf);
        }
    }

    private Optional<DpdkInterface> dpdkInterfaceByIntfName(OpenstackNode osNode,
                                                            String intf) {
        return osNode.dpdkConfig() == null ? Optional.empty() :
                osNode.dpdkConfig().dpdkIntfs().stream()
                        .filter(dpdkIntf -> dpdkIntf.intf().equals(intf))
                        .findAny();
    }

    private void removeInterfaceOnIntegrationBridge(OpenstackNode osNode,
                                      String intfName,
                                      Optional<DpdkInterface> dpdkInterface) {
        if (dpdkInterface.isPresent()) {
            addOrRemoveDpdkInterface(osNode, dpdkInterface.get(), ovsdbPortNum,
                    ovsdbController, false);
        } else {
            addOrRemoveSystemInterface(osNode, INTEGRATION_BRIDGE, intfName, deviceService,
                    false);
        }
    }

    /**
     * Checks the validity of the given endpoint.
     *
     * @param osNode gateway node
     * @return validity result
     */
    private boolean checkEndpoint(OpenstackNode osNode) {
        if (osNode == null) {
            log.warn("Keystone auth info has not been configured. " +
                     "Please specify auth info via network-cfg.json.");
            return false;
        }

        OSClient client = getConnectedClient(osNode);

        if (client == null) {
            return false;
        } else {
            return client.getSupportedServices().size() != 0;
        }
    }

    /**
     * Extracts properties from the component configuration context.
     *
     * @param context the component context
     */
    private void readComponentConfiguration(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();

        Integer ovsdbPortConfigured = Tools.getIntegerProperty(properties, OVSDB_PORT);
        if (ovsdbPortConfigured == null) {
            ovsdbPortNum = OVSDB_PORT_NUM_DEFAULT;
            log.info("OVSDB port is NOT configured, default value is {}", ovsdbPortNum);
        } else {
            ovsdbPortNum = ovsdbPortConfigured;
            log.info("Configured. OVSDB port is {}", ovsdbPortNum);
        }

        Boolean autoRecoveryConfigured =
                getBooleanProperty(properties, AUTO_RECOVERY);
        if (autoRecoveryConfigured == null) {
            autoRecovery = AUTO_RECOVERY_DEFAULT;
            log.info("Auto recovery flag is NOT " +
                    "configured, default value is {}", autoRecovery);
        } else {
            autoRecovery = autoRecoveryConfigured;
            log.info("Configured. Auto recovery flag is {}", autoRecovery);
        }
    }

    /**
     * An internal OVSDB listener. This listener is used for listening the
     * network facing events from OVSDB device. If a new OVSDB device is detected,
     * ONOS tries to bootstrap the openstack node.
     */
    private class InternalOvsdbListener implements DeviceListener {

        @Override
        public boolean isRelevant(DeviceEvent event) {
            return event.subject().type() == Device.Type.CONTROLLER;
        }

        private boolean isRelevantHelper() {
            return Objects.equals(localNode, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(DeviceEvent event) {
            Device device = event.subject();

            switch (event.type()) {
                case DEVICE_AVAILABILITY_CHANGED:
                case DEVICE_ADDED:
                    eventExecutor.execute(() -> {
                        if (!isRelevantHelper()) {
                            return;
                        }
                        processDeviceAddedOfOvsdbDevice(osNodeService.node(device.id()), device);
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

        private void processDeviceAddedOfOvsdbDevice(OpenstackNode osNode, Device device) {
            if (osNode == null || osNode.type() == CONTROLLER) {
                return;
            }

            if (deviceService.isAvailable(device.id())) {
                log.debug("OVSDB {} detected", device.id());
                bootstrapNode(osNode);
            }
        }
    }

    /**
     * An internal integration bridge listener. This listener is used for
     * listening the events from integration bridge. To listen the events from
     * other types of bridge such as provider bridge or tunnel bridge, we need
     * to augment OpenstackNodeService.node() method.
     */
    private class InternalBridgeListener implements DeviceListener {

        @Override
        public boolean isRelevant(DeviceEvent event) {
            return event.subject().type() == Device.Type.SWITCH;
        }

        private boolean isRelevantHelper() {
            return Objects.equals(localNode, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(DeviceEvent event) {
            Device device = event.subject();

            switch (event.type()) {
                case DEVICE_AVAILABILITY_CHANGED:
                case DEVICE_ADDED:
                    eventExecutor.execute(() -> {
                        if (!isRelevantHelper()) {
                            return;
                        }
                        processDeviceAddedOfBridge(osNodeService.node(device.id()), device);
                    });
                    break;
                case PORT_UPDATED:
                case PORT_ADDED:
                    eventExecutor.execute(() -> {
                        if (!isRelevantHelper()) {
                            return;
                        }
                        processPortAddedOfBridge(osNodeService.node(device.id()), event.port());
                    });
                    break;
                case PORT_REMOVED:
                    eventExecutor.execute(() -> {
                        if (!isRelevantHelper()) {
                            return;
                        }
                        processPortRemovedOfBridge(osNodeService.node(device.id()), event.port());
                    });
                    break;
                case DEVICE_REMOVED:
                default:
                    // do nothing
                    break;
            }
        }

        private void processDeviceAddedOfBridge(OpenstackNode osNode, Device device) {

            if (osNode == null || osNode.type() == CONTROLLER) {
                return;
            }

            if (deviceService.isAvailable(device.id())) {
                log.debug("Integration bridge created on {}", osNode.hostname());
                bootstrapNode(osNode);
            } else if (osNode.state() == COMPLETE) {
                log.info("Device {} disconnected", device.id());
                setState(osNode, INCOMPLETE);
            }

            if (autoRecovery) {
                if (osNode.state() == INCOMPLETE ||
                        osNode.state() == DEVICE_CREATED) {
                    log.info("Device {} is reconnected", device.id());
                    osNodeAdminService.updateNode(
                            osNode.updateState(NodeState.INIT));
                }
            }
        }

        private void processPortAddedOfBridge(OpenstackNode osNode, Port port) {
            if (osNode == null || osNode.type() == CONTROLLER) {
                return;
            }

            String portName = port.annotations().value(PORT_NAME);
            if (osNode.state() == DEVICE_CREATED && (
                    Objects.equals(portName, VXLAN_TUNNEL) ||
                            Objects.equals(portName, GRE_TUNNEL) ||
                            Objects.equals(portName, GENEVE_TUNNEL) ||
                            Objects.equals(portName, osNode.vlanIntf()) ||
                            Objects.equals(portName, osNode.uplinkPort()) ||
                            containsPatchPort(osNode, portName)) ||
                    containsDpdkIntfs(osNode, portName)) {
                log.info("Interface {} added or updated to {}",
                        portName, osNode.intgBridge());
                bootstrapNode(osNode);
            }
        }

        private void processPortRemovedOfBridge(OpenstackNode osNode, Port port) {
            if (osNode == null || osNode.type() == CONTROLLER) {
                return;
            }

            String portName = port.annotations().value(PORT_NAME);
            if (osNode.state() == COMPLETE && (
                    Objects.equals(portName, VXLAN_TUNNEL) ||
                            Objects.equals(portName, GRE_TUNNEL) ||
                            Objects.equals(portName, GENEVE_TUNNEL) ||
                            Objects.equals(portName, osNode.vlanIntf()) ||
                            Objects.equals(portName, osNode.uplinkPort()) ||
                            containsPatchPort(osNode, portName)) ||
                    containsDpdkIntfs(osNode, portName)) {
                log.warn("Interface {} removed from {}",
                        portName, osNode.intgBridge());
                setState(osNode, INCOMPLETE);
            }
        }

        /**
         * Checks whether the openstack node contains the given patch port.
         *
         * @param osNode    openstack node
         * @param portName  patch port name
         * @return true if openstack node contains the given patch port,
         *         false otherwise
         */
        private boolean containsPatchPort(OpenstackNode osNode, String portName) {
            return osNode.phyIntfs().stream()
                    .anyMatch(pi -> structurePortName(INTEGRATION_TO_PHYSICAL_PREFIX
                            + pi.network()).equals(portName));
        }

        /**
         * Checks whether the openstack node contains the given dpdk interface.
         *
         * @param osNode openstack node
         * @param portName dpdk interface
         * @return true if openstack node contains the given dpdk interface,
         *          false otherwise
         */
        private boolean containsDpdkIntfs(OpenstackNode osNode, String portName) {
            if (osNode.dpdkConfig() == null) {
                return false;
            }
            return osNode.dpdkConfig().dpdkIntfs().stream()
                    .anyMatch(dpdkInterface -> dpdkInterface.intf().equals(portName));
        }
    }

    /**
     * An internal openstack node listener.
     * The notification is triggered by OpenstackNodeStore.
     */
    private class InternalOpenstackNodeListener implements OpenstackNodeListener {

        private boolean isRelevantHelper() {
            return Objects.equals(localNode, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(OpenstackNodeEvent event) {
            switch (event.type()) {
                case OPENSTACK_NODE_CREATED:
                case OPENSTACK_NODE_UPDATED:
                    eventExecutor.execute(() -> {

                        if (!isRelevantHelper()) {
                            return;
                        }

                        bootstrapNode(event.subject());
                    });
                    break;
                case OPENSTACK_NODE_REMOVED:
                    eventExecutor.execute(() -> {

                        if (!isRelevantHelper()) {
                            return;
                        }
                        processOpenstackNodeRemoved(event.subject());
                    });
                    break;
                case OPENSTACK_NODE_COMPLETE:
                default:
                    break;
            }
        }

        private void processOpenstackNodeRemoved(OpenstackNode osNode) {
            OvsdbClientService client = getOvsdbClient(osNode, ovsdbPortNum, ovsdbController);
            if (client == null) {
                log.info("Failed to get ovsdb client");
                return;
            }

            // unprovision physical interfaces from the node
            // this procedure includes detaching physical port from physical bridge,
            // remove patch ports from br-int, removing physical bridge
            unprovisionPhysicalInterfaces(osNode);

            //delete vlan interface from the node
            removeVlanInterface(osNode);

            //delete dpdk interfaces from the node
            if (osNode.dpdkConfig() != null) {
                osNode.dpdkConfig().dpdkIntfs().forEach(dpdkInterface -> {
                    if (isDpdkIntfsCreated(osNode, Lists.newArrayList(dpdkInterface))) {
                        addOrRemoveDpdkInterface(osNode, dpdkInterface, ovsdbPortNum,
                                ovsdbController, false);
                    }
                });
            }

            //delete tunnel bridge from the node
            if (hasDpdkTunnelBridge(osNode)) {
                client.dropBridge(TUNNEL_BRIDGE);
            }

            //delete integration bridge from the node
            client.dropBridge(INTEGRATION_BRIDGE);

            //disconnect ovsdb
            client.disconnect();
        }
    }
}
