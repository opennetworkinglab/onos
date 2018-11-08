/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstackvtap.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.GroupId;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostLocation;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.DefaultTunnelDescription;
import org.onosproject.net.behaviour.ExtensionTreatmentResolver;
import org.onosproject.net.behaviour.InterfaceConfig;
import org.onosproject.net.behaviour.TunnelDescription;
import org.onosproject.net.behaviour.TunnelEndPoints;
import org.onosproject.net.behaviour.TunnelKey;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleOperationsContext;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.ExtensionPropertyException;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.group.DefaultGroupBucket;
import org.onosproject.net.group.DefaultGroupDescription;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.net.group.GroupBuckets;
import org.onosproject.net.group.GroupDescription;
import org.onosproject.net.group.GroupService;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeEvent;
import org.onosproject.openstacknode.api.OpenstackNodeListener;
import org.onosproject.openstacknode.api.OpenstackNodeService;
import org.onosproject.openstackvtap.api.OpenstackVtap;
import org.onosproject.openstackvtap.api.OpenstackVtap.Type;
import org.onosproject.openstackvtap.api.OpenstackVtapAdminService;
import org.onosproject.openstackvtap.api.OpenstackVtapCriterion;
import org.onosproject.openstackvtap.api.OpenstackVtapEvent;
import org.onosproject.openstackvtap.api.OpenstackVtapId;
import org.onosproject.openstackvtap.api.OpenstackVtapListener;
import org.onosproject.openstackvtap.api.OpenstackVtapNetwork;
import org.onosproject.openstackvtap.api.OpenstackVtapNetwork.Mode;
import org.onosproject.openstackvtap.api.OpenstackVtapService;
import org.onosproject.openstackvtap.api.OpenstackVtapStore;
import org.onosproject.openstackvtap.api.OpenstackVtapStoreDelegate;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Dictionary;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.packet.Ethernet.TYPE_IPV4;
import static org.onlab.packet.IPv4.PROTOCOL_ICMP;
import static org.onlab.packet.IPv4.PROTOCOL_TCP;
import static org.onlab.packet.IPv4.PROTOCOL_UDP;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.AnnotationKeys.PORT_NAME;
import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_RESUBMIT_TABLE;
import static org.onosproject.net.flow.instructions.ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_TUNNEL_DST;
import static org.onosproject.openstacknetworking.api.Constants.DHCP_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.FLAT_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.FORWARDING_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.VTAP_FLAT_OUTBOUND_GROUP_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.VTAP_FLAT_OUTBOUND_MIRROR_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.VTAP_FLAT_OUTBOUND_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.VTAP_INBOUND_GROUP_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.VTAP_INBOUND_MIRROR_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.VTAP_INBOUND_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.VTAP_OUTBOUND_GROUP_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.VTAP_OUTBOUND_MIRROR_TABLE;
import static org.onosproject.openstacknetworking.api.Constants.VTAP_OUTBOUND_TABLE;
import static org.onosproject.openstacknode.api.Constants.INTEGRATION_BRIDGE;
import static org.onosproject.openstacknode.api.NodeState.COMPLETE;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.COMPUTE;
import static org.onosproject.openstackvtap.impl.OsgiPropertyConstants.TUNNEL_NICIRA;
import static org.onosproject.openstackvtap.impl.OsgiPropertyConstants.TUNNEL_NICRA_DEFAULT;
import static org.onosproject.openstackvtap.util.OpenstackVtapUtil.containsIp;
import static org.onosproject.openstackvtap.util.OpenstackVtapUtil.dumpStackTrace;
import static org.onosproject.openstackvtap.util.OpenstackVtapUtil.getGroupKey;
import static org.onosproject.openstackvtap.util.OpenstackVtapUtil.getTunnelName;
import static org.onosproject.openstackvtap.util.OpenstackVtapUtil.getTunnelType;
import static org.onosproject.openstackvtap.util.OpenstackVtapUtil.hostCompareIp;
import static org.onosproject.openstackvtap.util.OpenstackVtapUtil.isValidHost;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides implementation of the openstack vtap and openstack vtap network APIs.
 */
@Component(
    immediate = true,
    service = { OpenstackVtapService.class, OpenstackVtapAdminService.class },
    property = {
        TUNNEL_NICIRA + ":Boolean=" + TUNNEL_NICRA_DEFAULT
    }
)
public class OpenstackVtapManager
        extends AbstractListenerManager<OpenstackVtapEvent, OpenstackVtapListener>
        implements OpenstackVtapService, OpenstackVtapAdminService {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected GroupService groupService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackNodeService osNodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)

    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackVtapStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService componentConfigService;

    /** Use nicra extension for tunneling. */
    private boolean tunnelNicira = TUNNEL_NICRA_DEFAULT;

    public static final String APP_ID = "org.onosproject.openstackvtap";
    public static final String VTAP_DESC_NULL = "vtap field %s cannot be null";

    private static final int PRIORITY_VTAP_RULE = 50000;
    private static final int PRIORITY_VTAP_OUTPUT_RULE = 1000;
    private static final int PRIORITY_VTAP_OUTPUT_DROP = 0;

    private static final int INBOUND_NEXT_TABLE = DHCP_TABLE;
    private static final int FLAT_OUTBOUND_NEXT_TABLE = FLAT_TABLE;
    private static final int OUTBOUND_NEXT_TABLE = FORWARDING_TABLE;

    private static final int[][] VTAP_TABLES = {
            {VTAP_INBOUND_TABLE, VTAP_INBOUND_GROUP_TABLE,
                    INBOUND_NEXT_TABLE, VTAP_INBOUND_MIRROR_TABLE},
            {VTAP_FLAT_OUTBOUND_TABLE, VTAP_FLAT_OUTBOUND_GROUP_TABLE,
                    FLAT_OUTBOUND_NEXT_TABLE, VTAP_FLAT_OUTBOUND_MIRROR_TABLE},
            {VTAP_OUTBOUND_TABLE, VTAP_OUTBOUND_GROUP_TABLE,
                    OUTBOUND_NEXT_TABLE, VTAP_OUTBOUND_MIRROR_TABLE}};
    private static final int VTAP_TABLE_INBOUND_IDX = 0;
    private static final int VTAP_TABLE_FLAT_OUTBOUND_IDX = 1;
    private static final int VTAP_TABLE_OUTBOUND_IDX = 2;
    private static final int VTAP_TABLE_INPUT_IDX = 0;
    private static final int VTAP_TABLE_GROUP_IDX = 1;
    private static final int VTAP_TABLE_NEXT_IDX = 2;
    private static final int VTAP_TABLE_OUTPUT_IDX = 3;

    private static final IpPrefix ARBITRARY_IP_PREFIX =
                    IpPrefix.valueOf(IpAddress.valueOf("0.0.0.0"), 0);
    private static final String TABLE_EXTENSION = "table";
    private static final String TUNNEL_DST_EXTENSION = "tunnelDst";

    private static final int VTAP_NETWORK_KEY = 0;

    private final DeviceListener deviceListener = new InternalDeviceListener();
    private final OpenstackNodeListener osNodeListener = new InternalOpenstackNodeListener();
    private final HostListener hostListener = new InternalHostListener();

    private OpenstackVtapStoreDelegate delegate = new InternalStoreDelegate();

    private ApplicationId appId;
    private NodeId localNodeId;
    private ScheduledExecutorService eventExecutor;

    private final Object syncInterface = new Object();              // notification of tunnel interface
    private static final int INTERFACE_MANIPULATION_TIMEOUT = 1000; // 1000msec
    private static final int INTERFACE_MANIPULATION_RETRY = 10;     // 10 times (totally 10sec)

    @Activate
    public void activate() {
        appId = coreService.registerApplication(APP_ID);
        localNodeId = clusterService.getLocalNode().id();
        leadershipService.runForLeadership(appId.name());
        componentConfigService.registerProperties(getClass());

        eventExecutor = newSingleThreadScheduledExecutor(
                groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

        store.setDelegate(delegate);
        eventDispatcher.addSink(OpenstackVtapEvent.class, listenerRegistry);

        deviceService.addListener(deviceListener);
        osNodeService.addListener(osNodeListener);
        hostService.addListener(hostListener);

        initVtap();

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        clearVtap();

        hostService.removeListener(hostListener);
        osNodeService.removeListener(osNodeListener);
        deviceService.removeListener(deviceListener);

        eventDispatcher.removeSink(OpenstackVtapEvent.class);
        store.unsetDelegate(delegate);

        eventExecutor.shutdown();

        componentConfigService.unregisterProperties(getClass(), false);
        leadershipService.withdraw(appId.name());

        log.info("Stopped");
    }

    @Modified
    protected void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();

        boolean updatedTunnelNicira = Tools.isPropertyEnabled(properties, TUNNEL_NICIRA);
        if (tunnelNicira != updatedTunnelNicira) {
            if (Objects.equals(localNodeId, leadershipService.getLeader(appId.name()))) {
                // Update the tunnel flow rule by reflecting the change.
                osNodeService.completeNodes(COMPUTE)
                        .forEach(osNode -> applyVtapNetwork(getVtapNetwork(), osNode, false));
                tunnelNicira = updatedTunnelNicira;
                osNodeService.completeNodes(COMPUTE).stream()
                        .filter(osNode -> osNode.state() == COMPLETE)
                        .forEach(osNode -> applyVtapNetwork(getVtapNetwork(), osNode, true));
                log.debug("Apply {} nicira extension for tunneling", tunnelNicira ? "enable" : "disable");
            } else {
                tunnelNicira = updatedTunnelNicira;
            }
        }

        log.info("Modified");
    }

    /**
     * Initializes the flow rules and group tables, tunneling interface for all completed compute nodes.
     */
    @Override
    public void initVtap() {
        if (Objects.equals(localNodeId, leadershipService.getLeader(appId.name()))) {
            osNodeService.completeNodes(COMPUTE).stream()
                    .filter(osNode -> osNode.state() == COMPLETE)
                    .forEach(osNode -> initVtapForNode(osNode));
            log.trace("{} flow rules, groups, tunnel interface are initialized", appId.name());
        }
    }

    /**
     * Clears the flow rules and group tables, tunneling interface for all compute nodes.
     */
    @Override
    public void clearVtap() {
        if (Objects.equals(localNodeId, leadershipService.getLeader(appId.name()))) {
            osNodeService.completeNodes(COMPUTE).stream()
                    .forEach(osNode -> clearVtapForNode(osNode));
            log.trace("{} flow rules, groups, tunnel interface are cleared", appId.name());
        }
    }

    /**
     * Purges all flow rules and group tables, tunneling interface for openstack vtap.
     */
    @Override
    public void purgeVtap() {
        // Remove all flow rules
        flowRuleService.removeFlowRulesById(appId);

        // Remove all groups and tunnel interfaces
        osNodeService.completeNodes(COMPUTE).stream()
                .filter(osNode -> osNode.state() == COMPLETE)
                .forEach(osNode -> {
                    groupService.getGroups(osNode.intgBridge(), appId)
                            .forEach(group ->
                                    groupService.removeGroup(osNode.intgBridge(), group.appCookie(), appId));

                    OpenstackVtapNetwork vtapNetwork = getVtapNetwork();
                    setTunnelInterface(osNode, vtapNetwork, false);
                });

        log.trace("{} all flow rules, groups, tunnel interface are purged", appId.name());
    }

    private void initVtapForNode(OpenstackNode osNode) {
        // Make base vtap network
        initVtapNetwork(osNode);

        // Make vtap connections by OpenstackVtap config
        getVtapsByDeviceId(osNode.intgBridge())
                .forEach(vtap -> applyVtap(vtap, osNode, true));

        // Make vtap networks by OpenstackVtapNetwork config
        applyVtapNetwork(getVtapNetwork(), osNode, true);
    }

    private void clearVtapForNode(OpenstackNode osNode) {
        // Clear vtap networks by OpenstackVtapNetwork config
        applyVtapNetwork(getVtapNetwork(), osNode, false);

        // Clear vtap connections by OpenstackVtap config
        getVtapsByDeviceId(osNode.intgBridge())
                .forEach(vtap -> applyVtap(vtap, osNode, false));

        // Clear base vtap network
        clearVtapNetwork(osNode);
    }

    /**
     * Initializes vtap pipeline of the given device.
     *
     * @param osNode device identifier
     */
    private void initVtapNetwork(OpenstackNode osNode) {
        // Create default output tables
        for (int idx = 0; idx < VTAP_TABLES.length; idx++) {
            setOutputTableForDrop(osNode.intgBridge(),
                    VTAP_TABLES[idx][VTAP_TABLE_OUTPUT_IDX], true);
        }

        // Create group tables
        for (int idx = 0; idx < VTAP_TABLES.length; idx++) {
            createGroupTable(osNode.intgBridge(),
                    VTAP_TABLES[idx][VTAP_TABLE_GROUP_IDX],
                    ImmutableList.of(VTAP_TABLES[idx][VTAP_TABLE_NEXT_IDX],
                            VTAP_TABLES[idx][VTAP_TABLE_OUTPUT_IDX]),
                    null);
        }
    }

    /**
     * Clear vtap pipeline of the given device.
     *
     * @param osNode device identifier
     */
    private void clearVtapNetwork(OpenstackNode osNode) {
        // Clear group tables
        for (int idx = 0; idx < VTAP_TABLES.length; idx++) {
            removeGroupTable(osNode.intgBridge(),
                    VTAP_TABLES[idx][VTAP_TABLE_GROUP_IDX]);
        }

        // Clear default output tables
        for (int idx = 0; idx < VTAP_TABLES.length; idx++) {
            setOutputTableForDrop(osNode.intgBridge(),
                    VTAP_TABLES[idx][VTAP_TABLE_OUTPUT_IDX], false);
        }
    }

    @Override
    public OpenstackVtapNetwork getVtapNetwork() {
        return store.getVtapNetwork(VTAP_NETWORK_KEY);
    }

    @Override
    public OpenstackVtapNetwork createVtapNetwork(Mode mode, Integer networkId, IpAddress serverIp) {
        checkNotNull(mode, VTAP_DESC_NULL, "mode");
        checkNotNull(serverIp, VTAP_DESC_NULL, "serverIp");
        DefaultOpenstackVtapNetwork vtapNetwork = DefaultOpenstackVtapNetwork.builder()
                .mode(mode)
                .networkId(networkId)
                .serverIp(serverIp)
                .build();
        return store.createVtapNetwork(VTAP_NETWORK_KEY, vtapNetwork);
    }

    @Override
    public OpenstackVtapNetwork updateVtapNetwork(OpenstackVtapNetwork description) {
        checkNotNull(description, VTAP_DESC_NULL, "vtapNetwork");
        return store.updateVtapNetwork(VTAP_NETWORK_KEY, description);
    }

    @Override
    public OpenstackVtapNetwork removeVtapNetwork() {
        return store.removeVtapNetwork(VTAP_NETWORK_KEY);
    }

    @Override
    public Set<DeviceId> getVtapNetworkDevices() {
        return store.getVtapNetworkDevices(VTAP_NETWORK_KEY);
    }

    @Override
    public int getVtapCount(Type type) {
        return store.getVtapCount(type);
    }

    @Override
    public Set<OpenstackVtap> getVtaps(Type type) {
        return store.getVtaps(type);
    }

    @Override
    public OpenstackVtap getVtap(OpenstackVtapId vtapId) {
        return store.getVtap(vtapId);
    }

    @Override
    public Set<OpenstackVtap> getVtapsByDeviceId(DeviceId deviceId) {
        return store.getVtapsByDeviceId(deviceId);
    }

    @Override
    public OpenstackVtap createVtap(Type type, OpenstackVtapCriterion vtapCriterion) {
        checkNotNull(type, VTAP_DESC_NULL, "type");
        checkNotNull(vtapCriterion, VTAP_DESC_NULL, "vtapCriterion");

        Set<DeviceId> txDevices = type.isValid(Type.VTAP_TX) ?
                getEdgeDevice(Type.VTAP_TX, vtapCriterion) : ImmutableSet.of();
        Set<DeviceId> rxDevices = type.isValid(Type.VTAP_RX) ?
                getEdgeDevice(Type.VTAP_RX, vtapCriterion) : ImmutableSet.of();

        DefaultOpenstackVtap description = DefaultOpenstackVtap.builder()
                .id(OpenstackVtapId.vtapId())
                .type(type)
                .vtapCriterion(vtapCriterion)
                .txDeviceIds(txDevices)
                .rxDeviceIds(rxDevices)
                .build();
        return store.createVtap(description);
    }

    @Override
    public OpenstackVtap updateVtap(OpenstackVtap description) {
        checkNotNull(description, VTAP_DESC_NULL, "vtap");

        Set<DeviceId> txDevices = description.type().isValid(Type.VTAP_TX) ?
                getEdgeDevice(Type.VTAP_TX, description.vtapCriterion()) : ImmutableSet.of();
        Set<DeviceId> rxDevices = description.type().isValid(Type.VTAP_RX) ?
                getEdgeDevice(Type.VTAP_RX, description.vtapCriterion()) : ImmutableSet.of();

        DefaultOpenstackVtap vtap = DefaultOpenstackVtap.builder(description)
                .txDeviceIds(txDevices)
                .rxDeviceIds(rxDevices)
                .build();
        return store.updateVtap(vtap, true);
    }

    @Override
    public OpenstackVtap removeVtap(OpenstackVtapId vtapId) {
        return store.removeVtap(vtapId);
    }

    /**
     * Obtains the identifier set of edge device where the targeted host is located.
     * Note that, in most of cases target host is attached to one device,
     * however, in some cases, the host can be attached to multiple devices.
     *
     * @param type          vtap type
     * @param criterion     vtap criterion
     * @return a collection of device identifiers
     */
    private Set<DeviceId> getEdgeDevice(Type type, OpenstackVtapCriterion criterion) {
        if (hostService.getHosts() == null) {
            return ImmutableSet.of();
        }

        Set<DeviceId> deviceIds = Sets.newConcurrentHashSet();
        StreamSupport.stream(hostService.getHosts().spliterator(), true)
                .filter(host -> isValidHost(host) &&
                        host.ipAddresses().stream().anyMatch(ip -> containsIp(type, criterion, ip)))
                .forEach(host -> {
                    Set<DeviceId> hostDeviceIds =
                            host.locations().stream()
                                    .map(HostLocation::deviceId)
                                    .filter(deviceId -> Objects.nonNull(osNodeService.node(deviceId)))
                                    .collect(Collectors.toSet());
                    deviceIds.addAll(hostDeviceIds);
                });
        return deviceIds;
    }

    /**
     * Updates device list of vtaps with respect to the host changes.
     *
     * @param newHost   new host instance
     * @param oldHost   old host instance
     */
    private void updateHostbyType(Type type, Host newHost, Host oldHost) {
        getVtaps(type).forEach(vtap -> {
            IpPrefix prefix = (type == Type.VTAP_TX) ?
                    vtap.vtapCriterion().srcIpPrefix() :
                    vtap.vtapCriterion().dstIpPrefix();

            int hostDiff = hostCompareIp(newHost, oldHost, prefix);
            if (hostDiff < 0) {
                oldHost.locations().stream()
                        .map(HostLocation::deviceId)
                        .forEach(deviceId ->
                                store.removeDeviceFromVtap(vtap.id(), type, deviceId));
            } else if (hostDiff > 0) {
                newHost.locations().stream()
                        .map(HostLocation::deviceId)
                        .filter(deviceId -> Objects.nonNull(osNodeService.node(deviceId)))
                        .forEach(deviceId ->
                                store.addDeviceToVtap(vtap.id(), type, deviceId));
            }
        });
    }

    private void updateHost(Host newHost, Host oldHost) {
        // update devices for vtap tx
        updateHostbyType(Type.VTAP_TX, newHost, oldHost);

        // update devices for vtap rx
        updateHostbyType(Type.VTAP_RX, newHost, oldHost);
    }

    private void applyFlowRule(FlowRule flowRule, boolean install) {
        FlowRuleOperations.Builder flowOpsBuilder = FlowRuleOperations.builder();

        if (install) {
            flowOpsBuilder.add(flowRule);
        } else {
            flowOpsBuilder.remove(flowRule);
        }

        flowRuleService.apply(flowOpsBuilder.build(new FlowRuleOperationsContext() {
            @Override
            public void onSuccess(FlowRuleOperations ops) {
                log.debug("Installed flow rules for vtap");
            }

            @Override
            public void onError(FlowRuleOperations ops) {
                log.warn("Failed to install flow rules for vtap");
            }
        }));
    }

    private void connectTables(DeviceId deviceId,
                               int fromTable,
                               int toTableOrGroup, boolean isGroup,
                               OpenstackVtapCriterion vtapCriterion,
                               int rulePriority, boolean install) {
        log.debug("Table Transition: table[{}] -> table/group[{}]", fromTable, toTableOrGroup);

        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder()
                .matchEthType(TYPE_IPV4);

        // if the IpPrefix is "0.0.0.0/0", we do not include such a match into the flow rule
        if (!vtapCriterion.srcIpPrefix().equals(ARBITRARY_IP_PREFIX)) {
            selectorBuilder.matchIPSrc(vtapCriterion.srcIpPrefix());
        }

        if (!vtapCriterion.dstIpPrefix().equals(ARBITRARY_IP_PREFIX)) {
            selectorBuilder.matchIPDst(vtapCriterion.dstIpPrefix());
        }

        switch (vtapCriterion.ipProtocol()) {
            case PROTOCOL_TCP:
                selectorBuilder.matchIPProtocol(vtapCriterion.ipProtocol());

                // Add port match only if the port number is greater than zero
                if (vtapCriterion.srcTpPort().toInt() > 0) {
                    selectorBuilder.matchTcpSrc(vtapCriterion.srcTpPort());
                }
                if (vtapCriterion.dstTpPort().toInt() > 0) {
                    selectorBuilder.matchTcpDst(vtapCriterion.dstTpPort());
                }
                break;
            case PROTOCOL_UDP:
                selectorBuilder.matchIPProtocol(vtapCriterion.ipProtocol());

                // Add port match only if the port number is greater than zero
                if (vtapCriterion.srcTpPort().toInt() > 0) {
                    selectorBuilder.matchUdpSrc(vtapCriterion.srcTpPort());
                }
                if (vtapCriterion.dstTpPort().toInt() > 0) {
                    selectorBuilder.matchUdpDst(vtapCriterion.dstTpPort());
                }
                break;
            case PROTOCOL_ICMP:
                selectorBuilder.matchIPProtocol(vtapCriterion.ipProtocol());
                break;
            default:
                break;
        }

        TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();
        if (isGroup) {
            treatmentBuilder.group(GroupId.valueOf(toTableOrGroup));
        } else {
            treatmentBuilder.transition(toTableOrGroup);
        }

        FlowRule flowRule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selectorBuilder.build())
                .withTreatment(treatmentBuilder.build())
                .withPriority(rulePriority)
                .fromApp(appId)
                .makePermanent()
                .forTable(fromTable)
                .build();

        applyFlowRule(flowRule, install);
    }

    /**
     * Creates/Removes a tunnel interface in a given openstack node by vtap network information.
     *
     * @param osNode openstack node
     * @param vtapNetwork openstack vtap network for making
     *
     */
    private boolean setTunnelInterface(OpenstackNode osNode,
                                       OpenstackVtapNetwork vtapNetwork,
                                       boolean install) {
        String tunnelName = getTunnelName(vtapNetwork.mode());
        if (tunnelName == null) {
            return false;
        }

        if (!deviceService.isAvailable(osNode.ovsdb())) {
            log.warn("Not available osNode {} ovs {}", osNode.hostname(), osNode.ovsdb());
            return false;
        }

        if (install == isInterfaceEnabled(osNode.intgBridge(), tunnelName)) {
            log.warn("Already {} {} interface on osNode ovs {}, bridge {}",
                    install ? "add" : "remove",
                    tunnelName, osNode.ovsdb(), osNode.intgBridge());
            return true;
        }

        Device device = deviceService.getDevice(osNode.ovsdb());
        if (device == null || !device.is(InterfaceConfig.class)) {
            log.warn("Not able to get InterfaceConfig on osNode ovs {}", osNode.ovsdb());
            return false;
        }

        InterfaceConfig ifaceConfig = device.as(InterfaceConfig.class);
        if (install) {
            TunnelDescription.Builder tunnelDesc = DefaultTunnelDescription.builder()
                    .deviceId(INTEGRATION_BRIDGE)
                    .ifaceName(tunnelName)
                    .type(getTunnelType(vtapNetwork.mode()))
                    .key((vtapNetwork.networkId() == 0) ? null : new TunnelKey<>(vtapNetwork.networkId()))
                    .remote(TunnelEndPoints.ipTunnelEndpoint(vtapNetwork.serverIp()));
            if (!ifaceConfig.addTunnelMode(tunnelName, tunnelDesc.build())) {
                log.error("Fail to create {} interface on osNode ovs {}", tunnelName, osNode.ovsdb());
                return false;
            }
        } else {
            if (!ifaceConfig.removeTunnelMode(tunnelName)) {
                log.error("Fail to remove {} interface on osNode ovs {}", tunnelName, osNode.ovsdb());
                return false;
            }
        }

        // Wait for tunnel interface create/remove complete
        synchronized (syncInterface) {
            for (int i = 0; i < INTERFACE_MANIPULATION_RETRY; i++) {
                try {
                    syncInterface.wait(INTERFACE_MANIPULATION_TIMEOUT);
                    if (install == isInterfaceEnabled(osNode.intgBridge(), tunnelName)) {
                        log.debug("Success to {} {} interface on osNode ovs {}, bridge {}",
                                install ? "add" : "remove",
                                tunnelName, osNode.ovsdb(), osNode.intgBridge());
                        return true;
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
        log.warn("Fail to {} {} interface on osNode ovs {}, bridge {}",
                install ? "add" : "remove",
                tunnelName, osNode.ovsdb(), osNode.intgBridge());
        return false;
    }

    /**
     * Checks whether a given network interface in a given openstack node is enabled or not.
     *
     * @param deviceId openstack node
     * @param interfaceName network interface name
     * @return true if the given interface is enabled, false otherwise
     */
    private boolean isInterfaceEnabled(DeviceId deviceId, String interfaceName) {
        return deviceService.isAvailable(deviceId) &&
                deviceService.getPorts(deviceId).parallelStream().anyMatch(port ->
                        Objects.equals(port.annotations().value(PORT_NAME), interfaceName) && port.isEnabled());
    }

    private PortNumber portNumber(DeviceId deviceId, String interfaceName) {
        Port port = deviceService.getPorts(deviceId).stream()
                .filter(p -> p.isEnabled() &&
                        Objects.equals(p.annotations().value(PORT_NAME), interfaceName))
                .findAny().orElse(null);
        return port != null ? port.number() : null;
    }

    private void setOutputTableForTunnel(DeviceId deviceId, int tableId,
                                         PortNumber outPort, IpAddress serverIp,
                                         boolean install) {
        log.debug("setOutputTableForTunnel[{}]: deviceId={}, tableId={}, outPort={}, serverIp={}",
                install ? "add" : "remove", deviceId, tableId, outPort, serverIp);

        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
                .setOutput(outPort);

        if (tunnelNicira) {
            ExtensionTreatment extensionTreatment = buildTunnelExtension(deviceId, serverIp);
            if (extensionTreatment == null) {
                return;
            }
            treatment.extension(extensionTreatment, deviceId);
        }

        FlowRule flowRule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(PRIORITY_VTAP_OUTPUT_RULE)
                .makePermanent()
                .forTable(tableId)
                .fromApp(appId)
                .build();

        log.debug("setOutputTableForTunnel flowRule={}, install={}", flowRule, install);
        applyFlowRule(flowRule, install);
    }

    private void setOutputTableForDrop(DeviceId deviceId, int tableId,
                                       boolean install) {
        TrafficSelector.Builder selector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();

        FlowRule flowRule = DefaultFlowRule.builder()
                .forDevice(deviceId)
                .withSelector(selector.build())
                .withTreatment(treatment.build())
                .withPriority(PRIORITY_VTAP_OUTPUT_DROP)
                .makePermanent()
                .forTable(tableId)
                .fromApp(appId)
                .build();
        applyFlowRule(flowRule, install);
    }

    private void setOutputTable(DeviceId deviceId, Mode mode,
                                IpAddress serverIp, boolean install) {
        log.debug("setOutputTable[{}]: deviceId={}, mode={}, serverIp={}",
                install ? "add" : "remove", deviceId, mode, serverIp);

        if (deviceId == null) {
            return;
        }

        switch (mode) {
            case GRE:
            case VXLAN:
                String tunnelName = getTunnelName(mode);
                PortNumber vtapPort = portNumber(deviceId, tunnelName);
                if (vtapPort != null) {
                    for (int idx = 0; idx < VTAP_TABLES.length; idx++) {
                        setOutputTableForTunnel(deviceId, VTAP_TABLES[idx][VTAP_TABLE_OUTPUT_IDX],
                                vtapPort, serverIp, install);
                    }
                } else {
                    log.warn("Vtap tunnel port {} doesn't exist", tunnelName);
                }
                break;
            default:
                log.warn("Invalid vtap network mode {}", mode);
                break;
        }
    }

    /**
     * Returns tunnel destination extension treatment object.
     *
     * @param deviceId device id to apply this treatment
     * @param remoteIp tunnel destination ip address
     * @return extension treatment
     */
    private ExtensionTreatment buildTunnelExtension(DeviceId deviceId, IpAddress remoteIp) {
        Device device = deviceService.getDevice(deviceId);
        if (device == null || !device.is(ExtensionTreatmentResolver.class)) {
            log.warn("Nicira extension treatment is not supported");
            return null;
        }

        ExtensionTreatmentResolver resolver = device.as(ExtensionTreatmentResolver.class);
        ExtensionTreatment treatment =
                resolver.getExtensionInstruction(NICIRA_SET_TUNNEL_DST.type());
        try {
            treatment.setPropertyValue(TUNNEL_DST_EXTENSION, remoteIp.getIp4Address());
            return treatment;
        } catch (ExtensionPropertyException e) {
            log.error("Failed to set nicira tunnelDst extension treatment for {}", deviceId);
            return null;
        }
    }

    private ExtensionTreatment buildResubmitExtension(DeviceId deviceId, int tableId) {
        Device device = deviceService.getDevice(deviceId);
        if (device == null || !device.is(ExtensionTreatmentResolver.class)) {
            log.warn("Nicira extension treatment is not supported");
            return null;
        }

        ExtensionTreatmentResolver resolver = device.as(ExtensionTreatmentResolver.class);
        ExtensionTreatment treatment =
                resolver.getExtensionInstruction(NICIRA_RESUBMIT_TABLE.type());

        try {
            treatment.setPropertyValue(TABLE_EXTENSION, ((short) tableId));
            return treatment;
        } catch (ExtensionPropertyException e) {
            log.error("Failed to set nicira resubmit extension treatment for {}", deviceId);
            return null;
        }
    }

    private void createGroupTable(DeviceId deviceId, int groupId,
                                  List<Integer> tableIds, List<PortNumber> ports) {
        List<GroupBucket> buckets = Lists.newArrayList();
        if (tableIds != null) {
            tableIds.forEach(tableId -> {
                TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
                        .extension(buildResubmitExtension(deviceId, tableId), deviceId);
                GroupBucket bucket = DefaultGroupBucket
                        .createAllGroupBucket(treatment.build());
                buckets.add(bucket);
            });
        }
        if (ports != null) {
            ports.forEach(port -> {
                TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
                        .setOutput(port);
                GroupBucket bucket = DefaultGroupBucket
                        .createAllGroupBucket(treatment.build());
                buckets.add(bucket);
            });
        }

        GroupDescription groupDescription = new DefaultGroupDescription(deviceId,
                GroupDescription.Type.ALL,
                new GroupBuckets(buckets),
                getGroupKey(groupId),
                groupId,
                appId);
        groupService.addGroup(groupDescription);
    }

    private void removeGroupTable(DeviceId deviceId, int groupId) {
        groupService.removeGroup(deviceId, getGroupKey(groupId), appId);
    }

    /**
     * Internal listener for device events.
     */
    private class InternalDeviceListener implements DeviceListener {

        @Override
        public void event(DeviceEvent event) {
            DeviceEvent.Type type = event.type();
            Device device = event.subject();

            switch (type) {
                case PORT_ADDED:
                case PORT_UPDATED:
                case PORT_REMOVED:
                    String portName = event.port().annotations().value(PORT_NAME);
                    if (portName.equals(getTunnelName(Mode.GRE)) ||
                            portName.equals(getTunnelName(Mode.VXLAN))) {
                        log.trace("InternalDeviceListener type={}, host={}", type, device);
                        synchronized (syncInterface) {
                            try {
                                syncInterface.notifyAll();
                            } catch (IllegalMonitorStateException e) {
                                log.warn("Already syncInterface exited");
                            }
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Internal listener for openstack node events.
     */
    private class InternalOpenstackNodeListener implements OpenstackNodeListener {

        @Override
        public boolean isRelevant(OpenstackNodeEvent event) {
            // do not allow to proceed without leadership and compute node
            NodeId leader = leadershipService.getLeader(appId.name());
            OpenstackNode osNode = event.subject();

            return Objects.equals(localNodeId, leader) && osNode.type() == COMPUTE;
        }

        @Override
        public void event(OpenstackNodeEvent event) {
            OpenstackNodeEvent.Type type = event.type();
            OpenstackNode osNode = event.subject();
            log.trace("InternalOpenstackNodeListener type={}, osNode={}", type, osNode);

            eventExecutor.execute(() -> {
                try {
                    switch (type) {
                        case OPENSTACK_NODE_COMPLETE:
                            initVtapForNode(osNode);
                            break;

                        case OPENSTACK_NODE_REMOVED:
                            clearVtapForNode(osNode);
                            break;

                        default:
                            break;
                    }
                } catch (Exception e) {
                    dumpStackTrace(log, e);
                }
            });
        }
    }

    /**
     * Internal listener for host events.
     */
    private class InternalHostListener implements HostListener {

        @Override
        public boolean isRelevant(HostEvent event) {
            Host host = event.subject();
            if (!isValidHost(host)) {
                log.debug("Invalid host detected, ignore it {}", host);
                return false;
            }

            // do not allow to proceed without leadership
            NodeId leader = leadershipService.getLeader(appId.name());
            return Objects.equals(localNodeId, leader);
        }

        @Override
        public void event(HostEvent event) {
            HostEvent.Type type = event.type();
            Host host = event.subject();
            Host prevHost = event.prevSubject();
            log.trace("InternalHostListener {}: {} -> {}", type, prevHost, host);

            eventExecutor.execute(() -> {
                try {
                    switch (event.type()) {
                        case HOST_ADDED:
                            updateHost(host, null);
                            break;

                        case HOST_REMOVED:
                            updateHost(null, host);
                            break;

                        case HOST_MOVED:
                        case HOST_UPDATED:
                            updateHost(host, prevHost);
                            break;

                        default:
                            break;
                    }
                } catch (Exception e) {
                    dumpStackTrace(log, e);
                }
            });
        }
    }

    // Store delegate to re-post events emitted from the store.
    private class InternalStoreDelegate implements OpenstackVtapStoreDelegate {

        @Override
        public void notify(OpenstackVtapEvent event) {
            OpenstackVtapEvent.Type type = event.type();
            log.trace("InternalStoreDelegate {}: {} -> {}", type, event.prevSubject(), event.subject());

            if (Objects.equals(localNodeId, leadershipService.getLeader(appId.name()))) {
                eventExecutor.execute(() -> {
                    try {
                        switch (type) {
                            case VTAP_NETWORK_ADDED:
                            case VTAP_NETWORK_UPDATED:
                            case VTAP_NETWORK_REMOVED:
                                // Update network
                                updateVtapNetwork(event.openstackVtapNetwork(),
                                        event.prevOpenstackVtapNetwork());
                                break;

                            case VTAP_ADDED:
                            case VTAP_UPDATED:
                            case VTAP_REMOVED:
                                // Update vtap rule
                                updateVtap(event.openstackVtap(),
                                        event.prevOpenstackVtap());
                                break;

                            default:
                                break;
                        }
                    } catch (Exception e) {
                        dumpStackTrace(log, e);
                    }
                });
            }
            post(event);
        }
    }

    private void applyVtap(OpenstackVtap vtap,
                           OpenstackNode osNode,
                           boolean install) {
        if (vtap == null || osNode == null) {
            return;
        }

        log.debug("applyVtap vtap={}, osNode={}, install={}", vtap, osNode, install);

        DeviceId deviceId = osNode.intgBridge();
        for (int idx = 0; idx < VTAP_TABLES.length; idx++) {
            if ((idx == VTAP_TABLE_INBOUND_IDX &&
                        vtap.type().isValid(Type.VTAP_TX) &&
                        vtap.txDeviceIds().contains(deviceId)) ||
                (idx != VTAP_TABLE_INBOUND_IDX &&
                        vtap.type().isValid(Type.VTAP_RX) &&
                        vtap.rxDeviceIds().contains(deviceId))) {
                connectTables(deviceId,
                        VTAP_TABLES[idx][VTAP_TABLE_INPUT_IDX],
                        VTAP_TABLES[idx][VTAP_TABLE_GROUP_IDX],
                        true,
                        vtap.vtapCriterion(), PRIORITY_VTAP_RULE, install);
            }
        }
    }

    private void updateVtap(OpenstackVtap vtap,
                            OpenstackVtap prevVtap) {
        if (Objects.equals(vtap, prevVtap)) {
            return;
        }

        Set<DeviceId> prevTxDeviceIds = (prevVtap != null ? prevVtap.txDeviceIds() : ImmutableSet.of());
        Set<DeviceId> txDeviceIds = (vtap != null ? vtap.txDeviceIds() : ImmutableSet.of());
        Set<DeviceId> prevRxDeviceIds = (prevVtap != null ? prevVtap.rxDeviceIds() : ImmutableSet.of());
        Set<DeviceId> rxDeviceIds = (vtap != null ? vtap.rxDeviceIds() : ImmutableSet.of());

        // Remake all vtap rule
        if (prevVtap != null) {
            Set<DeviceId> deviceIds = Sets.newHashSet();
            deviceIds.addAll(Sets.difference(prevTxDeviceIds, txDeviceIds));
            deviceIds.addAll(Sets.difference(prevRxDeviceIds, rxDeviceIds));
            deviceIds.stream()
                    .map(deviceId -> osNodeService.node(deviceId))
                    .filter(osNode -> Objects.nonNull(osNode) &&
                            osNode.type() == COMPUTE)
                    .forEach(osNode -> applyVtap(prevVtap, osNode, false));
        }
        if (vtap != null) {
            Set<DeviceId> deviceIds = Sets.newHashSet();
            deviceIds.addAll(Sets.difference(txDeviceIds, prevTxDeviceIds));
            deviceIds.addAll(Sets.difference(rxDeviceIds, prevRxDeviceIds));
            deviceIds.stream()
                    .map(deviceId -> osNodeService.node(deviceId))
                    .filter(osNode -> Objects.nonNull(osNode) &&
                            osNode.type() == COMPUTE && osNode.state() == COMPLETE)
                    .forEach(osNode -> applyVtap(vtap, osNode, true));
        }
    }

    // create/remove tunnel interface and output table
    private boolean applyVtapNetwork(OpenstackVtapNetwork vtapNetwork,
                                     OpenstackNode osNode,
                                     boolean install) {
        if (vtapNetwork == null || osNode == null) {
            return false;
        }

        if (install) {
            if (setTunnelInterface(osNode, vtapNetwork, true)) {
                setOutputTable(osNode.intgBridge(), vtapNetwork.mode(), vtapNetwork.serverIp(), true);
                store.addDeviceToVtapNetwork(VTAP_NETWORK_KEY, osNode.intgBridge());
                return true;
            }
        } else {
            Set<DeviceId> deviceIds = getVtapNetworkDevices();
            if (deviceIds != null && deviceIds.contains(osNode.intgBridge())) {
                store.removeDeviceFromVtapNetwork(VTAP_NETWORK_KEY, osNode.intgBridge());
                setOutputTable(osNode.intgBridge(), vtapNetwork.mode(), vtapNetwork.serverIp(), false);
                setTunnelInterface(osNode, vtapNetwork, false);
                return true;
            }
        }
        return false;
    }

    private void updateVtapNetwork(OpenstackVtapNetwork network,
                                   OpenstackVtapNetwork prevNetwork) {
        // Remake all output tables
        if (prevNetwork != null) {
            osNodeService.completeNodes(COMPUTE)
                    .forEach(osNode -> applyVtapNetwork(prevNetwork, osNode, false));
        }
        if (network != null) {
            osNodeService.completeNodes(COMPUTE).stream()
                    .filter(osNode -> osNode.state() == COMPLETE)
                    .forEach(osNode -> applyVtapNetwork(network, osNode, true));
        }
    }

}
