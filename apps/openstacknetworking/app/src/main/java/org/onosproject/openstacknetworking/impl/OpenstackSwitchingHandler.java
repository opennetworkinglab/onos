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
import org.onosproject.openstacknetworking.api.OpenstackNetwork.Type;
import org.onosproject.openstacknetworking.api.OpenstackNetworkEvent;
import org.onosproject.openstacknetworking.api.OpenstackNetworkListener;
import org.onosproject.openstacknetworking.api.OpenstackNetworkService;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeService;
import org.openstack4j.model.network.Network;
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
import static org.onosproject.openstacknetworking.api.Constants.ACL_EGRESS_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.ARP_BROADCAST_MODE;
import static org.onosproject.openstacknetworking.api.Constants.ARP_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.FORWARDING_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.OPENSTACK_NETWORKING_APP_ID;
import static org.onosproject.openstacknetworking.api.Constants.PRE_FLAT_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_ADMIN_RULE;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_SWITCHING_RULE;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_TUNNEL_TAG_RULE;
import static org.onosproject.openstacknetworking.api.Constants.VTAG_TABLE;
import static org.onosproject.openstacknetworking.api.InstancePortEvent.Type.OPENSTACK_INSTANCE_MIGRATION_STARTED;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.deriveResourceName;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.getPropertyValue;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.swapStaleLocation;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.tunnelPortNumByNetId;
import static org.onosproject.openstacknetworking.util.RulePopulatorUtil.buildExtension;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.COMPUTE;
import static org.slf4j.LoggerFactory.getLogger;


/**
 * Populates switching flow rules on OVS for the basic connectivity among the
 * virtual instances in the same network.
 */
@Component(immediate = true)
public class OpenstackSwitchingHandler {

    private final Logger log = getLogger(getClass());

    private static final String ARP_MODE = "arpMode";
    private static final String ERR_SET_FLOWS_VNI = "Failed to set flows for " +
                                                    "%s: Failed to get VNI for %s";
    private static final String STR_NONE = "<none>";

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
    protected void activate() {
        appId = coreService.registerApplication(OPENSTACK_NETWORKING_APP_ID);
        localNodeId = clusterService.getLocalNode().id();
        instancePortService.addListener(instancePortListener);
        osNetworkService.addListener(osNetworkListener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        osNetworkService.removeListener(osNetworkListener);
        instancePortService.removeListener(instancePortListener);
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    private void setJumpRulesForFlat(InstancePort port, boolean install) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchInPort(port.portNumber())
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .transition(PRE_FLAT_TABLE)
                .build();

        osFlowRuleService.setRule(
                appId,
                port.deviceId(),
                selector,
                treatment,
                PRIORITY_SWITCHING_RULE,
                VTAG_TABLE,
                install);
    }

    /**
     * Configures the flow rules which are used for L2 packet switching.
     * Note that these rules will be inserted in switching table (table 5).
     *
     * @param instPort instance port object
     * @param install install flag, add the rule if true, remove it otherwise
     */
    private void setForwardingRulesForTunnel(InstancePort instPort, boolean install) {
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
                    PortNumber portNum = tunnelPortNumByNetId(instPort.networkId(),
                            osNetworkService, remoteNode);
                    TrafficTreatment treatmentToRemote = DefaultTrafficTreatment.builder()
                            .extension(buildExtension(
                                    deviceService,
                                    remoteNode.intgBridge(),
                                    localNode.dataIp().getIp4Address()),
                                    remoteNode.intgBridge())
                            .setOutput(portNum)
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
     * Configures the flow rule which is for using VXLAN/GRE to tag the packet
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
            tBuilder.transition(ACL_EGRESS_TABLE);
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
            tBuilder.transition(ACL_EGRESS_TABLE);
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

        Type type = osNetworkService.networkType(network.getId());

        // TODO: we block a network traffic by referring to segment ID for now
        // we might need to find a better way to block the traffic of a network
        // in case the segment ID is overlapped in different types network (VXLAN, GRE, VLAN)
        switch (type) {
            case VXLAN:
            case GRE:
            case GENEVE:
                setNetworkBlockRulesForTunnel(network.getProviderSegID(), install);
                break;
            case VLAN:
                setNetworkBlockRulesForVlan(network.getProviderSegID(), install);
                break;
            default:
                break;
        }
    }

    private void setNetworkBlockRulesForTunnel(String segmentId, boolean install) {
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
                        ACL_EGRESS_TABLE,
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
                        ACL_EGRESS_TABLE,
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
                        instPort, osNet == null ? STR_NONE : deriveResourceName(osNet));
            throw new IllegalStateException(error);
        }

        return VlanId.vlanId(osNet.getProviderSegID());
    }

    /**
     * Obtains the VNI from the given instance port.
     *
     * @param instPort instance port object
     * @return Virtual Network Identifier (VNI)
     */
    private Long getVni(InstancePort instPort) {
        Network osNet = osNetworkService.network(instPort.networkId());
        if (osNet == null || Strings.isNullOrEmpty(osNet.getProviderSegID())) {
            final String error =
                    String.format(ERR_SET_FLOWS_VNI,
                        instPort, osNet == null ? STR_NONE : deriveResourceName(osNet));
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
                    eventExecutor.execute(() ->
                                    processInstanceDetection(event, instPort));
                    break;
                case OPENSTACK_INSTANCE_TERMINATED:
                    eventExecutor.execute(() ->
                                    processInstanceTermination(event, instPort));
                    break;
                case OPENSTACK_INSTANCE_PORT_VANISHED:
                    eventExecutor.execute(() ->
                                    processInstanceRemoval(event, instPort));
                    break;
                case OPENSTACK_INSTANCE_MIGRATION_ENDED:
                    eventExecutor.execute(() ->
                                    processInstanceMigrationEnd(event, instPort));
                    break;
                default:
                    break;
            }
        }

        private void processInstanceDetection(InstancePortEvent event,
                                              InstancePort instPort) {
            if (!isRelevantHelper(event)) {
                return;
            }

            if (event.type() == OPENSTACK_INSTANCE_MIGRATION_STARTED) {
                log.info("SwitchingHandler: Migration started at MAC:{} IP:{}",
                                                        instPort.macAddress(),
                                                        instPort.ipAddress());
            } else {
                log.info("SwitchingHandler: Instance port detected MAC:{} IP:{}",
                                                        instPort.macAddress(),
                                                        instPort.ipAddress());
            }

            instPortDetected(instPort);

            Port osPort = osNetworkService.port(instPort.portId());

            if (osPort != null) {
                setPortBlockRules(instPort, !osPort.isAdminStateUp());
            }
        }

        private void processInstanceTermination(InstancePortEvent event,
                                                InstancePort instPort) {
            if (!isRelevantHelper(event)) {
                return;
            }

            log.info("SwitchingHandler: Instance port terminated MAC:{} IP:{}",
                                                        instPort.macAddress(),
                                                        instPort.ipAddress());

            removeVportRules(instPort);
        }

        private void processInstanceRemoval(InstancePortEvent event,
                                            InstancePort instPort) {
            if (!isRelevantHelper(event)) {
                return;
            }

            log.info("SwitchingHandler: Instance port vanished MAC:{} IP:{}",
                                                        instPort.macAddress(),
                                                        instPort.ipAddress());

            instPortRemoved(instPort);

            Port osPort = osNetworkService.port(instPort.portId());

            if (osPort != null) {
                setPortBlockRules(instPort, false);
            }
        }

        private void processInstanceMigrationEnd(InstancePortEvent event,
                                                 InstancePort instPort) {
            if (!isRelevantHelper(event)) {
                return;
            }

            log.info("SwitchingHandler: Migration finished for MAC:{} IP:{}",
                                                        instPort.macAddress(),
                                                        instPort.ipAddress());

            InstancePort revisedInstPort = swapStaleLocation(instPort);

            removeVportRules(revisedInstPort);
        }

        /**
         * Configures L2 forwarding rules.
         * Currently, SONA supports Flat, VXLAN, GRE, GENEVE and VLAN modes.
         *
         * @param instPort instance port object
         * @param install install flag, add the rule if true, remove it otherwise
         */
        private void setNetworkRules(InstancePort instPort, boolean install) {
            Type type = osNetworkService.networkType(instPort.networkId());

            switch (type) {
                case VXLAN:
                case GRE:
                case GENEVE:
                    setNetworkRulesForTunnel(instPort, install);
                    break;
                case VLAN:
                    setNetworkRulesForVlan(instPort, install);
                    break;
                case FLAT:
                    setNetworkRulesForFlat(instPort, install);
                    break;
                default:
                    log.warn("Unsupported network tunnel type {}", type.name());
                    break;
            }
        }

        private void setNetworkRulesForTunnel(InstancePort instPort, boolean install) {
            setTunnelTagIpFlowRules(instPort, install);
            setForwardingRulesForTunnel(instPort, install);

            if (ARP_BROADCAST_MODE.equals(getArpMode())) {
                setTunnelTagArpFlowRules(instPort, install);
            }
        }

        private void setNetworkRulesForVlan(InstancePort instPort, boolean install) {
            setVlanTagIpFlowRules(instPort, install);
            setForwardingRulesForVlan(instPort, install);

            if (ARP_BROADCAST_MODE.equals(getArpMode())) {
                setVlanTagArpFlowRules(instPort, install);
            }
        }

        private void setNetworkRulesForFlat(InstancePort instPort, boolean install) {
            setJumpRulesForFlat(instPort, install);
        }

        /**
         * Removes virtual port related flow rules.
         *
         * @param instPort instance port
         */
        private void removeVportRules(InstancePort instPort) {
            Type type = osNetworkService.networkType(instPort.networkId());

            switch (type) {
                case VXLAN:
                case GRE:
                case GENEVE:
                    removeVportRulesForTunnel(instPort);
                    break;
                case VLAN:
                    removeVportRulesForVlan(instPort);
                    break;
                case FLAT:
                    removeVportRulesForFlat(instPort);
                    break;
                default:
                    log.warn("Unsupported network type {}", type.name());
                    break;
            }
        }

        private void removeVportRulesForTunnel(InstancePort instPort) {
            setTunnelTagIpFlowRules(instPort, false);

            if (ARP_BROADCAST_MODE.equals(getArpMode())) {
                setTunnelTagArpFlowRules(instPort, false);
            }
        }

        private void removeVportRulesForVlan(InstancePort instPort) {
            setVlanTagIpFlowRules(instPort, false);

            if (ARP_BROADCAST_MODE.equals(getArpMode())) {
                setVlanTagArpFlowRules(instPort, false);
            }
        }

        private void removeVportRulesForFlat(InstancePort instPort) {
            setJumpRulesForFlat(instPort, false);
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
            switch (event.type()) {
                case OPENSTACK_NETWORK_CREATED:
                case OPENSTACK_NETWORK_UPDATED:
                    eventExecutor.execute(() -> processNetworkAddition(event));
                    break;
                case OPENSTACK_NETWORK_PRE_REMOVED:
                    eventExecutor.execute(() -> processNetworkRemoval(event));
                    break;
                case OPENSTACK_PORT_CREATED:
                case OPENSTACK_PORT_UPDATED:
                    eventExecutor.execute(() -> processPortAddition(event));
                    break;
                case OPENSTACK_PORT_REMOVED:
                    eventExecutor.execute(() -> processPortRemoval(event));
                    break;
                default:
                    break;
            }
        }

        private void processNetworkAddition(OpenstackNetworkEvent event) {
            if (!isRelevantHelper()) {
                return;
            }

            boolean isNwAdminStateUp = event.subject().isAdminStateUp();
            setNetworkBlockRules(event.subject(), !isNwAdminStateUp);
        }

        private void processNetworkRemoval(OpenstackNetworkEvent event) {
            if (!isRelevantHelper()) {
                return;
            }

            setNetworkBlockRules(event.subject(), false);
        }

        private void processPortAddition(OpenstackNetworkEvent event) {
            if (!isRelevantHelper()) {
                return;
            }

            boolean isPortAdminStateUp = event.port().isAdminStateUp();
            String portId = event.port().getId();
            InstancePort instPort = instancePortService.instancePort(portId);
            if (instPort != null) {
                setPortBlockRules(instPort, !isPortAdminStateUp);
            }
        }

        private void processPortRemoval(OpenstackNetworkEvent event) {
            if (!isRelevantHelper()) {
                return;
            }

            String portId = event.port().getId();
            InstancePort instPort = instancePortService.instancePort(portId);
            if (instPort != null) {
                setPortBlockRules(instPort, false);
            }
        }
    }
}
