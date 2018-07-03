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
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
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
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.openstacknetworking.api.InstancePort;
import org.onosproject.openstacknetworking.api.InstancePortEvent;
import org.onosproject.openstacknetworking.api.InstancePortListener;
import org.onosproject.openstacknetworking.api.InstancePortService;
import org.onosproject.openstacknetworking.api.OpenstackFlowRuleService;
import org.onosproject.openstacknetworking.api.OpenstackNetworkService;
import org.onosproject.openstacknetworking.api.OpenstackSecurityGroupService;
import org.onosproject.openstacknetworking.util.RulePopulatorUtil;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeService;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.NetworkType;
import org.openstack4j.model.network.Port;
import org.slf4j.Logger;

import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.openstacknetworking.api.Constants.ACL_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.DHCP_ARP_TABLE;
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

    private static final String ERR_SET_FLOWS_VNI = "Failed to set flows for %s: Failed to get VNI for %s";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackFlowRuleService osFlowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InstancePortService instancePortService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackNetworkService osNetworkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackNodeService osNodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackSecurityGroupService securityGroupService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler"));
    private final InstancePortListener instancePortListener = new InternalInstancePortListener();
    private ApplicationId appId;

    @Activate
    void activate() {
        appId = coreService.registerApplication(OPENSTACK_NETWORKING_APP_ID);
        instancePortService.addListener(instancePortListener);

        log.info("Started");
    }

    @Deactivate
    void deactivate() {
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
                setTunnelTagFlowRules(instPort, install);
                setForwardingRulesForVxlan(instPort, install);
                break;
            case VLAN:
                setVlanTagFlowRules(instPort, install);
                setForwardingRulesForVlan(instPort, install);
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
                setTunnelTagFlowRules(instPort, false);
                break;
            case VLAN:
                setVlanTagFlowRules(instPort, false);
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
                DHCP_ARP_TABLE,
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
                DHCP_ARP_TABLE,
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
                DHCP_ARP_TABLE,
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

    /**
     * Configures the flow rule which is for using VXLAN to tag the packet
     * based on the in_port number of a virtual instance.
     * Note that this rule will be inserted in VNI table (table 0).
     *
     * @param instPort instance port object
     * @param install install flag, add the rule if true, remove it otherwise
     */
    private void setTunnelTagFlowRules(InstancePort instPort, boolean install) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                // TODO: need to handle IPv6 in near future
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchInPort(instPort.portNumber())
                .build();

        // XXX All egress traffic needs to go through connection tracking module,
        // which might hurt its performance.
        ExtensionTreatment ctTreatment =
                RulePopulatorUtil.niciraConnTrackTreatmentBuilder(driverService, instPort.deviceId())
                        .commit(true).build();

        TrafficTreatment.Builder tb = DefaultTrafficTreatment.builder()
                .setTunnelId(getVni(instPort))
                .transition(ACL_TABLE);

        if (securityGroupService.isSecurityGroupEnabled()) {
            tb.extension(ctTreatment, instPort.deviceId());
        }

        osFlowRuleService.setRule(
                appId,
                instPort.deviceId(),
                selector,
                tb.build(),
                PRIORITY_TUNNEL_TAG_RULE,
                VTAG_TABLE,
                install);
    }

    /**
     * Configures the flow rule which is for using VLAN to tag the packet
     * based on the in_port number of a virtual instance.
     * Note that this rule will be inserted in VNI table (table 0).
     *
     * @param instPort instance port object
     * @param install install flag, add the rule if true, remove it otherwise
     */
    private void setVlanTagFlowRules(InstancePort instPort, boolean install) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                // TODO: need to handle IPv6 in near future
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchInPort(instPort.portNumber())
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .pushVlan()
                .setVlanId(getVlanId(instPort))
                .transition(ACL_TABLE)
                .build();

        osFlowRuleService.setRule(
                appId,
                instPort.deviceId(),
                selector,
                treatment,
                PRIORITY_TUNNEL_TAG_RULE,
                VTAG_TABLE,
                install);
    }

    private void setNetworkAdminRules(Network network, boolean install) {
        TrafficSelector selector;
        if (network.getNetworkType() == NetworkType.VXLAN) {

            selector = DefaultTrafficSelector.builder()
                    .matchTunnelId(Long.valueOf(network.getProviderSegID()))
                    .build();
        } else {
            selector = DefaultTrafficSelector.builder()
                    .matchVlanId(VlanId.vlanId(network.getProviderSegID()))
                    .build();
        }

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .drop()
                .build();

        osNodeService.completeNodes().stream()
                .filter(osNode -> osNode.type() == COMPUTE)
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

    // TODO: need to be purged sooner or later
    private void setPortAdminRules(Port port, boolean install) {
        InstancePort instancePort =
                instancePortService.instancePort(MacAddress.valueOf(port.getMacAddress()));
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchInPort(instancePort.portNumber())
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .drop()
                .build();

        osFlowRuleService.setRule(
                appId,
                instancePort.deviceId(),
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

    /**
     * An internal instance port listener which listens the port events generated
     * from VM. The corresponding L2 forwarding rules will be generated and
     * inserted to integration bridge only if a new VM port is detected. If the
     * existing detected VM port is removed due to VM purge, we will remove the
     * corresponding L2 forwarding to as well for the sake of resource saving.
     */
    private class InternalInstancePortListener implements InstancePortListener {

        @Override
        public boolean isRelevant(InstancePortEvent event) {
            InstancePort instPort = event.subject();
            return mastershipService.isLocalMaster(instPort.deviceId());
        }

        @Override
        public void event(InstancePortEvent event) {
            InstancePort instPort = event.subject();

            switch (event.type()) {
                case OPENSTACK_INSTANCE_PORT_UPDATED:
                case OPENSTACK_INSTANCE_PORT_DETECTED:
                    log.info("Instance port detected MAC:{} IP:{}",
                                                        instPort.macAddress(),
                                                        instPort.ipAddress());
                    eventExecutor.execute(() ->
                        instPortDetected(instPort)
                    );

                    break;
                case OPENSTACK_INSTANCE_PORT_VANISHED:
                    log.info("Instance port vanished MAC:{} IP:{}",
                                                        instPort.macAddress(),
                                                        instPort.ipAddress());
                    eventExecutor.execute(() ->
                        instPortRemoved(instPort)
                    );

                    break;

                // we do not consider MIGRATION_STARTED case, because the rules
                // will be installed to corresponding switches at
                // OPENSTACK_INSTANCE_PORT_UPDATED phase

                // TODO: we may need to consider to refactor the VM migration
                // event detection logic for better code readability
                case OPENSTACK_INSTANCE_MIGRATION_ENDED:
                    log.info("Instance port vanished MAC:{} IP:{}, " +
                                 "due to VM migration", instPort.macAddress(),
                                                        instPort.ipAddress());
                    eventExecutor.execute(() ->
                        removeVportRules(instPort)
                    );
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
}
