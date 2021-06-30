/*
 * Copyright 2020-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.impl;

import com.google.common.collect.Sets;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cfg.ConfigProperty;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.kubevirtnetworking.api.KubevirtFlowRuleService;
import org.onosproject.kubevirtnetworking.api.KubevirtNetwork;
import org.onosproject.kubevirtnetworking.api.KubevirtNetworkEvent;
import org.onosproject.kubevirtnetworking.api.KubevirtNetworkListener;
import org.onosproject.kubevirtnetworking.api.KubevirtNetworkService;
import org.onosproject.kubevirtnetworking.api.KubevirtPort;
import org.onosproject.kubevirtnetworking.api.KubevirtPortEvent;
import org.onosproject.kubevirtnetworking.api.KubevirtPortListener;
import org.onosproject.kubevirtnetworking.api.KubevirtPortService;
import org.onosproject.kubevirtnetworking.api.KubevirtSecurityGroup;
import org.onosproject.kubevirtnetworking.api.KubevirtSecurityGroupEvent;
import org.onosproject.kubevirtnetworking.api.KubevirtSecurityGroupListener;
import org.onosproject.kubevirtnetworking.api.KubevirtSecurityGroupRule;
import org.onosproject.kubevirtnetworking.api.KubevirtSecurityGroupService;
import org.onosproject.kubevirtnetworking.util.RulePopulatorUtil;
import org.onosproject.kubevirtnode.api.KubevirtNode;
import org.onosproject.kubevirtnode.api.KubevirtNodeEvent;
import org.onosproject.kubevirtnode.api.KubevirtNodeListener;
import org.onosproject.kubevirtnode.api.KubevirtNodeService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.ExtensionSelector;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.store.service.StorageService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Dictionary;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.kubevirtnetworking.api.Constants.ACL_CT_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.ACL_EGRESS_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.ACL_INGRESS_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.ACL_RECIRC_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.ERROR_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.FORWARDING_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.KUBEVIRT_NETWORKING_APP_ID;
import static org.onosproject.kubevirtnetworking.api.Constants.PRIORITY_ACL_INGRESS_RULE;
import static org.onosproject.kubevirtnetworking.api.Constants.PRIORITY_ACL_RULE;
import static org.onosproject.kubevirtnetworking.api.Constants.PRIORITY_CT_DROP_RULE;
import static org.onosproject.kubevirtnetworking.api.Constants.PRIORITY_CT_HOOK_RULE;
import static org.onosproject.kubevirtnetworking.api.Constants.PRIORITY_CT_RULE;
import static org.onosproject.kubevirtnetworking.api.Constants.TENANT_ACL_CT_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.TENANT_ACL_EGRESS_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.TENANT_ACL_INGRESS_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.TENANT_ACL_RECIRC_TABLE;
import static org.onosproject.kubevirtnetworking.api.Constants.TENANT_FORWARDING_TABLE;
import static org.onosproject.kubevirtnetworking.api.KubevirtNetwork.Type.FLAT;
import static org.onosproject.kubevirtnetworking.api.KubevirtNetwork.Type.VLAN;
import static org.onosproject.kubevirtnetworking.impl.OsgiPropertyConstants.USE_SECURITY_GROUP;
import static org.onosproject.kubevirtnetworking.impl.OsgiPropertyConstants.USE_SECURITY_GROUP_DEFAULT;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.getPropertyValueAsBoolean;
import static org.onosproject.kubevirtnetworking.util.KubevirtNetworkingUtil.waitFor;
import static org.onosproject.kubevirtnetworking.util.RulePopulatorUtil.buildPortRangeMatches;
import static org.onosproject.kubevirtnetworking.util.RulePopulatorUtil.computeCtMaskFlag;
import static org.onosproject.kubevirtnetworking.util.RulePopulatorUtil.computeCtStateFlag;
import static org.onosproject.kubevirtnetworking.util.RulePopulatorUtil.niciraConnTrackTreatmentBuilder;
import static org.onosproject.kubevirtnode.api.KubevirtNode.Type.WORKER;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Populates flow rules to handle EdgeStack SecurityGroups.
 */
@Component(
        immediate = true,
        property = {
                USE_SECURITY_GROUP + ":Boolean=" + USE_SECURITY_GROUP_DEFAULT
        }
)
public class KubevirtSecurityGroupHandler {

    private final Logger log = getLogger(getClass());

    private static final int VM_IP_PREFIX = 32;

    private static final String STR_NULL = "null";
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

    private static final int ICMP_CODE_MIN = 0;
    private static final int ICMP_CODE_MAX = 255;
    private static final int ICMP_TYPE_MIN = 0;
    private static final int ICMP_TYPE_MAX = 255;

    private static final int CT_COMMIT = 0;
    private static final int CT_NO_COMMIT = 1;
    private static final short CT_NO_RECIRC = -1;

    private static final int ACTION_NONE = 0;
    private static final int ACTION_DROP = -1;

    /** Apply EdgeStack security group rule for VM traffic. */
    private boolean useSecurityGroup = USE_SECURITY_GROUP_DEFAULT;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipService mastershipService;

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

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtNodeService nodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtNetworkService networkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtPortService portService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtFlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected KubevirtSecurityGroupService securityGroupService;

    private final KubevirtPortListener portListener =
            new InternalKubevirtPortListener();
    private final KubevirtSecurityGroupListener securityGroupListener =
            new InternalSecurityGroupListener();
    private final KubevirtNodeListener nodeListener =
            new InternalNodeListener();
    private final KubevirtNetworkListener networkListener =
            new InternalNetworkListener();

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler"));

    private ApplicationId appId;
    private NodeId localNodeId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(KUBEVIRT_NETWORKING_APP_ID);
        localNodeId = clusterService.getLocalNode().id();
        securityGroupService.addListener(securityGroupListener);
        portService.addListener(portListener);
        networkService.addListener(networkListener);
        configService.registerProperties(getClass());
        nodeService.addListener(nodeListener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        securityGroupService.removeListener(securityGroupListener);
        portService.removeListener(portListener);
        configService.unregisterProperties(getClass(), false);
        nodeService.removeListener(nodeListener);
        networkService.removeListener(networkListener);
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

    private void initializeProviderConnTrackTable(DeviceId deviceId, boolean install) {
        initializeConnTrackTable(deviceId, ACL_CT_TABLE, FORWARDING_TABLE, install);
    }

    private void initializeTenantConnTrackTable(DeviceId deviceId, boolean install) {
        initializeConnTrackTable(deviceId, TENANT_ACL_CT_TABLE, TENANT_FORWARDING_TABLE, install);
    }

    private void initializeConnTrackTable(DeviceId deviceId, int ctTable,
                                            int forwardTable, boolean install) {

        // table={ACL_INGRESS_TABLE(44)},ip,ct_state=-trk, actions=ct(table:{ACL_CT_TABLE(45)})
        long ctState = computeCtStateFlag(false, false, false);
        long ctMask = computeCtMaskFlag(true, false, false);
        setConnTrackRule(deviceId, ctState, ctMask, CT_NO_COMMIT, (short) ctTable,
                ACTION_NONE, PRIORITY_CT_HOOK_RULE, install);

        //table={ACL_CT_TABLE(45)},ip,nw_dst=10.10.0.2,ct_state=+trk+est,action=goto_table:{NORMAL_TABLE(80)}
        ctState = computeCtStateFlag(true, false, true);
        ctMask = computeCtMaskFlag(true, false, true);
        setConnTrackRule(deviceId, ctState, ctMask, CT_NO_COMMIT, CT_NO_RECIRC,
                forwardTable, PRIORITY_CT_RULE, install);

        //table={ACL_CT_TABLE(45)},ip,nw_dst=10.10.0.2,ct_state=+trk+new,action=drop
        ctState = computeCtStateFlag(true, true, false);
        ctMask = computeCtMaskFlag(true, true, false);
        setConnTrackRule(deviceId, ctState, ctMask, CT_NO_COMMIT, CT_NO_RECIRC,
                ACTION_DROP, PRIORITY_CT_DROP_RULE, install);
    }

    private void initializeProviderAclTable(DeviceId deviceId, boolean install) {
        initializeAclTable(deviceId, ACL_RECIRC_TABLE, PortNumber.NORMAL, install);
    }

    private void initializeTenantAclTable(KubevirtNetwork network,
                                            DeviceId deviceId, boolean install) {
        // FIXME: in bridge initialization phase, some patch ports may not be
        // available until they are created, we wait for a while ensure all
        // patch ports are created via network bootstrap
        while (true) {
            if (network.tenantToTunnelPort(deviceId) != null) {
                break;
            } else {
                log.info("Wait for tenant patch ports creation for device {} " +
                         "and network {}", deviceId, network.networkId());
                waitFor(5);
            }
        }

        PortNumber patchPort = network.tenantToTunnelPort(deviceId);
        initializeAclTable(deviceId, TENANT_ACL_RECIRC_TABLE, patchPort, install);
    }

    private void initializeAclTable(DeviceId deviceId, int recircTable,
                                    PortNumber outport, boolean install) {

        ExtensionTreatment ctTreatment =
                niciraConnTrackTreatmentBuilder(driverService, deviceId)
                        .commit(true)
                        .build();

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        sBuilder.matchEthType(Ethernet.TYPE_IPV4);

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();
        tBuilder.extension(ctTreatment, deviceId)
                .setOutput(outport);

        flowRuleService.setRule(appId,
                deviceId,
                sBuilder.build(),
                tBuilder.build(),
                PRIORITY_ACL_INGRESS_RULE,
                recircTable,
                install);
    }

    private void initializeProviderEgressTable(DeviceId deviceId, boolean install) {
        initializeEgressTable(deviceId, ACL_EGRESS_TABLE, FORWARDING_TABLE, install);
    }

    private void initializeTenantEgressTable(DeviceId deviceId, boolean install) {
        initializeEgressTable(deviceId, TENANT_ACL_EGRESS_TABLE, TENANT_FORWARDING_TABLE, install);
    }

    private void initializeEgressTable(DeviceId deviceId, int egressTable,
                                        int forwardTable, boolean install) {
        if (install) {
            flowRuleService.setUpTableMissEntry(deviceId, TENANT_ACL_EGRESS_TABLE);
        } else {
            flowRuleService.connectTables(deviceId, egressTable, forwardTable);
        }
    }

    private void initializeProviderIngressTable(DeviceId deviceId, boolean install) {
        initializeIngressTable(deviceId, ACL_INGRESS_TABLE, FORWARDING_TABLE, install);
    }

    private void initializeTenantIngressTable(DeviceId deviceId, boolean install) {
        initializeIngressTable(deviceId, TENANT_ACL_INGRESS_TABLE, TENANT_FORWARDING_TABLE, install);
    }

    private void initializeIngressTable(DeviceId deviceId, int ingressTable,
                                        int forwardTable, boolean install) {
        if (install) {
            flowRuleService.setUpTableMissEntry(deviceId, ingressTable);
        } else {
            flowRuleService.connectTables(deviceId, ingressTable, forwardTable);
        }
    }

    private void initializeProviderPipeline(KubevirtNode node, boolean install) {
        initializeProviderIngressTable(node.intgBridge(), install);
        initializeProviderEgressTable(node.intgBridge(), install);
        initializeProviderConnTrackTable(node.intgBridge(), install);
        initializeProviderAclTable(node.intgBridge(), install);
    }

    private void initializeTenantPipeline(KubevirtNetwork network,
                                          KubevirtNode node, boolean install) {
        DeviceId deviceId = network.tenantDeviceId(node.hostname());
        if (deviceId == null) {
            return;
        }

        // we check whether the given device is available from the store
        // if not we will wait until the device is eventually created
        // FIXME: it would be better to listen to device event to perform
        // pipeline initialization rather on network events.
        while (true) {
            if (deviceService.getDevice(deviceId) != null) {
                break;
            } else {
                waitFor(5);
            }
        }

        initializeTenantIngressTable(deviceId, install);
        initializeTenantEgressTable(deviceId, install);
        initializeTenantConnTrackTable(deviceId, install);
        initializeTenantAclTable(network, deviceId, install);
    }

    private void updateSecurityGroupRule(KubevirtPort port,
                                         KubevirtSecurityGroupRule sgRule, boolean install) {

        if (port == null || sgRule == null) {
            return;
        }

        if (sgRule.remoteGroupId() != null && !sgRule.remoteGroupId().isEmpty()) {
            getRemotePorts(port, sgRule.remoteGroupId())
                    .forEach(rPort -> {
                        populateSecurityGroupRule(sgRule, port,
                                rPort.ipAddress().toIpPrefix(), install);
                        populateSecurityGroupRule(sgRule, rPort,
                                port.ipAddress().toIpPrefix(), install);

                        KubevirtSecurityGroupRule rSgRule = sgRule.updateDirection(
                                sgRule.direction().equalsIgnoreCase(EGRESS) ? INGRESS : EGRESS);
                        populateSecurityGroupRule(rSgRule, port,
                                rPort.ipAddress().toIpPrefix(), install);
                        populateSecurityGroupRule(rSgRule, rPort,
                                port.ipAddress().toIpPrefix(), install);
                    });
        } else {
            populateSecurityGroupRule(sgRule, port,
                    sgRule.remoteIpPrefix() == null ? IP_PREFIX_ANY :
                            sgRule.remoteIpPrefix(), install);
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

    private void populateSecurityGroupRule(KubevirtSecurityGroupRule sgRule,
                                           KubevirtPort port,
                                           IpPrefix remoteIp,
                                           boolean install) {
        if (!checkProtocol(sgRule.protocol())) {
            return;
        }

        DeviceId deviceId = port.isTenant() ? port.tenantDeviceId() : port.deviceId();

        Set<TrafficSelector> ctSelectors = buildSelectors(
                sgRule,
                Ip4Address.valueOf(port.ipAddress().toInetAddress()),
                port.macAddress(),
                remoteIp, port.networkId());
        if (ctSelectors == null || ctSelectors.isEmpty()) {
            return;
        }

        // if the device is not available we do not perform any action
        if (deviceId == null || !deviceService.isAvailable(deviceId)) {
            return;
        }

        // XXX All egress traffic needs to go through connection tracking module,
        // which might hurt its performance.
        ExtensionTreatment ctTreatment =
                niciraConnTrackTreatmentBuilder(driverService, deviceId)
                        .commit(true)
                        .build();

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        KubevirtNetwork net = networkService.network(port.networkId());

        int aclTable;
        if (sgRule.direction().equalsIgnoreCase(EGRESS)) {

            if (net.type() == FLAT || net.type() == VLAN) {
                aclTable = ACL_EGRESS_TABLE;
            } else {
                aclTable = TENANT_ACL_EGRESS_TABLE;
            }

            tBuilder.transition(TENANT_ACL_RECIRC_TABLE);
        } else {

            if (net.type() == FLAT || net.type() == VLAN) {
                aclTable = ACL_INGRESS_TABLE;
            } else {
                aclTable = TENANT_ACL_INGRESS_TABLE;
            }

            tBuilder.extension(ctTreatment, deviceId)
                    .transition(TENANT_FORWARDING_TABLE);
        }

        int finalAclTable = aclTable;
        ctSelectors.forEach(selector -> {
            flowRuleService.setRule(appId,
                    deviceId,
                    selector, tBuilder.build(),
                    PRIORITY_ACL_RULE,
                    finalAclTable,
                    install);
        });

        TrafficSelector tSelector = DefaultTrafficSelector.builder()
                        .matchEthType(Ethernet.TYPE_IPV4)
                        .matchEthDst(port.macAddress())
                        .matchIPDst(IpPrefix.valueOf(port.ipAddress(), 32))
                        .build();
        TrafficTreatment tTreatment = DefaultTrafficTreatment.builder()
                        .transition(TENANT_ACL_INGRESS_TABLE)
                        .build();

        flowRuleService.setRule(appId,
                    deviceId,
                    tSelector,
                    tTreatment,
                    PRIORITY_ACL_RULE,
                    TENANT_ACL_RECIRC_TABLE,
                    install);
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
            natTreatmentBuilder.commit(commit == CT_COMMIT);
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
            tableType = TENANT_ACL_CT_TABLE;
        } else if (priority == PRIORITY_CT_HOOK_RULE) {
            tableType = TENANT_ACL_INGRESS_TABLE;
        } else {
            log.error("Cannot an appropriate table for the conn track rule.");
        }

        flowRuleService.setRule(
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
     * @param srcPort edgestack port
     * @param sgId security group id
     * @return set of ip addresses
     */
    private Set<KubevirtPort> getRemotePorts(KubevirtPort srcPort, String sgId) {
        return portService.ports().stream()
                .filter(port -> !port.macAddress().equals(srcPort.macAddress()))
                .filter(port -> port.securityGroups().contains(sgId))
                .filter(port -> port.ipAddress() != null)
                .collect(Collectors.toSet());
    }

    private Set<TrafficSelector> buildSelectors(KubevirtSecurityGroupRule sgRule,
                                                Ip4Address vmIp,
                                                MacAddress vmMac,
                                                IpPrefix remoteIp,
                                                String netId) {
        if (remoteIp != null && remoteIp.equals(IpPrefix.valueOf(vmIp, VM_IP_PREFIX))) {
            // do nothing if the remote IP is my IP
            return null;
        }

        Set<TrafficSelector> selectorSet = Sets.newHashSet();

        if (sgRule.portRangeMax() != null && sgRule.portRangeMin() != null &&
                sgRule.portRangeMin() < sgRule.portRangeMax()) {
            Map<TpPort, TpPort> portRangeMatchMap =
                    buildPortRangeMatches(sgRule.portRangeMin(),
                            sgRule.portRangeMax());
            portRangeMatchMap.forEach((key, value) -> {

                TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
                buildMatches(sBuilder, sgRule, vmIp, vmMac, remoteIp);

                if (sgRule.protocol().equalsIgnoreCase(PROTO_TCP) ||
                        sgRule.protocol().equals(PROTO_TCP_NUM)) {
                    if (sgRule.direction().equalsIgnoreCase(EGRESS)) {
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
                } else if (sgRule.protocol().equalsIgnoreCase(PROTO_UDP) ||
                        sgRule.protocol().equals(PROTO_UDP_NUM)) {
                    if (sgRule.direction().equalsIgnoreCase(EGRESS)) {
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
                } else if (sgRule.protocol().equalsIgnoreCase(PROTO_SCTP) ||
                        sgRule.protocol().equals(PROTO_SCTP_NUM)) {
                    if (sgRule.direction().equalsIgnoreCase(EGRESS)) {
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
            buildMatches(sBuilder, sgRule, vmIp, vmMac, remoteIp);

            selectorSet.add(sBuilder.build());
        }

        return selectorSet;
    }

    private void buildMatches(TrafficSelector.Builder sBuilder,
                              KubevirtSecurityGroupRule sgRule, Ip4Address vmIp,
                              MacAddress vmMac, IpPrefix remoteIp) {
        buildMatchEthType(sBuilder, sgRule.etherType());
        buildMatchDirection(sBuilder, sgRule.direction(), vmIp, vmMac);
        buildMatchProto(sBuilder, sgRule.protocol());
        buildMatchPort(sBuilder, sgRule.protocol(), sgRule.direction(),
                sgRule.portRangeMin() == null ? 0 : sgRule.portRangeMin(),
                sgRule.portRangeMax() == null ? 0 : sgRule.portRangeMax());
        buildMatchIcmp(sBuilder, sgRule.protocol(),
                sgRule.portRangeMin(), sgRule.portRangeMax());
        buildMatchRemoteIp(sBuilder, remoteIp, sgRule.direction());
    }

    private void buildMatchDirection(TrafficSelector.Builder sBuilder,
                                     String direction,
                                     Ip4Address vmIp,
                                     MacAddress vmMac) {
        if (direction.equalsIgnoreCase(EGRESS)) {
            sBuilder.matchIPSrc(IpPrefix.valueOf(vmIp, VM_IP_PREFIX));
            sBuilder.matchEthSrc(vmMac);
        } else {
            sBuilder.matchIPDst(IpPrefix.valueOf(vmIp, VM_IP_PREFIX));
            sBuilder.matchEthDst(vmMac);
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
                if (icmpCode != null && icmpCode >= ICMP_CODE_MIN &&
                        icmpCode <= ICMP_CODE_MAX) {
                    sBuilder.matchIcmpCode(icmpCode.byteValue());
                }
                if (icmpType != null && icmpType >= ICMP_TYPE_MIN &&
                        icmpType <= ICMP_TYPE_MAX) {
                    sBuilder.matchIcmpType(icmpType.byteValue());
                }
            }
        }
    }

    private void resetSecurityGroupRules() {

        if (getUseSecurityGroupFlag()) {
            nodeService.completeNodes(WORKER).forEach(node -> {
                initializeProviderPipeline(node, true);

                for (KubevirtNetwork network : networkService.tenantNetworks()) {
                    initializeTenantPipeline(network, node, true);
                }
            });

            securityGroupService.securityGroups().forEach(securityGroup ->
                    securityGroup.rules().forEach(this::securityGroupRuleAdded));
        } else {
            nodeService.completeNodes(WORKER).forEach(node -> {
                initializeProviderPipeline(node, false);

                for (KubevirtNetwork network : networkService.tenantNetworks()) {
                    initializeTenantPipeline(network, node, false);
                }
            });

            securityGroupService.securityGroups().forEach(securityGroup ->
                    securityGroup.rules().forEach(this::securityGroupRuleRemoved));
        }

        log.info("Reset security group info " +
                (getUseSecurityGroupFlag() ? "with" : "without") + " Security Group");
    }

    private void securityGroupRuleAdded(KubevirtSecurityGroupRule sgRule) {
        portService.ports().stream()
                .filter(port -> port.securityGroups().contains(sgRule.securityGroupId()))
                .forEach(port -> {
                    updateSecurityGroupRule(port, sgRule, true);
                    log.info("Applied security group rule {} to port {}",
                            sgRule.id(), port.macAddress());
                });
    }

    private void securityGroupRuleRemoved(KubevirtSecurityGroupRule sgRule) {
        portService.ports().stream()
                .filter(port -> port.securityGroups().contains(sgRule.securityGroupId()))
                .forEach(port -> {
                    updateSecurityGroupRule(port, sgRule, false);
                    log.info("Removed security group rule {} from port {}",
                            sgRule.id(), port.macAddress());
                });
    }

    private class InternalKubevirtPortListener implements KubevirtPortListener {

        @Override
        public boolean isRelevant(KubevirtPortEvent event) {
            return getUseSecurityGroupFlag();
        }

        private boolean isRelevantHelper(KubevirtPortEvent event) {
            DeviceId deviceId = event.subject().deviceId();

            if (deviceId == null) {
                return false;
            }

            return mastershipService.isLocalMaster(deviceId);
        }

        @Override
        public void event(KubevirtPortEvent event) {
            log.debug("security group event received {}", event);

            switch (event.type()) {
                case KUBEVIRT_PORT_SECURITY_GROUP_ADDED:
                    eventExecutor.execute(() -> processPortSgAdd(event));
                    break;
                case KUBEVIRT_PORT_SECURITY_GROUP_REMOVED:
                    eventExecutor.execute(() -> processPortSgRemove(event));
                    break;
                case KUBEVIRT_PORT_REMOVED:
                    eventExecutor.execute(() -> processPortRemove(event));
                    break;
                case KUBEVIRT_PORT_DEVICE_ADDED:
                    eventExecutor.execute(() -> processPortDeviceAdded(event));
                    break;
                default:
                    // do nothing for the other events
                    break;
            }
        }

        private void processPortSgAdd(KubevirtPortEvent event) {
            if (!isRelevantHelper(event)) {
                return;
            }

            if (event.securityGroupId() == null ||
                securityGroupService.securityGroup(event.securityGroupId()) == null) {
                return;
            }

            KubevirtPort port = event.subject();
            KubevirtSecurityGroup sg = securityGroupService.securityGroup(event.securityGroupId());

            sg.rules().forEach(sgRule -> {
                updateSecurityGroupRule(port, sgRule, true);
            });
            log.info("Added security group {} to port {}",
                    event.securityGroupId(), event.subject().macAddress());
        }

        private void processPortSgRemove(KubevirtPortEvent event) {
            if (!isRelevantHelper(event)) {
                return;
            }

            if (event.securityGroupId() == null ||
                securityGroupService.securityGroup(event.securityGroupId()) == null) {
                return;
            }

            KubevirtPort port = event.subject();
            KubevirtSecurityGroup sg = securityGroupService.securityGroup(event.securityGroupId());

            sg.rules().forEach(sgRule -> {
                updateSecurityGroupRule(port, sgRule, false);
            });
            log.info("Removed security group {} from port {}",
                    event.securityGroupId(), event.subject().macAddress());
        }

        private void processPortRemove(KubevirtPortEvent event) {
            if (!isRelevantHelper(event)) {
                return;
            }

            KubevirtPort port = event.subject();
            for (String sgStr : port.securityGroups()) {
                KubevirtSecurityGroup sg = securityGroupService.securityGroup(sgStr);
                sg.rules().forEach(sgRule -> {
                    updateSecurityGroupRule(port, sgRule, false);
                });
                log.info("Removed security group {} from port {}",
                                        sgStr, event.subject().macAddress());
            }
        }

        private void processPortDeviceAdded(KubevirtPortEvent event) {
            if (!isRelevantHelper(event)) {
                return;
            }

            for (String sgId : event.subject().securityGroups()) {
                KubevirtSecurityGroup sg = securityGroupService.securityGroup(sgId);

                sg.rules().forEach(sgRule -> {
                    updateSecurityGroupRule(event.subject(), sgRule, true);
                });
                log.info("Added security group {} to port {}",
                        sg.id(), event.subject().macAddress());
            }
        }
    }

    private class InternalNetworkListener implements KubevirtNetworkListener {

        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(KubevirtNetworkEvent event) {
            switch (event.type()) {
                case KUBEVIRT_NETWORK_CREATED:
                    eventExecutor.execute(() -> processNetworkCreation(event.subject()));
                    break;
                case KUBEVIRT_NETWORK_REMOVED:
                case KUBEVIRT_NETWORK_UPDATED:
                default:
                    // do thing
                    break;
            }
        }

        private void processNetworkCreation(KubevirtNetwork network) {
            if (!isRelevantHelper()) {
                return;
            }

            Set<KubevirtNode> nodes = nodeService.completeNodes(WORKER);

            if (nodes.size() > 0) {
                // now we wait 5s for all tenant bridges are created,
                // FIXME: we need to fina a better way to wait all tenant bridges
                // are created before installing default security group rules
                waitFor(5);

                for (KubevirtNode node : nodes) {
                    initializeTenantPipeline(network, node, true);
                }
            }
        }
    }

    private class InternalSecurityGroupListener implements KubevirtSecurityGroupListener {

        @Override
        public boolean isRelevant(KubevirtSecurityGroupEvent event) {
            return getUseSecurityGroupFlag();
        }

        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(KubevirtSecurityGroupEvent event) {
            switch (event.type()) {
                case KUBEVIRT_SECURITY_GROUP_RULE_CREATED:
                    eventExecutor.execute(() -> processSgRuleCreate(event));
                    break;
                case KUBEVIRT_SECURITY_GROUP_RULE_REMOVED:
                    eventExecutor.execute(() -> processSgRuleRemove(event));
                    break;
                default:
                    // do nothing
                    break;
            }
        }

        private void processSgRuleCreate(KubevirtSecurityGroupEvent event) {
            if (!isRelevantHelper()) {
                return;
            }

            KubevirtSecurityGroupRule sgRuleToAdd = event.rule();
            securityGroupRuleAdded(sgRuleToAdd);
            log.info("Applied new security group rule {} to ports", sgRuleToAdd.id());
        }

        private void processSgRuleRemove(KubevirtSecurityGroupEvent event) {
            if (!isRelevantHelper()) {
                return;
            }

            KubevirtSecurityGroupRule sgRuleToRemove = event.rule();
            securityGroupRuleRemoved(sgRuleToRemove);
            log.info("Removed security group rule {} from ports", sgRuleToRemove.id());
        }
    }

    private class InternalNodeListener implements KubevirtNodeListener {

        @Override
        public boolean isRelevant(KubevirtNodeEvent event) {
            return event.subject().type() == WORKER;
        }

        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(KubevirtNodeEvent event) {
            switch (event.type()) {
                case KUBEVIRT_NODE_COMPLETE:
                    eventExecutor.execute(() -> processNodeComplete(event.subject()));
                    break;
                default:
                    break;
            }
        }

        private void processNodeComplete(KubevirtNode node) {
            if (!isRelevantHelper()) {
                return;
            }

            // FIXME: we wait all port get its deviceId updated
            waitFor(5);

            resetSecurityGroupRulesByNode(node);
        }

        private void resetSecurityGroupRulesByNode(KubevirtNode node) {
            if (getUseSecurityGroupFlag()) {
                initializeProviderPipeline(node, true);

                for (KubevirtNetwork network : networkService.tenantNetworks()) {
                    initializeTenantPipeline(network, node, true);
                }

                securityGroupService.securityGroups().forEach(securityGroup ->
                        securityGroup.rules().forEach(
                                KubevirtSecurityGroupHandler.this::securityGroupRuleAdded));
            } else {
                initializeProviderPipeline(node, false);

                for (KubevirtNetwork network : networkService.tenantNetworks()) {
                    initializeTenantPipeline(network, node, false);
                }

                securityGroupService.securityGroups().forEach(securityGroup ->
                        securityGroup.rules().forEach(
                                KubevirtSecurityGroupHandler.this::securityGroupRuleRemoved));
            }

            log.info("Reset security group info " +
                    (getUseSecurityGroupFlag() ? "with" : "without") + " Security Group");
        }
    }
}