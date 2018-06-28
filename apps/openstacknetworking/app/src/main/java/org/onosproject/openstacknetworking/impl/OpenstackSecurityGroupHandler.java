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
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.TpPort;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.ExtensionSelector;
import org.onosproject.openstacknetworking.api.InstancePort;
import org.onosproject.openstacknetworking.api.InstancePortEvent;
import org.onosproject.openstacknetworking.api.InstancePortListener;
import org.onosproject.openstacknetworking.api.InstancePortService;
import org.onosproject.openstacknetworking.api.OpenstackFlowRuleService;
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
import org.openstack4j.model.network.Port;
import org.openstack4j.model.network.SecurityGroup;
import org.openstack4j.model.network.SecurityGroupRule;
import org.openstack4j.openstack.networking.domain.NeutronSecurityGroupRule;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.openstacknetworking.api.Constants.ACL_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.JUMP_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.CT_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.ERROR_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.OPENSTACK_NETWORKING_APP_ID;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_ACL_RULE;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_CT_DROP_RULE;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_CT_HOOK_RULE;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_CT_RULE;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.COMPUTE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Populates flow rules to handle OpenStack SecurityGroups.
 */
@Component(immediate = true)
public class OpenstackSecurityGroupHandler {

    private final Logger log = getLogger(getClass());

    private static final boolean USE_SECURITY_GROUP = false;

    @Property(name = "useSecurityGroup", boolValue = USE_SECURITY_GROUP,
            label = "Apply OpenStack security group rule for VM traffic")
    private boolean useSecurityGroup = USE_SECURITY_GROUP;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InstancePortService instancePortService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackNetworkService osNetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackSecurityGroupService securityGroupService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackFlowRuleService osFlowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackNodeService osNodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    private final InstancePortListener instancePortListener = new InternalInstancePortListener();
    private final OpenstackNetworkListener portListener = new InternalOpenstackPortListener();
    private final OpenstackSecurityGroupListener securityGroupListener = new InternalSecurityGroupListener();
    private final OpenstackNodeListener osNodeListener = new InternalNodeListener();
    private ApplicationId appId;
    private NodeId localNodeId;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler"));

    private static final String PROTO_ICMP = "ICMP";
    private static final String PROTO_TCP = "TCP";
    private static final String PROTO_UDP = "UDP";
    private static final String ETHTYPE_IPV4 = "IPV4";
    private static final String EGRESS = "EGRESS";
    private static final String INGRESS = "INGRESS";
    private static final IpPrefix IP_PREFIX_ANY = Ip4Prefix.valueOf("0.0.0.0/0");

    // We expose pipeline structure to SONA application considering removing pipeline soon.
    private static final int GOTO_CONNTRACK_TABLE = 2;
    private static final int GOTO_JUMP_TABLE = 3;

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
        osNetService.addListener(portListener);
        configService.registerProperties(getClass());
        osNodeService.addListener(osNodeListener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        instancePortService.removeListener(instancePortListener);
        securityGroupService.removeListener(securityGroupListener);
        osNetService.removeListener(portListener);
        configService.unregisterProperties(getClass(), false);
        osNodeService.removeListener(osNodeListener);
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    @Modified
    protected void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        Boolean flag;

        flag = Tools.isPropertyEnabled(properties, "useSecurityGroup");
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

    private void initializeConnTrackTable(DeviceId deviceId, boolean install) {

        //table=1,ip,ct_state=-trk, actions=ct(table:2)
        long ctState = RulePopulatorUtil.computeCtStateFlag(false, false, false);
        long ctMask = RulePopulatorUtil.computeCtMaskFlag(true, false, false);
        setConnTrackRule(deviceId, ctState, ctMask, CT_NO_COMMIT, (short) GOTO_CONNTRACK_TABLE,
                ACTION_NONE, PRIORITY_CT_HOOK_RULE, install);

        //table=2,ip,nw_dst=10.10.0.2,ct_state=+trk+est,action=goto_table:3
        ctState = RulePopulatorUtil.computeCtStateFlag(true, false, true);
        ctMask = RulePopulatorUtil.computeCtMaskFlag(true, false, true);
        setConnTrackRule(deviceId, ctState, ctMask, CT_NO_COMMIT, CT_NO_RECIRC,
                GOTO_JUMP_TABLE, PRIORITY_CT_RULE, install);

        //table=2,ip,nw_dst=10.10.0.2,ct_state=+trk+new,action=drop
        ctState = RulePopulatorUtil.computeCtStateFlag(true, true, false);
        ctMask = RulePopulatorUtil.computeCtMaskFlag(true, true, false);
        setConnTrackRule(deviceId, ctState, ctMask, CT_NO_COMMIT, CT_NO_RECIRC,
                ACTION_DROP, PRIORITY_CT_DROP_RULE, install);
    }

    private void setSecurityGroupRules(InstancePort instPort, Port port, boolean install) {
        port.getSecurityGroups().forEach(sgId -> {
            SecurityGroup sg = securityGroupService.securityGroup(sgId);
            if (sg == null) {
                log.error("Security Group Not Found : {}", sgId);
                return;
            }
            sg.getRules().forEach(sgRule -> updateSecurityGroupRule(instPort, port, sgRule, install));
            final String action = install ? "Installed " : "Removed ";
            log.debug(action + "security group rule ID : " + sgId);
        });
    }

    private void updateSecurityGroupRule(InstancePort instPort, Port port, SecurityGroupRule sgRule, boolean install) {

        if (instPort == null || port == null || sgRule == null) {
            return;
        }

        if (sgRule.getRemoteGroupId() != null && !sgRule.getRemoteGroupId().isEmpty()) {
            getRemoteInstPorts(port.getTenantId(), sgRule.getRemoteGroupId())
                .forEach(rInstPort -> {
                    populateSecurityGroupRule(sgRule, instPort, rInstPort.ipAddress().toIpPrefix(), install);
                    populateSecurityGroupRule(sgRule, rInstPort, instPort.ipAddress().toIpPrefix(), install);

                    SecurityGroupRule rSgRule = new NeutronSecurityGroupRule.SecurityGroupRuleConcreteBuilder()
                            .from(sgRule)
                            .direction(sgRule.getDirection().toUpperCase().equals(EGRESS) ? INGRESS : EGRESS).build();
                    populateSecurityGroupRule(rSgRule, instPort, rInstPort.ipAddress().toIpPrefix(), install);
                    populateSecurityGroupRule(rSgRule, rInstPort, instPort.ipAddress().toIpPrefix(), install);
                });
        } else {
            populateSecurityGroupRule(sgRule, instPort, sgRule.getRemoteIpPrefix() == null ? IP_PREFIX_ANY :
                    IpPrefix.valueOf(sgRule.getRemoteIpPrefix()), install);
        }
    }

    private void populateSecurityGroupRule(SecurityGroupRule sgRule, InstancePort instPort,
                                           IpPrefix remoteIp, boolean install) {
        Set<TrafficSelector> selectors = buildSelectors(sgRule,
                Ip4Address.valueOf(instPort.ipAddress().toInetAddress()), remoteIp);
        if (selectors == null || selectors.isEmpty()) {
            return;
        }

        selectors.forEach(selector -> {
            osFlowRuleService.setRule(appId,
                    instPort.deviceId(),
                    selector,
                    DefaultTrafficTreatment.builder().transition(JUMP_TABLE).build(),
                    PRIORITY_ACL_RULE,
                    ACL_TABLE,
                    install);
        });
    }

    /**
     * Sets connection tracking rule using OVS extension commands.
     * It is not so graceful, but I don't want to make it more general because it is going to be used
     * only here. The following is the usage of the function.
     *
     * @param deviceId Device ID
     * @param ctState ctState: please use RulePopulatorUtil.computeCtStateFlag() to build the value
     * @param ctMask crMask: please use RulePopulatorUtil.computeCtMaskFlag() to build the value
     * @param commit CT_COMMIT for commit action, CT_NO_COMMIT otherwise
     * @param recircTable table number for recirculation after CT actions. CT_NO_RECIRC with no recirculation
     * @param action Additional actions. ACTION_DROP, ACTION_NONE, GOTO_XXX_TABLE are supported.
     * @param priority priority value for the rule
     * @param install true for insertion, false for removal
     */
    private void setConnTrackRule(DeviceId deviceId, long ctState, long ctMask,
                                  int commit, short recircTable,
                                  int action, int priority, boolean install) {

        ExtensionSelector esCtSate = RulePopulatorUtil.buildCtExtensionSelector(driverService, deviceId,
                ctState, ctMask);
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .extension(esCtSate, deviceId)
                .matchEthType(Ethernet.TYPE_IPV4)
                .build();

        TrafficTreatment.Builder tb = DefaultTrafficTreatment.builder();

        if (commit == CT_COMMIT || recircTable > 0) {
            RulePopulatorUtil.NiriraConnTrackTreatmentBuilder natTreatmentBuilder =
                    RulePopulatorUtil.niciraConnTrackTreatmentBuilder(driverService, deviceId);
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

        if (action != ACTION_NONE) {
            tb.transition(action);
        }

        int tableType = ERROR_TABLE;
        if (priority == PRIORITY_CT_RULE || priority == PRIORITY_CT_DROP_RULE) {
            tableType = CT_TABLE;
        } else if (priority == PRIORITY_CT_HOOK_RULE) {
            tableType = ACL_TABLE;
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
     * @param tenantId tenant id
     * @param sgId security group id
     * @return set of ip addresses
     */
    private Set<InstancePort> getRemoteInstPorts(String tenantId, String sgId) {
        Set<InstancePort> remoteInstPorts;

        remoteInstPorts = osNetService.ports().stream()
                .filter(port -> port.getTenantId().equals(tenantId))
                .filter(port -> port.getSecurityGroups().contains(sgId))
                .map(port -> instancePortService.instancePort(port.getId()))
                .filter(instPort -> instPort != null && instPort.ipAddress() != null)
                .collect(Collectors.toSet());

        return Collections.unmodifiableSet(remoteInstPorts);
    }

    private Set<TrafficSelector> buildSelectors(SecurityGroupRule sgRule,
                                                Ip4Address vmIp,
                                                IpPrefix remoteIp) {
        if (remoteIp != null && remoteIp.equals(IpPrefix.valueOf(vmIp, 32))) {
            // do nothing if the remote IP is my IP
            return null;
        }

        Set<TrafficSelector> selectorSet = Sets.newHashSet();

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        buildMatchs(sBuilder, sgRule, vmIp, remoteIp);

        if (sgRule.getPortRangeMax() != null && sgRule.getPortRangeMin() != null &&
                sgRule.getPortRangeMin() < sgRule.getPortRangeMax()) {
            Map<TpPort, TpPort> portRangeMatchMap = buildPortRangeMatches(sgRule.getPortRangeMin(),
                    sgRule.getPortRangeMax());
            portRangeMatchMap.entrySet().forEach(entry -> {

                if (sgRule.getProtocol().toUpperCase().equals(PROTO_TCP)) {
                    if (sgRule.getDirection().toUpperCase().equals(EGRESS)) {
                        sBuilder.matchTcpSrcMasked(entry.getKey(), entry.getValue());
                    } else {
                        sBuilder.matchTcpDstMasked(entry.getKey(), entry.getValue());
                    }
                } else if (sgRule.getProtocol().toUpperCase().equals(PROTO_UDP)) {
                    if (sgRule.getDirection().toUpperCase().equals(EGRESS)) {
                        sBuilder.matchUdpSrcMasked(entry.getKey(), entry.getValue());
                    } else {
                        sBuilder.matchUdpDstMasked(entry.getKey(), entry.getValue());
                    }
                }

                selectorSet.add(sBuilder.build());
                }
            );
        } else {
            selectorSet.add(sBuilder.build());
        }

        return selectorSet;
    }

    private void buildMatchs(TrafficSelector.Builder sBuilder, SecurityGroupRule sgRule,
                             Ip4Address vmIp, IpPrefix remoteIp) {
        buildMatchEthType(sBuilder, sgRule.getEtherType());
        buildMatchDirection(sBuilder, sgRule.getDirection(), vmIp);
        buildMatchProto(sBuilder, sgRule.getProtocol());
        buildMatchPort(sBuilder, sgRule.getProtocol(), sgRule.getDirection(),
                sgRule.getPortRangeMin() == null ? 0 : sgRule.getPortRangeMin(),
                sgRule.getPortRangeMax() == null ? 0 : sgRule.getPortRangeMax());
        buildMatchRemoteIp(sBuilder, remoteIp, sgRule.getDirection());
        if (sgRule.getRemoteGroupId() != null && sgRule.getRemoteGroupId().isEmpty()) {
            buildMatchRemoteIp(sBuilder, remoteIp, sgRule.getDirection());
        }
    }

    private void buildMatchDirection(TrafficSelector.Builder sBuilder,
                                     String direction,
                                     Ip4Address vmIp) {
        if (direction.toUpperCase().equals(EGRESS)) {
            sBuilder.matchIPSrc(IpPrefix.valueOf(vmIp, 32));
        } else {
            sBuilder.matchIPDst(IpPrefix.valueOf(vmIp, 32));
        }
    }

    private void buildMatchEthType(TrafficSelector.Builder sBuilder, String etherType) {
        // Either IpSrc or IpDst (or both) is set by default, and we need to set EthType as IPv4.
        sBuilder.matchEthType(Ethernet.TYPE_IPV4);
        if (etherType != null && !Objects.equals(etherType, "null") &&
                !etherType.toUpperCase().equals(ETHTYPE_IPV4)) {
            log.debug("EthType {} is not supported yet in Security Group", etherType);
        }
    }

    private void buildMatchRemoteIp(TrafficSelector.Builder sBuilder, IpPrefix remoteIpPrefix, String direction) {
        if (remoteIpPrefix != null && !remoteIpPrefix.getIp4Prefix().equals(IP_PREFIX_ANY)) {
            if (direction.toUpperCase().equals(EGRESS)) {
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
                    sBuilder.matchIPProtocol(IPv4.PROTOCOL_ICMP);
                    break;
                case PROTO_TCP:
                    sBuilder.matchIPProtocol(IPv4.PROTOCOL_TCP);
                    break;
                case PROTO_UDP:
                    sBuilder.matchIPProtocol(IPv4.PROTOCOL_UDP);
                    break;
                default:
            }
        }
    }

    private void buildMatchPort(TrafficSelector.Builder sBuilder, String protocol, String direction,
                                int portMin, int portMax) {
        if (portMin > 0 && portMax > 0 && portMin == portMax) {
            if (protocol.toUpperCase().equals(PROTO_TCP)) {
                if (direction.toUpperCase().equals(EGRESS)) {
                    sBuilder.matchTcpSrc(TpPort.tpPort(portMax));
                } else {
                    sBuilder.matchTcpDst(TpPort.tpPort(portMax));
                }
            } else if (protocol.toUpperCase().equals(PROTO_UDP)) {
                if (direction.toUpperCase().equals(EGRESS)) {
                    sBuilder.matchUdpSrc(TpPort.tpPort(portMax));
                } else {
                    sBuilder.matchUdpDst(TpPort.tpPort(portMax));
                }
            }
        }
    }

    private void resetSecurityGroupRules() {

        if (useSecurityGroup) {
            osNodeService.completeNodes(OpenstackNode.NodeType.COMPUTE)
                    .forEach(node -> osFlowRuleService.setUpTableMissEntry(node.intgBridge(), ACL_TABLE));
            securityGroupService.securityGroups().forEach(securityGroup ->
                    securityGroup.getRules().forEach(this::securityGroupRuleAdded));
            osNodeService.nodes().stream()
                    .filter(node -> node.type().equals(OpenstackNode.NodeType.COMPUTE))
                    .forEach(node -> initializeConnTrackTable(node .intgBridge(), true));
        } else {
            osNodeService.completeNodes(OpenstackNode.NodeType.COMPUTE)
                    .forEach(node -> osFlowRuleService.connectTables(node.intgBridge(), ACL_TABLE, JUMP_TABLE));
            securityGroupService.securityGroups().forEach(securityGroup ->
                    securityGroup.getRules().forEach(this::securityGroupRuleRemoved));
            osNodeService.nodes().stream()
                    .filter(node -> node.type().equals(OpenstackNode.NodeType.COMPUTE))
                    .forEach(node -> initializeConnTrackTable(node.intgBridge(), false));
        }

        log.info("Reset security group info " + (useSecurityGroup ? " with " : " without") + " Security Group");
    }

    private void securityGroupRuleAdded(SecurityGroupRule sgRule) {
        osNetService.ports().stream()
                .filter(port -> port.getSecurityGroups().contains(sgRule.getSecurityGroupId()))
                .forEach(port -> {
                    updateSecurityGroupRule(
                            instancePortService.instancePort(port.getId()),
                            port, sgRule, true);
                    log.debug("Applied security group rule {} to port {}",
                            sgRule.getId(), port.getId());
                });
    }

    private void securityGroupRuleRemoved(SecurityGroupRule sgRule) {
        osNetService.ports().stream()
                .filter(port -> port.getSecurityGroups().contains(sgRule.getSecurityGroupId()))
                .forEach(port -> {
                    updateSecurityGroupRule(
                            instancePortService.instancePort(port.getId()),
                            port, sgRule, false);
                    log.debug("Removed security group rule {} from port {}",
                            sgRule.getId(), port.getId());
                });
    }

    private int binLower(String binStr, int bits) {
        String outBin = binStr.substring(0, 16 - bits);
        for (int i = 0; i < bits; i++) {
            outBin += "0";
        }

        return Integer.parseInt(outBin, 2);
    }

    private int binHigher(String binStr, int bits) {
        String outBin = binStr.substring(0, 16 - bits);
        for (int i = 0; i < bits; i++) {
            outBin += "1";
        }

        return Integer.parseInt(outBin, 2);
    }

    private int testMasks(String binStr, int start, int end) {
        int mask = 0;
        for (; mask <= 16; mask++) {
            int maskStart = binLower(binStr, mask);
            int maskEnd = binHigher(binStr, mask);
            if (maskStart < start || maskEnd > end) {
                return mask - 1;
            }
        }

        return mask;
    }

    private String getMask(int bits) {
        switch (bits) {
            case 0:  return "ffff";
            case 1:  return "fffe";
            case 2:  return "fffc";
            case 3:  return "fff8";
            case 4:  return "fff0";
            case 5:  return "ffe0";
            case 6:  return "ffc0";
            case 7:  return "ff80";
            case 8:  return "ff00";
            case 9:  return "fe00";
            case 10: return "fc00";
            case 11: return "f800";
            case 12: return "f000";
            case 13: return "e000";
            case 14: return "c000";
            case 15: return "8000";
            case 16: return "0000";
            default: return null;
        }
    }

    private Map<TpPort, TpPort> buildPortRangeMatches(int portMin, int portMax) {

        boolean processing = true;
        int start = portMin;
        Map<TpPort, TpPort> portMaskMap = Maps.newHashMap();
        while (processing) {
            String minStr = Integer.toBinaryString(start);
            String binStrMinPadded = "0000000000000000".substring(minStr.length()) + minStr;

            int mask = testMasks(binStrMinPadded, start, portMax);
            int maskStart = binLower(binStrMinPadded, mask);
            int maskEnd = binHigher(binStrMinPadded, mask);

            log.debug("start : {} port/mask = {} / {} ", start, getMask(mask), maskStart);
            portMaskMap.put(TpPort.tpPort(maskStart), TpPort.tpPort(Integer.parseInt(getMask(mask), 16)));

            start = maskEnd + 1;
            if (start > portMax) {
                processing = false;
            }
        }

        return portMaskMap;
    }

    private class InternalInstancePortListener implements InstancePortListener {

        @Override
        public boolean isRelevant(InstancePortEvent event) {
            InstancePort instPort = event.subject();
            if (!useSecurityGroup) {
                return false;
            }
            return mastershipService.isLocalMaster(instPort.deviceId());
        }

        @Override
        public void event(InstancePortEvent event) {
            InstancePort instPort = event.subject();
            switch (event.type()) {
                case OPENSTACK_INSTANCE_PORT_UPDATED:
                case OPENSTACK_INSTANCE_PORT_DETECTED:
                    log.debug("Instance port detected MAC:{} IP:{}",
                            instPort.macAddress(),
                            instPort.ipAddress());
                    eventExecutor.execute(() -> {
                        setSecurityGroupRules(instPort,
                                osNetService.port(event.subject().portId()),
                                true);
                    });
                    break;
                case OPENSTACK_INSTANCE_PORT_VANISHED:
                    log.debug("Instance port vanished MAC:{} IP:{}",
                            instPort.macAddress(),
                            instPort.ipAddress());
                    eventExecutor.execute(() -> {
                        setSecurityGroupRules(instPort,
                                osNetService.port(event.subject().portId()),
                                false);
                    });
                    break;
                default:
                    break;
            }
        }
    }

    private class InternalOpenstackPortListener implements OpenstackNetworkListener {

        @Override
        public boolean isRelevant(OpenstackNetworkEvent event) {
            if (event.port() == null || !Strings.isNullOrEmpty(event.port().getId())) {
                return false;
            }
            if (event.securityGroupId() == null ||
                    securityGroupService.securityGroup(event.securityGroupId()) == null) {
                return false;
            }
            if (instancePortService.instancePort(event.port().getId()) == null) {
                return false;
            }
            if (!useSecurityGroup) {
                return false;
            }
            return true;
        }

        @Override
        public void event(OpenstackNetworkEvent event) {
            Port osPort = event.port();
            InstancePort instPort = instancePortService.instancePort(osPort.getId());
            SecurityGroup osSg = securityGroupService.securityGroup(event.securityGroupId());

            switch (event.type()) {
                case OPENSTACK_PORT_SECURITY_GROUP_ADDED:
                    eventExecutor.execute(() -> {
                        osSg.getRules().forEach(sgRule -> {
                            updateSecurityGroupRule(instPort, osPort, sgRule, true);
                        });
                        log.info("Added security group {} to port {}",
                                event.securityGroupId(), event.port().getId());
                    });
                    break;
                case OPENSTACK_PORT_SECURITY_GROUP_REMOVED:
                    eventExecutor.execute(() -> {
                        osSg.getRules().forEach(sgRule -> {
                            updateSecurityGroupRule(instPort, osPort, sgRule, false);
                        });
                        log.info("Removed security group {} from port {}",
                                event.securityGroupId(), event.port().getId());
                    });
                    break;
                default:
                    // do nothing for the other events
                    break;
            }
        }
    }

    private class InternalSecurityGroupListener implements OpenstackSecurityGroupListener {

        @Override
        public boolean isRelevant(OpenstackSecurityGroupEvent event) {
            if (!useSecurityGroup) {
                return false;
            }
            return true;
        }

        @Override
        public void event(OpenstackSecurityGroupEvent event) {
            switch (event.type()) {
                case OPENSTACK_SECURITY_GROUP_RULE_CREATED:
                    SecurityGroupRule securityGroupRuleToAdd = event.securityGroupRule();
                    eventExecutor.execute(() -> {
                        securityGroupRuleAdded(securityGroupRuleToAdd);
                        log.info("Applied new security group rule {} to ports",
                                securityGroupRuleToAdd.getId());
                    });
                    break;

                case OPENSTACK_SECURITY_GROUP_RULE_REMOVED:
                    SecurityGroupRule securityGroupRuleToRemove = event.securityGroupRule();
                    eventExecutor.execute(() -> {
                        securityGroupRuleRemoved(securityGroupRuleToRemove);
                        log.info("Removed security group rule {} from ports",
                                securityGroupRuleToRemove.getId());
                    });
                    break;
                case OPENSTACK_SECURITY_GROUP_CREATED:
                case OPENSTACK_SECURITY_GROUP_REMOVED:
                default:
                    // do nothing
                    break;
            }
        }
    }

    private class InternalNodeListener implements OpenstackNodeListener {

        @Override
        public boolean isRelevant(OpenstackNodeEvent event) {
            // do not allow to proceed without leadership
            NodeId leader = leadershipService.getLeader(appId.name());
            if (!Objects.equals(localNodeId, leader)) {
                return false;
            }
            return event.subject().type() == COMPUTE;
        }

        @Override
        public void event(OpenstackNodeEvent event) {
            OpenstackNode osNode = event.subject();

            switch (event.type()) {
                case OPENSTACK_NODE_COMPLETE:
                    eventExecutor.execute(() -> {
                        try {
                            if (useSecurityGroup) {
                                initializeConnTrackTable(osNode.intgBridge(), true);
                                log.warn("SG table initialization : {} is done", osNode.intgBridge());
                            }
                        } catch (IllegalArgumentException e) {
                            log.error("ACL table initialization error : {}", e.getMessage());
                        }
                    });
                    break;
                case OPENSTACK_NODE_CREATED:
                case OPENSTACK_NODE_REMOVED:
                case OPENSTACK_NODE_UPDATED:
                case OPENSTACK_NODE_INCOMPLETE:
                default:
                    break;
            }
        }
    }
}
