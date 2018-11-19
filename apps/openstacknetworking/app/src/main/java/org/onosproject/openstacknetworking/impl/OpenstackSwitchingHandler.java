/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.openstacknetworking.impl;

import com.google.common.base.Strings;
import org.onlab.packet.Ethernet;
import org.onlab.packet.VlanId;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cfg.ConfigProperty;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.openstacknetworking.api.InstancePort;
import org.onosproject.openstacknetworking.api.InstancePortEvent;
import org.onosproject.openstacknetworking.api.InstancePortListener;
import org.onosproject.openstacknetworking.api.InstancePortService;
import org.onosproject.openstacknetworking.api.OpenstackFlowRuleService;
import org.onosproject.openstacknetworking.api.OpenstackNetworkEvent;
import org.onosproject.openstacknetworking.api.OpenstackNetworkListener;
import org.onosproject.openstacknetworking.api.OpenstackNetworkService;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeService;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.NetworkType;
import org.openstack4j.model.network.Port;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.openstacknetworking.api.Constants.ACL_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.ARP_BROADCAST_MODE;
import static org.onosproject.openstacknetworking.api.Constants.ARP_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.DHCP_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.FLAT_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.FORWARDING_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.OPENSTACK_NETWORKING_APP_ID;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_ADMIN_RULE;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_FLAT_DOWNSTREAM_RULE;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_FLAT_JUMP_DOWNSTREAM_RULE;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_FLAT_JUMP_UPSTREAM_RULE;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_FLAT_UPSTREAM_RULE;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_SWITCHING_RULE;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_TUNNEL_TAG_RULE;
import static org.onosproject.openstacknetworking.api.Constants.STAT_FLAT_OUTBOUND_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.VTAG_TABLE;
import static org.onosproject.openstacknetworking.api.InstancePortEvent.Type.OPENSTACK_INSTANCE_MIGRATION_STARTED;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.getPropertyValue;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.swapStaleLocation;
import static org.onosproject.openstacknetworking.util.RulePopulatorUtil.buildExtension;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.COMPUTE;
import static org.slf4j.LoggerFactory.getLogger;


/**
 * Populates switching flow rules on OVS for the basic connectivity among the
 * virtual instances in the same network.
 */
@Component(immediate = true)
public final class OpenstackSwitchingHandler {

    private final Logger log = getLogger(getClass());

    private static final String ARP_MODE = "arpMode";
    private static final String ERR_SET_FLOWS_VNI = "Failed to set flows for %s: Failed to get VNI for %s";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)

    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackFlowRuleService osFlowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected InstancePortService instancePortService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackNetworkService osNetworkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackNodeService osNodeService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler"));
    private final InstancePortListener instancePortListener = new InternalInstancePortListener();
    private final InternalOpenstackNetworkListener osNetworkListener =
                                            new InternalOpenstackNetworkListener();
    private ApplicationId appId;
    private NodeId localNodeId;

    @Activate
    void activate() {
        appId = coreService.registerApplication(OPENSTACK_NETWORKING_APP_ID);
        localNodeId = clusterService.getLocalNode().id();
        instancePortService.addListener(instancePortListener);
        osNetworkService.addListener(osNetworkListener);

        log.info("Started");
    }

    @Deactivate
    void deactivate() {
        osNetworkService.removeListener(osNetworkListener);
        instancePortService.removeListener(instancePortListener);
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    /**
     * Configures L2 forwarding rules.
     * Currently, SONA supports Flat, VXLAN and VLAN modes.
     *
     * @param instPort instance port object
     * @param install install flag, add the rule if true, remove it otherwise
     */
    private void setNetworkRules(InstancePort instPort, boolean install) {
        NetworkType type = osNetworkService.network(instPort.networkId()).getNetworkType();
        switch (type) {
            case VXLAN:
                setTunnelTagIpFlowRules(instPort, install);
                setForwardingRulesForVxlan(instPort, install);

                if (ARP_BROADCAST_MODE.equals(getArpMode())) {
                    setTunnelTagArpFlowRules(instPort, install);
                }

                break;
            case VLAN:
                setVlanTagIpFlowRules(instPort, install);
                setForwardingRulesForVlan(instPort, install);

                if (ARP_BROADCAST_MODE.equals(getArpMode())) {
                    setVlanTagArpFlowRules(instPort, install);
                }

                break;
            case FLAT:
                setFlatJumpRules(instPort, install);
                setDownstreamRulesForFlat(instPort, install);
                setUpstreamRulesForFlat(instPort, install);
                break;
            default:
                log.warn("Unsupported network tunnel type {}", type.name());
                break;
        }
    }

    /**
     * Removes virtual port.
     *
     * @param instPort instance port
     */
    private void removeVportRules(InstancePort instPort) {
        NetworkType type = osNetworkService.network(instPort.networkId()).getNetworkType();

        switch (type) {
            case VXLAN:
                setTunnelTagIpFlowRules(instPort, false);

                if (ARP_BROADCAST_MODE.equals(getArpMode())) {
                    setTunnelTagArpFlowRules(instPort, false);
                }

                break;
            case VLAN:
                setVlanTagIpFlowRules(instPort, false);

                if (ARP_BROADCAST_MODE.equals(getArpMode())) {
                    setVlanTagArpFlowRules(instPort, false);
                }

                break;
            case FLAT:
                setFlatJumpRules(instPort, false);
                setUpstreamRulesForFlat(instPort, false);
                setDownstreamRulesForFlat(instPort, false);
                break;
            default:
                log.warn("Unsupported network type {}", type.name());
                break;
        }
    }

    private void setFlatJumpRules(InstancePort port, boolean install) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        selector.matchInPort(port.portNumber());

        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
        treatment.transition(STAT_FLAT_OUTBOUND_TABLE);

        osFlowRuleService.setRule(
                appId,
                port.deviceId(),
                selector.build(),
                treatment.build(),
                PRIORITY_FLAT_JUMP_UPSTREAM_RULE,
                DHCP_TABLE,
                install);

        Network network = osNetworkService.network(port.networkId());

        if (network == null) {
            log.warn("The network does not exist");
            return;
        }
        PortNumber portNumber = osNodeService.node(port.deviceId())
                .phyIntfPortNum(network.getProviderPhyNet());

        if (portNumber == null) {
            log.warn("The port number does not exist");
            return;
        }

        selector = DefaultTrafficSelector.builder();
        selector.matchInPort(portNumber)
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(port.ipAddress().toIpPrefix());

        osFlowRuleService.setRule(
                appId,
                port.deviceId(),
                selector.build(),
                treatment.build(),
                PRIORITY_FLAT_JUMP_DOWNSTREAM_RULE,
                DHCP_TABLE,
                install);

        selector = DefaultTrafficSelector.builder();
        selector.matchInPort(portNumber)
                .matchEthType(Ethernet.TYPE_ARP)
                .matchArpTpa(port.ipAddress().getIp4Address());

        osFlowRuleService.setRule(
                appId,
                port.deviceId(),
                selector.build(),
                treatment.build(),
                PRIORITY_FLAT_JUMP_DOWNSTREAM_RULE,
                DHCP_TABLE,
                install);
    }

    private void setDownstreamRulesForFlat(InstancePort instPort, boolean install) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(instPort.ipAddress().toIpPrefix())
                .build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(instPort.portNumber())
                .build();

        osFlowRuleService.setRule(
                appId,
                instPort.deviceId(),
                selector,
                treatment,
                PRIORITY_FLAT_DOWNSTREAM_RULE,
                FLAT_TABLE,
                install);

        selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_ARP)
                .matchArpTpa(instPort.ipAddress().getIp4Address())
                .build();

        osFlowRuleService.setRule(
                appId,
                instPort.deviceId(),
                selector,
                treatment,
                PRIORITY_FLAT_DOWNSTREAM_RULE,
                FLAT_TABLE,
                install);
    }

    private void setUpstreamRulesForFlat(InstancePort instPort, boolean install) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchInPort(instPort.portNumber())
                .build();

        Network network = osNetworkService.network(instPort.networkId());

        if (network == null) {
            log.warn("The network does not exist");
            return;
        }

        PortNumber portNumber = osNodeService.node(instPort.deviceId())
                .phyIntfPortNum(network.getProviderPhyNet());

        if (portNumber == null) {
            log.warn("The port number does not exist");
            return;
        }

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(portNumber)
                .build();

        osFlowRuleService.setRule(
                appId,
                instPort.deviceId(),
                selector,
                treatment,
                PRIORITY_FLAT_UPSTREAM_RULE,
                FLAT_TABLE,
                install);
    }

    /**
     * Configures the flow rules which are used for L2 packet switching.
     * Note that these rules will be inserted in switching table (table 5).
     *
     * @param instPort instance port object
     * @param install install flag, add the rule if true, remove it otherwise
     */
    private void setForwardingRulesForVxlan(InstancePort instPort, boolean install) {
        // switching rules for the instPorts in the same node
        TrafficSelector selector = DefaultTrafficSelector.builder()
                // TODO: need to handle IPv6 in near future
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(instPort.ipAddress().toIpPrefix())
                .matchTunnelId(getVni(instPort))
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                // TODO: this might not be necessary for the VMs located in the same subnet
                .setEthDst(instPort.macAddress())
                .setOutput(instPort.portNumber())
                .build();

        osFlowRuleService.setRule(
                appId,
                instPort.deviceId(),
                selector,
                treatment,
                PRIORITY_SWITCHING_RULE,
                FORWARDING_TABLE,
                install);

        // switching rules for the instPorts in the remote node
        OpenstackNode localNode = osNodeService.node(instPort.deviceId());
        if (localNode == null) {
            final String error = String.format("Cannot find openstack node for %s",
                    instPort.deviceId());
            throw new IllegalStateException(error);
        }
        osNodeService.completeNodes(COMPUTE).stream()
                .filter(remoteNode -> !remoteNode.intgBridge().equals(localNode.intgBridge()))
                .forEach(remoteNode -> {
                    TrafficTreatment treatmentToRemote = DefaultTrafficTreatment.builder()
                            .extension(buildExtension(
                                    deviceService,
                                    remoteNode.intgBridge(),
                                    localNode.dataIp().getIp4Address()),
                                    remoteNode.intgBridge())
                            .setOutput(remoteNode.tunnelPortNum())
                            .build();

                    osFlowRuleService.setRule(
                            appId,
                            remoteNode.intgBridge(),
                            selector,
                            treatmentToRemote,
                            PRIORITY_SWITCHING_RULE,
                            FORWARDING_TABLE,
                            install);
                });
    }

    /**
     * Configures the flow rules which are used for L2 VLAN packet switching.
     * Note that these rules will be inserted in switching table (table 5).
     *
     * @param instPort instance port object
     * @param install install flag, add the rule if true, remove it otherwise
     */
    private void setForwardingRulesForVlan(InstancePort instPort, boolean install) {
        // switching rules for the instPorts in the same node
        TrafficSelector selector = DefaultTrafficSelector.builder()
                // TODO: need to handle IPv6 in near future
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(instPort.ipAddress().toIpPrefix())
                .matchVlanId(getVlanId(instPort))
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .popVlan()
                // TODO: this might not be necessary for the VMs located in the same subnet
                .setEthDst(instPort.macAddress())
                .setOutput(instPort.portNumber())
                .build();

        osFlowRuleService.setRule(
                appId,
                instPort.deviceId(),
                selector,
                treatment,
                PRIORITY_SWITCHING_RULE,
                FORWARDING_TABLE,
                install);

        // switching rules for the instPorts in the remote node
        osNodeService.completeNodes(COMPUTE).stream()
                .filter(remoteNode -> !remoteNode.intgBridge().equals(instPort.deviceId()) &&
                        remoteNode.vlanIntf() != null)
                .forEach(remoteNode -> {
                    TrafficTreatment treatmentToRemote = DefaultTrafficTreatment.builder()
                            .setEthDst(instPort.macAddress())
                            .setOutput(remoteNode.vlanPortNum())
                            .build();

                    osFlowRuleService.setRule(
                            appId,
                            remoteNode.intgBridge(),
                            selector,
                            treatmentToRemote,
                            PRIORITY_SWITCHING_RULE,
                            FORWARDING_TABLE,
                            install);
                });
    }

    private void setTunnelTagArpFlowRules(InstancePort instPort, boolean install) {
        setTunnelTagFlowRules(instPort, Ethernet.TYPE_ARP, install);
    }

    private void setTunnelTagIpFlowRules(InstancePort instPort, boolean install) {
        setTunnelTagFlowRules(instPort, Ethernet.TYPE_IPV4, install);
    }

    /**
     * Configures the flow rule which is for using VXLAN to tag the packet
     * based on the in_port number of a virtual instance.
     * Note that this rule will be inserted in vTag table.
     *
     * @param instPort instance port object
     * @param install install flag, add the rule if true, remove it otherwise
     */
    private void setTunnelTagFlowRules(InstancePort instPort,
                                       short ethType,
                                       boolean install) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(ethType)
                .matchInPort(instPort.portNumber())
                .build();

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder()
                .setTunnelId(getVni(instPort));


        if (ethType == Ethernet.TYPE_ARP) {
            tBuilder.transition(ARP_TABLE);
        } else if (ethType == Ethernet.TYPE_IPV4) {
            tBuilder.transition(ACL_TABLE);
        }

        osFlowRuleService.setRule(
                appId,
                instPort.deviceId(),
                selector,
                tBuilder.build(),
                PRIORITY_TUNNEL_TAG_RULE,
                VTAG_TABLE,
                install);
    }

    private void setVlanTagIpFlowRules(InstancePort instPort, boolean install) {
        setVlanTagFlowRules(instPort, Ethernet.TYPE_IPV4, install);
    }

    private void setVlanTagArpFlowRules(InstancePort instPort, boolean install) {
        setVlanTagFlowRules(instPort, Ethernet.TYPE_ARP, install);
    }

    /**
     * Configures the flow rule which is for using VLAN to tag the packet
     * based on the in_port number of a virtual instance.
     * Note that this rule will be inserted in vTag table.
     *
     * @param instPort instance port object
     * @param install install flag, add the rule if true, remove it otherwise
     */
    private void setVlanTagFlowRules(InstancePort instPort,
                                     short ethType,
                                     boolean install) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(ethType)
                .matchInPort(instPort.portNumber())
                .build();

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder()
                .pushVlan()
                .setVlanId(getVlanId(instPort));

        if (ethType == Ethernet.TYPE_ARP) {
            tBuilder.transition(ARP_TABLE);
        } else if (ethType == Ethernet.TYPE_IPV4) {
            tBuilder.transition(ACL_TABLE);
        }

        osFlowRuleService.setRule(
                appId,
                instPort.deviceId(),
                selector,
                tBuilder.build(),
                PRIORITY_TUNNEL_TAG_RULE,
                VTAG_TABLE,
                install);
    }

    private void setNetworkBlockRules(Network network, boolean install) {

        NetworkType type = network.getNetworkType();

        // TODO: we block a network traffic by referring to segment ID for now
        // we might need to find a better way to block the traffic of a network
        // in case the segment ID is overlapped in different types network (VXLAN, VLAN)
        switch (type) {
            case VXLAN:
                setNetworkBlockRulesForVxlan(network.getProviderSegID(), install);
                break;
            case VLAN:
                setNetworkBlockRulesForVlan(network.getProviderSegID(), install);
                break;
            case FLAT:
                // TODO: need to find a way to block flat typed network
                break;
            default:
                break;
        }
    }

    private void setNetworkBlockRulesForVxlan(String segmentId, boolean install) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchTunnelId(Long.valueOf(segmentId))
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .drop()
                .build();

        osNodeService.completeNodes(COMPUTE)
                .forEach(osNode ->
                    osFlowRuleService.setRule(
                        appId,
                        osNode.intgBridge(),
                        selector,
                        treatment,
                        PRIORITY_ADMIN_RULE,
                        ACL_TABLE,
                        install)
                );
    }

    private void setNetworkBlockRulesForVlan(String segmentId, boolean install) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchTunnelId(Long.valueOf(segmentId))
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .drop()
                .build();

        osNodeService.completeNodes(COMPUTE)
                .forEach(osNode ->
                    osFlowRuleService.setRule(
                        appId,
                        osNode.intgBridge(),
                        selector,
                        treatment,
                        PRIORITY_ADMIN_RULE,
                        ACL_TABLE,
                        install)
                );
    }

    private void setPortBlockRules(InstancePort instPort, boolean install) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchInPort(instPort.portNumber())
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .drop()
                .build();

        osFlowRuleService.setRule(
                appId,
                instPort.deviceId(),
                selector,
                treatment,
                PRIORITY_ADMIN_RULE,
                VTAG_TABLE,
                install);
    }

    /**
     * Obtains the VLAN ID from the given instance port.
     *
     * @param instPort instance port object
     * @return VLAN ID
     */
    private VlanId getVlanId(InstancePort instPort) {
        Network osNet = osNetworkService.network(instPort.networkId());

        if (osNet == null || Strings.isNullOrEmpty(osNet.getProviderSegID())) {
            final String error =
                    String.format(ERR_SET_FLOWS_VNI,
                        instPort, osNet == null ? "<none>" : osNet.getName());
            throw new IllegalStateException(error);
        }

        return VlanId.vlanId(osNet.getProviderSegID());
    }

    /**
     * Obtains the VNI from the given instance port.
     *
     * @param instPort instance port object
     * @return VXLAN Network Identifier (VNI)
     */
    private Long getVni(InstancePort instPort) {
        Network osNet = osNetworkService.network(instPort.networkId());
        if (osNet == null || Strings.isNullOrEmpty(osNet.getProviderSegID())) {
            final String error =
                    String.format(ERR_SET_FLOWS_VNI,
                        instPort, osNet == null ? "<none>" : osNet.getName());
            throw new IllegalStateException(error);
        }
        return Long.valueOf(osNet.getProviderSegID());
    }

    private String getArpMode() {
        Set<ConfigProperty> properties =
                configService.getProperties(OpenstackSwitchingArpHandler.class.getName());
        return getPropertyValue(properties, ARP_MODE);
    }

    /**
     * An internal instance port listener which listens the port events generated
     * from VM. The corresponding L2 forwarding rules will be generated and
     * inserted to integration bridge only if a new VM port is detected. If the
     * existing detected VM port is removed due to VM purge, we will remove the
     * corresponding L2 forwarding to as well for the sake of resource saving.
     */
    private class InternalInstancePortListener implements InstancePortListener {

        private boolean isRelevantHelper(InstancePortEvent event) {
            return mastershipService.isLocalMaster(event.subject().deviceId());
        }

        @Override
        public void event(InstancePortEvent event) {
            InstancePort instPort = event.subject();

            switch (event.type()) {
                case OPENSTACK_INSTANCE_PORT_DETECTED:
                case OPENSTACK_INSTANCE_PORT_UPDATED:
                case OPENSTACK_INSTANCE_MIGRATION_STARTED:
                case OPENSTACK_INSTANCE_RESTARTED:

                    if (event.type() == OPENSTACK_INSTANCE_MIGRATION_STARTED) {
                        log.info("SwitchingHandler: Migration started at MAC:{} IP:{}",
                                                        instPort.macAddress(),
                                                        instPort.ipAddress());
                    } else {
                        log.info("SwitchingHandler: Instance port detected MAC:{} IP:{}",
                                                        instPort.macAddress(),
                                                        instPort.ipAddress());
                    }

                    eventExecutor.execute(() -> {

                        if (!isRelevantHelper(event)) {
                            return;
                        }

                        instPortDetected(instPort);

                        Port osPort = osNetworkService.port(instPort.portId());

                        if (osPort != null) {
                            setPortBlockRules(instPort, !osPort.isAdminStateUp());
                        }
                    });

                    break;
                case OPENSTACK_INSTANCE_TERMINATED:
                    log.info("SwitchingHandler: Instance port terminated MAC:{} IP:{}",
                                                        instPort.macAddress(),
                                                        instPort.ipAddress());
                    eventExecutor.execute(() -> {

                        if (!isRelevantHelper(event)) {
                            return;
                        }

                        removeVportRules(instPort);
                    });

                    break;
                case OPENSTACK_INSTANCE_PORT_VANISHED:
                    log.info("SwitchingHandler: Instance port vanished MAC:{} IP:{}",
                                                        instPort.macAddress(),
                                                        instPort.ipAddress());

                    eventExecutor.execute(() -> {

                        if (!isRelevantHelper(event)) {
                            return;
                        }

                        instPortRemoved(instPort);

                        Port osPort = osNetworkService.port(instPort.portId());

                        if (osPort != null) {
                            setPortBlockRules(instPort, false);
                        }
                    });

                    break;
                case OPENSTACK_INSTANCE_MIGRATION_ENDED:
                    log.info("SwitchingHandler: Migration finished for MAC:{} IP:{}",
                                                        instPort.macAddress(),
                                                        instPort.ipAddress());

                    InstancePort revisedInstPort = swapStaleLocation(instPort);
                    eventExecutor.execute(() -> {

                        if (!isRelevantHelper(event)) {
                            return;
                        }

                        removeVportRules(revisedInstPort);
                    });

                    break;
                default:
                    break;
            }
        }

        private void instPortDetected(InstancePort instPort) {
            setNetworkRules(instPort, true);
            // TODO add something else if needed
        }

        private void instPortRemoved(InstancePort instPort) {
            setNetworkRules(instPort, false);
            // TODO add something else if needed
        }
    }

    private class InternalOpenstackNetworkListener implements OpenstackNetworkListener {

        @Override
        public boolean isRelevant(OpenstackNetworkEvent event) {
            return event.subject() != null && event.port() != null;
        }

        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(OpenstackNetworkEvent event) {

            boolean isNwAdminStateUp = event.subject().isAdminStateUp();
            boolean isPortAdminStateUp = event.port().isAdminStateUp();

            String portId = event.port().getId();

            switch (event.type()) {
                case OPENSTACK_NETWORK_CREATED:
                case OPENSTACK_NETWORK_UPDATED:
                    eventExecutor.execute(() -> {

                        if (!isRelevantHelper()) {
                            return;
                        }

                        setNetworkBlockRules(event.subject(), !isNwAdminStateUp);
                    });
                    break;
                case OPENSTACK_NETWORK_REMOVED:
                    eventExecutor.execute(() -> {

                        if (!isRelevantHelper()) {
                            return;
                        }

                        setNetworkBlockRules(event.subject(), false);
                    });
                    break;
                case OPENSTACK_PORT_CREATED:
                case OPENSTACK_PORT_UPDATED:
                    eventExecutor.execute(() -> {

                        if (!isRelevantHelper()) {
                            return;
                        }

                        InstancePort instPort = instancePortService.instancePort(portId);
                        if (instPort != null) {
                            setPortBlockRules(instPort, !isPortAdminStateUp);
                        }
                    });
                    break;
                case OPENSTACK_PORT_REMOVED:
                    eventExecutor.execute(() -> {

                        if (!isRelevantHelper()) {
                            return;
                        }

                        InstancePort instPort = instancePortService.instancePort(portId);
                        if (instPort != null) {
                            setPortBlockRules(instPort, false);
                        }
                    });
                    break;
                default:
                    break;
            }
        }
    }
}
