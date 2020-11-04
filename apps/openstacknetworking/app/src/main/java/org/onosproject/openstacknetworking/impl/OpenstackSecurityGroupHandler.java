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

package org.onosproject.openstacknetworking.impl;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.TpPort;
import org.onlab.packet.VlanId;
import org.onlab.util.KryoNamespace;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cfg.ConfigProperty;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.ExtensionSelector;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.openstacknetworking.api.InstancePort;
import org.onosproject.openstacknetworking.api.InstancePortAdminService;
import org.onosproject.openstacknetworking.api.InstancePortEvent;
import org.onosproject.openstacknetworking.api.InstancePortListener;
import org.onosproject.openstacknetworking.api.OpenstackFlowRuleService;
import org.onosproject.openstacknetworking.api.OpenstackNetwork.Type;
import org.onosproject.openstacknetworking.api.OpenstackNetworkEvent;
import org.onosproject.openstacknetworking.api.OpenstackNetworkListener;
import org.onosproject.openstacknetworking.api.OpenstackNetworkService;
import org.onosproject.openstacknetworking.api.OpenstackSecurityGroupEvent;
import org.onosproject.openstacknetworking.api.OpenstackSecurityGroupListener;
import org.onosproject.openstacknetworking.api.OpenstackSecurityGroupService;
import org.onosproject.openstacknetworking.util.RulePopulatorUtil;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeEvent;
import org.onosproject.openstacknode.api.OpenstackNodeListener;
import org.onosproject.openstacknode.api.OpenstackNodeService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.SecurityGroup;
import org.openstack4j.model.network.SecurityGroupRule;
import org.openstack4j.model.network.State;
import org.openstack4j.openstack.networking.domain.NeutronAllowedAddressPair;
import org.openstack4j.openstack.networking.domain.NeutronExtraDhcpOptCreate;
import org.openstack4j.openstack.networking.domain.NeutronIP;
import org.openstack4j.openstack.networking.domain.NeutronPort;
import org.openstack4j.openstack.networking.domain.NeutronSecurityGroupRule;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.openstacknetworking.api.Constants.ACL_EGRESS_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.ACL_INGRESS_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.ACL_RECIRC_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.CT_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.ERROR_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.JUMP_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.OPENSTACK_NETWORKING_APP_ID;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_ACL_INGRESS_RULE;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_ACL_RULE;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_CT_DROP_RULE;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_CT_HOOK_RULE;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_CT_RULE;
import static org.onosproject.openstacknetworking.api.OpenstackNetwork.Type.GENEVE;
import static org.onosproject.openstacknetworking.api.OpenstackNetwork.Type.GRE;
import static org.onosproject.openstacknetworking.api.OpenstackNetwork.Type.VLAN;
import static org.onosproject.openstacknetworking.api.OpenstackNetwork.Type.VXLAN;
import static org.onosproject.openstacknetworking.api.OpenstackNetworkEvent.Type.OPENSTACK_PORT_PRE_REMOVE;
import static org.onosproject.openstacknetworking.impl.OsgiPropertyConstants.USE_SECURITY_GROUP;
import static org.onosproject.openstacknetworking.impl.OsgiPropertyConstants.USE_SECURITY_GROUP_DEFAULT;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.getPropertyValueAsBoolean;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.swapStaleLocation;
import static org.onosproject.openstacknetworking.util.RulePopulatorUtil.buildPortRangeMatches;
import static org.onosproject.openstacknetworking.util.RulePopulatorUtil.computeCtMaskFlag;
import static org.onosproject.openstacknetworking.util.RulePopulatorUtil.computeCtStateFlag;
import static org.onosproject.openstacknetworking.util.RulePopulatorUtil.niciraConnTrackTreatmentBuilder;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.COMPUTE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Populates flow rules to handle OpenStack SecurityGroups.
 */
@Component(
    immediate = true,
    property = {
        USE_SECURITY_GROUP + ":Boolean=" + USE_SECURITY_GROUP_DEFAULT
    }
)
public class OpenstackSecurityGroupHandler {

    private final Logger log = getLogger(getClass());

    private static final int VM_IP_PREFIX = 32;

    private static final String STR_NULL = "null";

    /** Apply OpenStack security group rule for VM traffic. */
    private boolean useSecurityGroup = USE_SECURITY_GROUP_DEFAULT;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected InstancePortAdminService instancePortService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackNetworkService osNetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackSecurityGroupService securityGroupService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackFlowRuleService osFlowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackNodeService osNodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    private static final KryoNamespace SERIALIZER_PORT = KryoNamespace.newBuilder()
            .register(KryoNamespaces.API)
            .register(Port.class)
            .register(NeutronPort.class)
            .register(NeutronIP.class)
            .register(State.class)
            .register(NeutronAllowedAddressPair.class)
            .register(NeutronExtraDhcpOptCreate.class)
            .register(LinkedHashMap.class)
            .build();

    private final InstancePortListener instancePortListener =
                                            new InternalInstancePortListener();
    private final OpenstackNetworkListener osNetworkListener =
                                            new InternalOpenstackNetworkListener();
    private final OpenstackNetworkListener osPortListener =
                                            new InternalOpenstackPortListener();
    private final OpenstackSecurityGroupListener securityGroupListener =
                                            new InternalSecurityGroupListener();
    private final OpenstackNodeListener osNodeListener = new InternalNodeListener();

    private ConsistentMap<String, Port> removedOsPortStore;

    private ApplicationId appId;
    private NodeId localNodeId;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler"));

    private static final String PROTO_ICMP = "ICMP";
    private static final String PROTO_ICMP_NUM = "1";
    private static final String PROTO_TCP = "TCP";
    private static final String PROTO_TCP_NUM = "6";
    private static final String PROTO_UDP = "UDP";
    private static final String PROTO_UDP_NUM = "17";
    private static final String PROTO_SCTP = "SCTP";
    private static final String PROTO_SCTP_NUM = "132";
    private static final byte PROTOCOL_SCTP = (byte) 0x84;
    private static final String PROTO_ANY = "ANY";
    private static final String PROTO_ANY_NUM = "0";
    private static final String ETHTYPE_IPV4 = "IPV4";
    private static final String EGRESS = "EGRESS";
    private static final String INGRESS = "INGRESS";
    private static final IpPrefix IP_PREFIX_ANY = Ip4Prefix.valueOf("0.0.0.0/0");

    // We expose pipeline structure to SONA application considering removing pipeline soon.
    private static final int GOTO_CONNTRACK_TABLE = CT_TABLE;
    private static final int GOTO_JUMP_TABLE = JUMP_TABLE;

    private static final int CT_COMMIT = 0;
    private static final int CT_NO_COMMIT = 1;
    private static final short CT_NO_RECIRC = -1;

    private static final int ACTION_NONE = 0;
    private static final int ACTION_DROP = -1;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(OPENSTACK_NETWORKING_APP_ID);
        localNodeId = clusterService.getLocalNode().id();
        instancePortService.addListener(instancePortListener);
        securityGroupService.addListener(securityGroupListener);
        osNetService.addListener(osPortListener);
        osNetService.addListener(osNetworkListener);
        configService.registerProperties(getClass());
        osNodeService.addListener(osNodeListener);

        removedOsPortStore = storageService.<String, Port>consistentMapBuilder()
                .withSerializer(Serializer.using(SERIALIZER_PORT))
                .withName("openstack-removed-portstore")
                .withApplicationId(appId)
                .build();

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        instancePortService.removeListener(instancePortListener);
        securityGroupService.removeListener(securityGroupListener);
        osNetService.removeListener(osNetworkListener);
        osNetService.removeListener(osPortListener);
        configService.unregisterProperties(getClass(), false);
        osNodeService.removeListener(osNodeListener);
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    @Modified
    protected void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        Boolean flag;

        flag = Tools.isPropertyEnabled(properties, USE_SECURITY_GROUP);
        if (flag == null) {
            log.info("useSecurityGroup is not configured, " +
                    "using current value of {}", useSecurityGroup);
        } else {
            useSecurityGroup = flag;
            log.info("Configured. useSecurityGroup is {}",
                    useSecurityGroup ? "enabled" : "disabled");
        }

        securityGroupService.setSecurityGroupEnabled(useSecurityGroup);
        resetSecurityGroupRules();
    }

    private boolean getUseSecurityGroupFlag() {
        Set<ConfigProperty> properties =
                configService.getProperties(getClass().getName());
        return getPropertyValueAsBoolean(properties, USE_SECURITY_GROUP);
    }

    private void initializeConnTrackTable(DeviceId deviceId, boolean install) {

        //table=1,ip,ct_state=-trk, actions=ct(table:2)
        long ctState = computeCtStateFlag(false, false, false);
        long ctMask = computeCtMaskFlag(true, false, false);
        setConnTrackRule(deviceId, ctState, ctMask, CT_NO_COMMIT, (short) GOTO_CONNTRACK_TABLE,
                ACTION_NONE, PRIORITY_CT_HOOK_RULE, install);

        //table=2,ip,nw_dst=10.10.0.2,ct_state=+trk+est,action=goto_table:3
        ctState = computeCtStateFlag(true, false, true);
        ctMask = computeCtMaskFlag(true, false, true);
        setConnTrackRule(deviceId, ctState, ctMask, CT_NO_COMMIT, CT_NO_RECIRC,
                GOTO_JUMP_TABLE, PRIORITY_CT_RULE, install);

        //table=2,ip,nw_dst=10.10.0.2,ct_state=+trk+new,action=drop
        ctState = computeCtStateFlag(true, true, false);
        ctMask = computeCtMaskFlag(true, true, false);
        setConnTrackRule(deviceId, ctState, ctMask, CT_NO_COMMIT, CT_NO_RECIRC,
                ACTION_DROP, PRIORITY_CT_DROP_RULE, install);
    }

    private void initializeAclTable(DeviceId deviceId, boolean install) {

        ExtensionTreatment ctTreatment =
                niciraConnTrackTreatmentBuilder(driverService, deviceId)
                        .commit(true)
                        .build();

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        sBuilder.matchEthType(Ethernet.TYPE_IPV4);

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
        tBuilder.extension(ctTreatment, deviceId)
                .transition(JUMP_TABLE);

        osFlowRuleService.setRule(appId,
                deviceId,
                sBuilder.build(),
                tBuilder.build(),
                PRIORITY_ACL_INGRESS_RULE,
                ACL_RECIRC_TABLE,
                install);
    }

    private void initializeIngressTable(DeviceId deviceId, boolean install) {
        if (install) {
            osFlowRuleService.setUpTableMissEntry(deviceId, ACL_INGRESS_TABLE);
        } else {
            osFlowRuleService.connectTables(deviceId, ACL_INGRESS_TABLE, JUMP_TABLE);
        }
    }

    private void updateSecurityGroupRule(InstancePort instPort, Port port,
                                         SecurityGroupRule sgRule, boolean install) {

        if (instPort == null || port == null || sgRule == null) {
            return;
        }

        if (sgRule.getRemoteGroupId() != null && !sgRule.getRemoteGroupId().isEmpty()) {
            getRemoteInstPorts(port, sgRule.getRemoteGroupId(), install)
                    .forEach(rInstPort -> {
                        populateSecurityGroupRule(sgRule, instPort,
                                rInstPort.ipAddress().toIpPrefix(), install);
                        populateSecurityGroupRule(sgRule, rInstPort,
                                instPort.ipAddress().toIpPrefix(), install);

                        SecurityGroupRule rSgRule =
                                new NeutronSecurityGroupRule
                                        .SecurityGroupRuleConcreteBuilder()
                                        .from(sgRule)
                                        .direction(sgRule.getDirection()
                                                .equalsIgnoreCase(EGRESS) ? INGRESS : EGRESS)
                                        .build();
                        populateSecurityGroupRule(rSgRule, instPort,
                                rInstPort.ipAddress().toIpPrefix(), install);
                        populateSecurityGroupRule(rSgRule, rInstPort,
                                instPort.ipAddress().toIpPrefix(), install);
                    });
        } else {
            populateSecurityGroupRule(sgRule, instPort,
                    sgRule.getRemoteIpPrefix() == null ? IP_PREFIX_ANY :
                            IpPrefix.valueOf(sgRule.getRemoteIpPrefix()), install);
        }
    }

    private boolean checkProtocol(String protocol) {
        if (protocol == null) {
            log.debug("No protocol was specified, use default IP(v4/v6) protocol.");
            return true;
        } else {
            String protocolUpper = protocol.toUpperCase();
            if (protocolUpper.equals(PROTO_TCP) ||
                    protocolUpper.equals(PROTO_UDP) ||
                    protocolUpper.equals(PROTO_ICMP) ||
                    protocolUpper.equals(PROTO_SCTP) ||
                    protocolUpper.equals(PROTO_ANY) ||
                    protocol.equals(PROTO_TCP_NUM) ||
                    protocol.equals(PROTO_UDP_NUM) ||
                    protocol.equals(PROTO_ICMP_NUM) ||
                    protocol.equals(PROTO_SCTP_NUM) ||
                    protocol.equals(PROTO_ANY_NUM)) {
                return true;
            } else {
                log.error("Unsupported protocol {}, we only support " +
                        "TCP/UDP/ICMP/SCTP protocols.", protocol);
                return false;
            }
        }
    }

    private void populateSecurityGroupRule(SecurityGroupRule sgRule,
                                           InstancePort instPort,
                                           IpPrefix remoteIp,
                                           boolean install) {
        if (!checkProtocol(sgRule.getProtocol())) {
            return;
        }

        Set<TrafficSelector> selectors = buildSelectors(sgRule,
                        Ip4Address.valueOf(instPort.ipAddress().toInetAddress()),
                                    remoteIp, instPort.networkId());
        if (selectors == null || selectors.isEmpty()) {
            return;
        }

        // if the device is not available we do not perform any action
        if (instPort.deviceId() == null || !deviceService.isAvailable(instPort.deviceId())) {
            return;
        }

        // in case a port is bound to multiple security groups, we do NOT remove
        // egress rules unless all security groups bound to the port to be removed
        Port osPort = osNetService.port(instPort.portId());
        if (!install && osPort != null && sgRule.getDirection().equalsIgnoreCase(EGRESS)) {
            List<String> sgIds = osPort.getSecurityGroups();
            if (!sgIds.contains(sgRule.getSecurityGroupId()) && !sgIds.isEmpty()) {
                return;
            }
        }

        // XXX All egress traffic needs to go through connection tracking module,
        // which might hurt its performance.
        ExtensionTreatment ctTreatment =
                niciraConnTrackTreatmentBuilder(driverService, instPort.deviceId())
                        .commit(true)
                        .build();

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        int aclTable;
        if (sgRule.getDirection().equalsIgnoreCase(EGRESS)) {
            aclTable = ACL_EGRESS_TABLE;
            tBuilder.transition(ACL_RECIRC_TABLE);
        } else {
            aclTable = ACL_INGRESS_TABLE;
            tBuilder.extension(ctTreatment, instPort.deviceId())
                    .transition(JUMP_TABLE);
        }

        int finalAclTable = aclTable;
        selectors.forEach(selector -> {
            osFlowRuleService.setRule(appId,
                    instPort.deviceId(),
                    selector, tBuilder.build(),
                    PRIORITY_ACL_RULE,
                    finalAclTable,
                    install);
        });
    }

    /**
     * Sets connection tracking rule using OVS extension commands.
     * It is not so graceful, but I don't want to make it more general because
     * it is going to be used only here.
     * The following is the usage of the function.
     *
     * @param deviceId Device ID
     * @param ctState ctState: please use RulePopulatorUtil.computeCtStateFlag()
     *                to build the value
     * @param ctMask crMask: please use RulePopulatorUtil.computeCtMaskFlag()
     *               to build the value
     * @param commit CT_COMMIT for commit action, CT_NO_COMMIT otherwise
     * @param recircTable table number for recirculation after CT actions.
     *                    CT_NO_RECIRC with no recirculation
     * @param action Additional actions. ACTION_DROP, ACTION_NONE,
     *               GOTO_XXX_TABLE are supported.
     * @param priority priority value for the rule
     * @param install true for insertion, false for removal
     */
    private void setConnTrackRule(DeviceId deviceId, long ctState, long ctMask,
                                  int commit, short recircTable,
                                  int action, int priority, boolean install) {

        ExtensionSelector esCtSate = RulePopulatorUtil
                .buildCtExtensionSelector(driverService, deviceId, ctState, ctMask);
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .extension(esCtSate, deviceId)
                .matchEthType(Ethernet.TYPE_IPV4)
                .build();

        TrafficTreatment.Builder tb = DefaultTrafficTreatment.builder();

        if (commit == CT_COMMIT || recircTable > 0) {
            RulePopulatorUtil.NiciraConnTrackTreatmentBuilder natTreatmentBuilder =
                    niciraConnTrackTreatmentBuilder(driverService, deviceId);
            natTreatmentBuilder.natAction(false);
            if (commit == CT_COMMIT) {
                natTreatmentBuilder.commit(true);
            } else {
                natTreatmentBuilder.commit(false);
            }
            if (recircTable > 0) {
                natTreatmentBuilder.table(recircTable);
            }
            tb.extension(natTreatmentBuilder.build(), deviceId);
        } else if (action == ACTION_DROP) {
            tb.drop();
        }

        if (action != ACTION_NONE && action != ACTION_DROP) {
            tb.transition(action);
        }

        int tableType = ERROR_TABLE;
        if (priority == PRIORITY_CT_RULE || priority == PRIORITY_CT_DROP_RULE) {
            tableType = CT_TABLE;
        } else if (priority == PRIORITY_CT_HOOK_RULE) {
            tableType = ACL_INGRESS_TABLE;
        } else {
            log.error("Cannot an appropriate table for the conn track rule.");
        }

        osFlowRuleService.setRule(
                appId,
                deviceId,
                selector,
                tb.build(),
                priority,
                tableType,
                install);
    }

    /**
     * Returns a set of host IP addresses engaged with supplied security group ID.
     * It only searches a VM in the same tenant boundary.
     *
     * @param srcPort openstack port
     * @param sgId security group id
     * @return set of ip addresses
     */
    private Set<InstancePort> getRemoteInstPorts(Port srcPort,
                                                 String sgId, boolean install) {
        Set<InstancePort> remoteInstPorts;

        Set<Port> removedPorts = Sets.newConcurrentHashSet();

        if (!install) {
            removedPorts = new HashSet<>(removedOsPortStore.asJavaMap().values());
        }

        remoteInstPorts = Sets.union(osNetService.ports(), removedPorts).stream()
                .filter(port -> !port.getId().equals(srcPort.getId()))
                .filter(port -> port.getTenantId().equals(srcPort.getTenantId()))
                .filter(port -> port.getSecurityGroups().contains(sgId))
                .filter(port -> port.getNetworkId().equals(srcPort.getNetworkId()))
                .map(port -> instancePortService.instancePort(port.getId()))
                .filter(instPort -> instPort != null && instPort.ipAddress() != null)
                .collect(Collectors.toSet());

        return Collections.unmodifiableSet(remoteInstPorts);
    }

    private Set<TrafficSelector> buildSelectors(SecurityGroupRule sgRule,
                                                Ip4Address vmIp,
                                                IpPrefix remoteIp,
                                                String netId) {
        if (remoteIp != null && remoteIp.equals(IpPrefix.valueOf(vmIp, VM_IP_PREFIX))) {
            // do nothing if the remote IP is my IP
            return null;
        }

        Set<TrafficSelector> selectorSet = Sets.newHashSet();

        if (sgRule.getPortRangeMax() != null && sgRule.getPortRangeMin() != null &&
                sgRule.getPortRangeMin() < sgRule.getPortRangeMax()) {
            Map<TpPort, TpPort> portRangeMatchMap =
                    buildPortRangeMatches(sgRule.getPortRangeMin(),
                            sgRule.getPortRangeMax());
            portRangeMatchMap.forEach((key, value) -> {

                TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
                buildMatches(sBuilder, sgRule, vmIp, remoteIp, netId);

                if (sgRule.getProtocol().equalsIgnoreCase(PROTO_TCP) ||
                        sgRule.getProtocol().equals(PROTO_TCP_NUM)) {
                    if (sgRule.getDirection().equalsIgnoreCase(EGRESS)) {
                        if (value.toInt() == TpPort.MAX_PORT) {
                            sBuilder.matchTcpSrc(key);
                        } else {
                            sBuilder.matchTcpSrcMasked(key, value);
                        }
                    } else {
                        if (value.toInt() == TpPort.MAX_PORT) {
                            sBuilder.matchTcpDst(key);
                        } else {
                            sBuilder.matchTcpDstMasked(key, value);
                        }
                    }
                } else if (sgRule.getProtocol().equalsIgnoreCase(PROTO_UDP) ||
                        sgRule.getProtocol().equals(PROTO_UDP_NUM)) {
                    if (sgRule.getDirection().equalsIgnoreCase(EGRESS)) {
                        if (value.toInt() == TpPort.MAX_PORT) {
                            sBuilder.matchUdpSrc(key);
                        } else {
                            sBuilder.matchUdpSrcMasked(key, value);
                        }
                    } else {
                        if (value.toInt() == TpPort.MAX_PORT) {
                            sBuilder.matchUdpDst(key);
                        } else {
                            sBuilder.matchUdpDstMasked(key, value);
                        }
                    }
                } else if (sgRule.getProtocol().equalsIgnoreCase(PROTO_SCTP) ||
                        sgRule.getProtocol().equals(PROTO_SCTP_NUM)) {
                    if (sgRule.getDirection().equalsIgnoreCase(EGRESS)) {
                        if (value.toInt() == TpPort.MAX_PORT) {
                            sBuilder.matchSctpSrc(key);
                        } else {
                            sBuilder.matchSctpSrcMasked(key, value);
                        }
                    } else {
                        if (value.toInt() == TpPort.MAX_PORT) {
                            sBuilder.matchSctpDst(key);
                        } else {
                            sBuilder.matchSctpDstMasked(key, value);
                        }
                    }
                }

                selectorSet.add(sBuilder.build());
            });
        } else {

            TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
            buildMatches(sBuilder, sgRule, vmIp, remoteIp, netId);

            selectorSet.add(sBuilder.build());
        }

        return selectorSet;
    }

    private void buildMatches(TrafficSelector.Builder sBuilder,
                              SecurityGroupRule sgRule, Ip4Address vmIp,
                              IpPrefix remoteIp, String netId) {
        buildTunnelId(sBuilder, netId);
        buildMatchEthType(sBuilder, sgRule.getEtherType());
        buildMatchDirection(sBuilder, sgRule.getDirection(), vmIp);
        buildMatchProto(sBuilder, sgRule.getProtocol());
        buildMatchPort(sBuilder, sgRule.getProtocol(), sgRule.getDirection(),
                sgRule.getPortRangeMin() == null ? 0 : sgRule.getPortRangeMin(),
                sgRule.getPortRangeMax() == null ? 0 : sgRule.getPortRangeMax());
        buildMatchIcmp(sBuilder, sgRule.getProtocol(),
                sgRule.getPortRangeMin(), sgRule.getPortRangeMax());
        buildMatchRemoteIp(sBuilder, remoteIp, sgRule.getDirection());
    }

    private void buildTunnelId(TrafficSelector.Builder sBuilder, String netId) {
        String segId = osNetService.segmentId(netId);
        Type netType = osNetService.networkType(netId);

        if (netType == VLAN) {
            sBuilder.matchVlanId(VlanId.vlanId(segId));
        } else if (netType == VXLAN || netType == GRE || netType == GENEVE) {
            sBuilder.matchTunnelId(Long.valueOf(segId));
        } else {
            log.debug("Cannot tag the VID due to lack of support of virtual network type {}", netType);
        }
    }

    private void buildMatchDirection(TrafficSelector.Builder sBuilder,
                                     String direction,
                                     Ip4Address vmIp) {
        if (direction.equalsIgnoreCase(EGRESS)) {
            sBuilder.matchIPSrc(IpPrefix.valueOf(vmIp, VM_IP_PREFIX));
        } else {
            sBuilder.matchIPDst(IpPrefix.valueOf(vmIp, VM_IP_PREFIX));
        }
    }

    private void buildMatchEthType(TrafficSelector.Builder sBuilder, String etherType) {
        // Either IpSrc or IpDst (or both) is set by default, and we need to set EthType as IPv4.
        sBuilder.matchEthType(Ethernet.TYPE_IPV4);
        if (etherType != null && !Objects.equals(etherType, STR_NULL) &&
                !etherType.equalsIgnoreCase(ETHTYPE_IPV4)) {
            log.debug("EthType {} is not supported yet in Security Group", etherType);
        }
    }

    private void buildMatchRemoteIp(TrafficSelector.Builder sBuilder,
                                    IpPrefix remoteIpPrefix, String direction) {
        if (remoteIpPrefix != null &&
                !remoteIpPrefix.getIp4Prefix().equals(IP_PREFIX_ANY)) {
            if (direction.equalsIgnoreCase(EGRESS)) {
                sBuilder.matchIPDst(remoteIpPrefix);
            } else {
                sBuilder.matchIPSrc(remoteIpPrefix);
            }
        }
    }

    private void buildMatchProto(TrafficSelector.Builder sBuilder, String protocol) {
        if (protocol != null) {
            switch (protocol.toUpperCase()) {
                case PROTO_ICMP:
                case PROTO_ICMP_NUM:
                    sBuilder.matchIPProtocol(IPv4.PROTOCOL_ICMP);
                    break;
                case PROTO_TCP:
                case PROTO_TCP_NUM:
                    sBuilder.matchIPProtocol(IPv4.PROTOCOL_TCP);
                    break;
                case PROTO_UDP:
                case PROTO_UDP_NUM:
                    sBuilder.matchIPProtocol(IPv4.PROTOCOL_UDP);
                    break;
                case PROTO_SCTP:
                case PROTO_SCTP_NUM:
                    sBuilder.matchIPProtocol(PROTOCOL_SCTP);
                    break;
                default:
                    break;
            }
        }
    }

    private void buildMatchPort(TrafficSelector.Builder sBuilder,
                                String protocol, String direction,
                                int portMin, int portMax) {
        if (portMax > 0 && portMin == portMax) {
            if (protocol.equalsIgnoreCase(PROTO_TCP) ||
                    protocol.equals(PROTO_TCP_NUM)) {
                if (direction.equalsIgnoreCase(EGRESS)) {
                    sBuilder.matchTcpSrc(TpPort.tpPort(portMax));
                } else {
                    sBuilder.matchTcpDst(TpPort.tpPort(portMax));
                }
            } else if (protocol.equalsIgnoreCase(PROTO_UDP) ||
                    protocol.equals(PROTO_UDP_NUM)) {
                if (direction.equalsIgnoreCase(EGRESS)) {
                    sBuilder.matchUdpSrc(TpPort.tpPort(portMax));
                } else {
                    sBuilder.matchUdpDst(TpPort.tpPort(portMax));
                }
            } else if (protocol.equalsIgnoreCase(PROTO_SCTP) ||
                    protocol.equals(PROTO_SCTP_NUM)) {
                if (direction.equalsIgnoreCase(EGRESS)) {
                    sBuilder.matchSctpSrc(TpPort.tpPort(portMax));
                } else {
                    sBuilder.matchSctpDst(TpPort.tpPort(portMax));
                }
            }
        }


    }

    private void buildMatchIcmp(TrafficSelector.Builder sBuilder,
                                String protocol, Integer icmpCode, Integer icmpType) {
        if (protocol != null) {
            if (protocol.equalsIgnoreCase(PROTO_ICMP) ||
                    protocol.equals(PROTO_ICMP_NUM)) {
                if (icmpCode != null && icmpCode >= 0 && icmpCode <= 255) {
                    sBuilder.matchIcmpCode(icmpCode.byteValue());
                }
                if (icmpType != null && icmpType >= 0 && icmpType <= 255) {
                    sBuilder.matchIcmpType(icmpType.byteValue());
                }
            }
        }
    }

    private void resetSecurityGroupRules() {

        if (getUseSecurityGroupFlag()) {
            osNodeService.completeNodes(COMPUTE).forEach(node -> {
                osFlowRuleService.setUpTableMissEntry(node.intgBridge(), ACL_EGRESS_TABLE);
                initializeConnTrackTable(node.intgBridge(), true);
                initializeAclTable(node.intgBridge(), true);
                initializeIngressTable(node.intgBridge(), true);
            });

            securityGroupService.securityGroups().forEach(securityGroup ->
                    securityGroup.getRules().forEach(this::securityGroupRuleAdded));
        } else {
            osNodeService.completeNodes(COMPUTE).forEach(node -> {
                osFlowRuleService.connectTables(node.intgBridge(), ACL_EGRESS_TABLE, JUMP_TABLE);
                initializeConnTrackTable(node.intgBridge(), false);
                initializeAclTable(node.intgBridge(), false);
                initializeIngressTable(node.intgBridge(), false);
            });

            securityGroupService.securityGroups().forEach(securityGroup ->
                    securityGroup.getRules().forEach(this::securityGroupRuleRemoved));
        }

        log.info("Reset security group info " +
                (getUseSecurityGroupFlag() ? " with " : " without") + " Security Group");
    }

    private void securityGroupRuleAdded(SecurityGroupRule sgRule) {
        osNetService.ports().stream()
                .filter(port -> port.getSecurityGroups()
                        .contains(sgRule.getSecurityGroupId()))
                .forEach(port -> {
                    updateSecurityGroupRule(
                            instancePortService.instancePort(port.getId()),
                            port, sgRule, true);
                    log.debug("Applied security group rule {} to port {}",
                            sgRule.getId(), port.getId());
                });
    }

    private void securityGroupRuleRemoved(SecurityGroupRule sgRule) {
        Set<Port> removedPorts = new HashSet<>(removedOsPortStore.asJavaMap().values());

        Sets.union(osNetService.ports(), removedPorts).stream()
                .filter(port -> port.getSecurityGroups()
                        .contains(sgRule.getSecurityGroupId()))
                .forEach(port -> {
                    updateSecurityGroupRule(
                            instancePortService.instancePort(port.getId()),
                            port, sgRule, false);
                    log.debug("Removed security group rule {} from port {}",
                            sgRule.getId(), port.getId());
                });
    }

    private class InternalInstancePortListener implements InstancePortListener {

        @Override
        public boolean isRelevant(InstancePortEvent event) {
            return getUseSecurityGroupFlag();
        }

        private boolean isRelevantHelper(InstancePortEvent event) {
            return mastershipService.isLocalMaster(event.subject().deviceId());
        }

        @Override
        public void event(InstancePortEvent event) {
            switch (event.type()) {
                case OPENSTACK_INSTANCE_PORT_UPDATED:
                case OPENSTACK_INSTANCE_PORT_DETECTED:
                case OPENSTACK_INSTANCE_MIGRATION_STARTED:
                    eventExecutor.execute(() -> processInstanceMigrationStart(event));
                    break;
                case OPENSTACK_INSTANCE_PORT_VANISHED:
                    eventExecutor.execute(() -> processInstancePortVanish(event));
                    break;
                case OPENSTACK_INSTANCE_MIGRATION_ENDED:
                    eventExecutor.execute(() -> processInstanceMigrationEnd(event));
                    break;
                default:
                    break;
            }
        }

        private void processInstanceMigrationStart(InstancePortEvent event) {
            if (!isRelevantHelper(event)) {
                return;
            }

            InstancePort instPort = event.subject();
            installSecurityGroupRules(event, instPort);
            setAclRecircRules(instPort, true);
        }

        private void processInstancePortVanish(InstancePortEvent event) {
            if (!isRelevantHelper(event)) {
                return;
            }

            InstancePort instPort = event.subject();
            Port osPort = removedOsPortStore.asJavaMap().get(instPort.portId());
            setSecurityGroupRules(instPort, osPort, false);
            removedOsPortStore.remove(instPort.portId());
            setAclRecircRules(instPort, false);
        }

        private void processInstanceMigrationEnd(InstancePortEvent event) {
            if (!isRelevantHelper(event)) {
                return;
            }

            InstancePort instPort = event.subject();
            InstancePort revisedInstPort = swapStaleLocation(instPort);
            Port port = osNetService.port(instPort.portId());
            setSecurityGroupRules(revisedInstPort, port, false);
            setAclRecircRules(revisedInstPort, false);
        }

        private void installSecurityGroupRules(InstancePortEvent event,
                                               InstancePort instPort) {
            log.debug("Instance port detected/updated MAC:{} IP:{}",
                    instPort.macAddress(),
                    instPort.ipAddress());
            eventExecutor.execute(() ->
                    setSecurityGroupRules(instPort,
                            osNetService.port(event.subject().portId()), true));
        }

        private void setSecurityGroupRules(InstancePort instPort,
                                           Port port, boolean install) {
            Port osPort = port;

            if (!install) {
                Port rmvPort = removedOsPortStore.asJavaMap().get(instPort.portId());
                if (osPort == null && rmvPort == null) {
                    return;
                }

                if (port == null) {
                    osPort = rmvPort;
                }
            }

            final Port finalPort = osPort;

            osPort.getSecurityGroups().forEach(sgId -> {
                SecurityGroup sg = securityGroupService.securityGroup(sgId);
                if (sg == null) {
                    log.error("Security Group {} not found", sgId);
                    return;
                }
                sg.getRules().forEach(sgRule ->
                        updateSecurityGroupRule(instPort, finalPort, sgRule, install));
                final String action = install ? "Installed " : "Removed ";
                log.debug(action + "Security Group Rule ID : " + sgId);
            });
        }

        private void setAclRecircRules(InstancePort instPort, boolean install) {
            TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();

            Network net = osNetService.network(instPort.networkId());
            Type netType = osNetService.networkType(instPort.networkId());
            String segId = net.getProviderSegID();

            switch (netType) {
                case VXLAN:
                case GRE:
                case GENEVE:
                    sBuilder.matchTunnelId(Long.valueOf(segId));
                    break;
                case VLAN:
                    sBuilder.matchVlanId(VlanId.vlanId(segId));
                    break;
                default:
                    break;
            }

            sBuilder.matchEthType(Ethernet.TYPE_IPV4);
            sBuilder.matchIPDst(IpPrefix.valueOf(instPort.ipAddress(), VM_IP_PREFIX));

            TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
            tBuilder.transition(ACL_INGRESS_TABLE);

            osFlowRuleService.setRule(
                    appId,
                    instPort.deviceId(),
                    sBuilder.build(),
                    tBuilder.build(),
                    PRIORITY_ACL_RULE,
                    ACL_RECIRC_TABLE,
                    install);
        }
    }

    private class InternalOpenstackPortListener implements OpenstackNetworkListener {

        @Override
        public boolean isRelevant(OpenstackNetworkEvent event) {
            if (event.port() == null || Strings.isNullOrEmpty(event.port().getId())) {
                return false;
            }

            return getUseSecurityGroupFlag();
        }

        private boolean isRelevantHelper(OpenstackNetworkEvent event) {
            InstancePort instPort = instancePortService.instancePort(event.port().getId());

            if (instPort == null) {
                return false;
            }

            return mastershipService.isLocalMaster(instPort.deviceId());
        }

        @Override
        public void event(OpenstackNetworkEvent event) {
            log.debug("openstack port event received {}", event);

            if (event.type() == OPENSTACK_PORT_PRE_REMOVE) {
                eventExecutor.execute(() -> processPortPreRemove(event));
            }
        }

        private void processPortPreRemove(OpenstackNetworkEvent event) {
            if (!isRelevantHelper(event)) {
                return;
            }

            Port osPort = event.port();
            removedOsPortStore.put(osPort.getId(), osPort);
        }
    }

    private class InternalOpenstackNetworkListener implements OpenstackNetworkListener {

        @Override
        public boolean isRelevant(OpenstackNetworkEvent event) {
            if (event.port() == null || Strings.isNullOrEmpty(event.port().getId())) {
                return false;
            }

            return getUseSecurityGroupFlag();
        }

        private boolean isRelevantHelper(OpenstackNetworkEvent event) {
            if (event.securityGroupId() == null ||
                    securityGroupService.securityGroup(event.securityGroupId()) == null) {
                return false;
            }

            InstancePort instPort = instancePortService.instancePort(event.port().getId());

            if (instPort == null) {
                return false;
            }

            return mastershipService.isLocalMaster(instPort.deviceId());
        }

        @Override
        public void event(OpenstackNetworkEvent event) {
            log.debug("security group event received {}", event);

            switch (event.type()) {
                case OPENSTACK_PORT_SECURITY_GROUP_ADDED:
                    eventExecutor.execute(() -> processPortSgAdd(event));
                    break;
                case OPENSTACK_PORT_SECURITY_GROUP_REMOVED:
                    eventExecutor.execute(() -> processPortSgRemove(event));
                    break;
                default:
                    // do nothing for the other events
                    break;
            }
        }

        private void processPortSgAdd(OpenstackNetworkEvent event) {
            if (!isRelevantHelper(event)) {
                return;
            }

            InstancePort instPort = instancePortService.instancePort(event.port().getId());
            SecurityGroup osSg = securityGroupService.securityGroup(event.securityGroupId());

            osSg.getRules().forEach(sgRule -> {
                updateSecurityGroupRule(instPort, event.port(), sgRule, true);
            });
            log.info("Added security group {} to port {}",
                    event.securityGroupId(), event.port().getId());
        }

        private void processPortSgRemove(OpenstackNetworkEvent event) {
            if (!isRelevantHelper(event)) {
                return;
            }

            InstancePort instPort = instancePortService.instancePort(event.port().getId());
            SecurityGroup osSg = securityGroupService.securityGroup(event.securityGroupId());

            osSg.getRules().forEach(sgRule -> {
                updateSecurityGroupRule(instPort, event.port(), sgRule, false);
            });
            log.info("Removed security group {} from port {}",
                    event.securityGroupId(), event.port().getId());
        }
    }

    private class InternalSecurityGroupListener implements OpenstackSecurityGroupListener {

        @Override
        public boolean isRelevant(OpenstackSecurityGroupEvent event) {
            return getUseSecurityGroupFlag();
        }

        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(OpenstackSecurityGroupEvent event) {
            switch (event.type()) {
                case OPENSTACK_SECURITY_GROUP_RULE_CREATED:
                    eventExecutor.execute(() -> processSgRuleCreate(event));
                    break;
                case OPENSTACK_SECURITY_GROUP_RULE_REMOVED:
                    eventExecutor.execute(() -> processSgRuleRemove(event));
                    break;
                case OPENSTACK_SECURITY_GROUP_REMOVED:
                case OPENSTACK_SECURITY_GROUP_CREATED:
                default:
                    // do nothing
                    break;
            }
        }

        private void processSgRuleCreate(OpenstackSecurityGroupEvent event) {
            if (!isRelevantHelper()) {
                return;
            }

            SecurityGroupRule sgRuleToAdd = event.securityGroupRule();
            securityGroupRuleAdded(sgRuleToAdd);
            log.info("Applied new security group rule {} to ports", sgRuleToAdd.getId());
        }

        private void processSgRuleRemove(OpenstackSecurityGroupEvent event) {
            if (!isRelevantHelper()) {
                return;
            }

            SecurityGroupRule sgRuleToRemove = event.securityGroupRule();
            securityGroupRuleRemoved(sgRuleToRemove);
            log.info("Removed security group rule {} from ports", sgRuleToRemove.getId());
        }
    }

    private class InternalNodeListener implements OpenstackNodeListener {

        @Override
        public boolean isRelevant(OpenstackNodeEvent event) {
            return event.subject().type() == COMPUTE;
        }

        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(OpenstackNodeEvent event) {
            switch (event.type()) {
                case OPENSTACK_NODE_COMPLETE:
                    eventExecutor.execute(() -> processNodeComplete(event.subject()));
                    break;
                case OPENSTACK_NODE_CREATED:
                case OPENSTACK_NODE_REMOVED:
                case OPENSTACK_NODE_UPDATED:
                case OPENSTACK_NODE_INCOMPLETE:
                default:
                    break;
            }
        }

        private void processNodeComplete(OpenstackNode node) {
            if (!isRelevantHelper()) {
                return;
            }

            resetSecurityGroupRulesByNode(node);
        }

        private void resetSecurityGroupRulesByNode(OpenstackNode node) {
            if (getUseSecurityGroupFlag()) {
                osFlowRuleService.setUpTableMissEntry(node.intgBridge(), ACL_EGRESS_TABLE);
                initializeConnTrackTable(node.intgBridge(), true);
                initializeAclTable(node.intgBridge(), true);
                initializeIngressTable(node.intgBridge(), true);

                securityGroupService.securityGroups().forEach(securityGroup ->
                        securityGroup.getRules().forEach(
                                OpenstackSecurityGroupHandler.this::securityGroupRuleAdded));
            } else {
                osFlowRuleService.connectTables(node.intgBridge(), ACL_EGRESS_TABLE, JUMP_TABLE);
                initializeConnTrackTable(node.intgBridge(), false);
                initializeAclTable(node.intgBridge(), false);
                initializeIngressTable(node.intgBridge(), false);

                securityGroupService.securityGroups().forEach(securityGroup ->
                        securityGroup.getRules().forEach(
                                OpenstackSecurityGroupHandler.this::securityGroupRuleRemoved));
            }

            log.info("Reset security group info " +
                    (getUseSecurityGroupFlag() ? " with " : " without") + " Security Group");
        }
    }
}
