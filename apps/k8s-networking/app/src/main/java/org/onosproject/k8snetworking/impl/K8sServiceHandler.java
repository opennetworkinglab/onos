/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.k8snetworking.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import io.fabric8.kubernetes.api.model.EndpointAddress;
import io.fabric8.kubernetes.api.model.EndpointPort;
import io.fabric8.kubernetes.api.model.EndpointSubset;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.Service;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.TpPort;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.GroupId;
import org.onosproject.k8snetworking.api.K8sEndpointsService;
import org.onosproject.k8snetworking.api.K8sFlowRuleService;
import org.onosproject.k8snetworking.api.K8sGroupRuleService;
import org.onosproject.k8snetworking.api.K8sNetworkService;
import org.onosproject.k8snetworking.api.K8sPodService;
import org.onosproject.k8snetworking.api.K8sServiceEvent;
import org.onosproject.k8snetworking.api.K8sServiceListener;
import org.onosproject.k8snetworking.api.K8sServiceService;
import org.onosproject.k8snetworking.util.RulePopulatorUtil;
import org.onosproject.k8snetworking.util.RulePopulatorUtil.NiciraConnTrackTreatmentBuilder;
import org.onosproject.k8snode.api.K8sNode;
import org.onosproject.k8snode.api.K8sNodeEvent;
import org.onosproject.k8snode.api.K8sNodeListener;
import org.onosproject.k8snode.api.K8sNodeService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.ExtensionSelector;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.group.GroupBucket;
import org.onosproject.store.service.AtomicCounter;
import org.onosproject.store.service.StorageService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.k8snetworking.api.Constants.DEFAULT_SERVICE_IP_NAT_MODE_STR;
import static org.onosproject.k8snetworking.api.Constants.JUMP_TABLE;
import static org.onosproject.k8snetworking.api.Constants.K8S_NETWORKING_APP_ID;
import static org.onosproject.k8snetworking.api.Constants.NAT_STATEFUL;
import static org.onosproject.k8snetworking.api.Constants.NAT_STATELESS;
import static org.onosproject.k8snetworking.api.Constants.NAT_TABLE;
import static org.onosproject.k8snetworking.api.Constants.POD_TABLE;
import static org.onosproject.k8snetworking.api.Constants.PRIORITY_CT_RULE;
import static org.onosproject.k8snetworking.api.Constants.PRIORITY_NAT_RULE;
import static org.onosproject.k8snetworking.api.Constants.ROUTING_TABLE;
import static org.onosproject.k8snetworking.api.Constants.SERVICE_IP_CIDR;
import static org.onosproject.k8snetworking.api.Constants.SERVICE_TABLE;
import static org.onosproject.k8snetworking.api.Constants.SHIFTED_IP_CIDR;
import static org.onosproject.k8snetworking.api.Constants.SHIFTED_IP_PREFIX;
import static org.onosproject.k8snetworking.util.K8sNetworkingUtil.nodeIpGatewayIpMap;
import static org.onosproject.k8snetworking.util.RulePopulatorUtil.CT_NAT_DST_FLAG;
import static org.onosproject.k8snetworking.util.RulePopulatorUtil.buildGroupBucket;
import static org.onosproject.k8snetworking.util.RulePopulatorUtil.buildLoadExtension;
import static org.onosproject.k8snetworking.util.RulePopulatorUtil.buildResubmitExtension;
import static org.onosproject.k8snetworking.util.RulePopulatorUtil.computeCtMaskFlag;
import static org.onosproject.k8snetworking.util.RulePopulatorUtil.computeCtStateFlag;
import static org.onosproject.k8snetworking.util.RulePopulatorUtil.niciraConnTrackTreatmentBuilder;
import static org.onosproject.net.group.GroupDescription.Type.SELECT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Handles the service IP to pod IP related translation traffic.
 */
@Component(immediate = true)
public class K8sServiceHandler {

    private final Logger log = getLogger(getClass());

    private static final int HOST_CIDR_NUM = 32;

    private static final String NONE = "None";
    private static final String CLUSTER_IP = "ClusterIP";
    private static final String TCP = "TCP";
    private static final String SERVICE_IP_NAT_MODE = "serviceIpNatMode";

    private static final String GROUP_ID_COUNTER_NAME = "group-id-counter";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected K8sNetworkService k8sNetworkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected K8sFlowRuleService k8sFlowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected K8sGroupRuleService k8sGroupRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected K8sNodeService k8sNodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected K8sEndpointsService k8sEndpointsService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected K8sServiceService k8sServiceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected K8sPodService k8sPodService;

    @Property(name = SERVICE_IP_NAT_MODE, value = DEFAULT_SERVICE_IP_NAT_MODE_STR,
            label = "Service IP address translation mode.")
    private String serviceIpNatMode = DEFAULT_SERVICE_IP_NAT_MODE_STR;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));
    private final InternalNodeEventListener internalNodeEventListener =
            new InternalNodeEventListener();
    private final InternalK8sServiceListener internalK8sServiceListener =
            new InternalK8sServiceListener();

    private AtomicCounter groupIdCounter;

    private ApplicationId appId;
    private NodeId localNodeId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(K8S_NETWORKING_APP_ID);
        configService.registerProperties(getClass());
        localNodeId = clusterService.getLocalNode().id();
        leadershipService.runForLeadership(appId.name());
        k8sNodeService.addListener(internalNodeEventListener);
        k8sServiceService.addListener(internalK8sServiceListener);

        groupIdCounter = storageService.getAtomicCounter(GROUP_ID_COUNTER_NAME);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        leadershipService.withdraw(appId.name());
        k8sNodeService.removeListener(internalNodeEventListener);
        k8sServiceService.removeListener(internalK8sServiceListener);
        configService.unregisterProperties(getClass(), false);
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    @Modified
    void modified(ComponentContext context) {
        readComponentConfiguration(context);

        log.info("Modified");
    }

    private void setStatefulServiceNatRules(DeviceId deviceId, boolean install) {
        // -trk CT rules
        long ctUntrack = computeCtStateFlag(false, false, false);
        long ctMaskUntrack = computeCtMaskFlag(true, false, false);

        k8sNetworkService.networks().forEach(n -> {
            // TODO: need to provide a way to add multiple service IP CIDR ranges
            setUntrack(deviceId, ctUntrack, ctMaskUntrack, n.cidr(), SERVICE_IP_CIDR,
                    JUMP_TABLE, NAT_TABLE, PRIORITY_CT_RULE, install);
            setUntrack(deviceId, ctUntrack, ctMaskUntrack, n.cidr(), n.cidr(),
                    JUMP_TABLE, ROUTING_TABLE, PRIORITY_CT_RULE, install);
        });

        // +trk-new CT rules
        long ctTrackUnnew = computeCtStateFlag(true, false, false);
        long ctMaskTrackUnnew = computeCtMaskFlag(true, true, false);

        setTrackEstablish(deviceId, ctTrackUnnew, ctMaskTrackUnnew,
                NAT_TABLE, ROUTING_TABLE, PRIORITY_CT_RULE, install);

        // +trk+new CT rules
        long ctTrackNew = computeCtStateFlag(true, true, false);
        long ctMaskTrackNew = computeCtMaskFlag(true, true, false);

        k8sServiceService.services().stream()
                .filter(s -> CLUSTER_IP.equals(s.getSpec().getType()))
                .forEach(s -> setStatefulGroupFlowRules(deviceId, ctTrackNew,
                ctMaskTrackNew, s, install));
    }

    private void setStatelessServiceNatRules(DeviceId deviceId, boolean install) {

        k8sNetworkService.networks().forEach(n -> {
            setSrcDstCidrRules(deviceId, n.cidr(), SERVICE_IP_CIDR, JUMP_TABLE,
                    SERVICE_TABLE, PRIORITY_CT_RULE, install);
            setSrcDstCidrRules(deviceId, n.cidr(), SHIFTED_IP_CIDR, JUMP_TABLE,
                    POD_TABLE, PRIORITY_CT_RULE, install);
            setSrcDstCidrRules(deviceId, n.cidr(), n.cidr(), JUMP_TABLE,
                    ROUTING_TABLE, PRIORITY_CT_RULE, install);
        });

        // setup load balancing rules using group table
        k8sServiceService.services().stream()
                .filter(s -> CLUSTER_IP.equals(s.getSpec().getType()))
                .forEach(s -> setStatelessGroupFlowRules(deviceId, s, install));
    }

    private void setSrcDstCidrRules(DeviceId deviceId, String srcCidr,
                                    String dstCidr, int installTable,
                                    int transitTable, int priority, boolean install) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPSrc(IpPrefix.valueOf(srcCidr))
                .matchIPDst(IpPrefix.valueOf(dstCidr))
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .transition(transitTable)
                .build();

        k8sFlowRuleService.setRule(
                appId,
                deviceId,
                selector,
                treatment,
                priority,
                installTable,
                install);
    }

    private void setStatelessGroupFlowRules(DeviceId deviceId,
                                            Service service, boolean install) {
        int groupId = (int) groupIdCounter.incrementAndGet();

        List<GroupBucket> buckets = Lists.newArrayList();

        String serviceName = service.getMetadata().getName();

        List<Endpoints> endpointses = k8sEndpointsService.endpointses()
                .stream()
                .filter(ep -> serviceName.equals(ep.getMetadata().getName()))
                .collect(Collectors.toList());

        Map<String, String> nodeIpGatewayIpMap =
                nodeIpGatewayIpMap(k8sNodeService, k8sNetworkService);

        Map<String, Set<Integer>> podIpPorts = Maps.newConcurrentMap();

        for (Endpoints endpoints : endpointses) {
            for (EndpointSubset endpointSubset : endpoints.getSubsets()) {
                List<EndpointPort> ports = endpointSubset.getPorts()
                        .stream()
                        .filter(p -> p.getProtocol().equals(TCP))
                        .collect(Collectors.toList());

                for (EndpointAddress address : endpointSubset.getAddresses()) {
                    String podIp = nodeIpGatewayIpMap.containsKey(address.getIp()) ?
                            nodeIpGatewayIpMap.get(address.getIp()) : address.getIp();

                    ports.forEach(p -> {
                        ExtensionTreatment resubmitTreatment = buildResubmitExtension(
                                deviceService.getDevice(deviceId), ROUTING_TABLE);
                        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                                .setIpDst(IpAddress.valueOf(podIp))
                                .setTcpDst(TpPort.tpPort(p.getPort()))
                                .extension(resubmitTreatment, deviceId)
                                .build();
                        buckets.add(buildGroupBucket(treatment, SELECT, (short) -1));

                        Set<Integer> existPorts = podIpPorts.get(podIp);
                        if (existPorts == null || existPorts.isEmpty()) {
                            existPorts = Sets.newConcurrentHashSet();
                        }
                        existPorts.add(p.getPort());
                        podIpPorts.put(podIp, existPorts);
                    });
                }
            }
        }

        if (!buckets.isEmpty()) {
            k8sGroupRuleService.setRule(appId, deviceId, groupId, SELECT, buckets, install);
            setShiftDomainRules(deviceId, SERVICE_TABLE, groupId,
                    PRIORITY_NAT_RULE, service, install);

            podIpPorts.forEach((k, v) ->
                v.forEach(p -> setUnshiftDomainRules(deviceId, POD_TABLE,
                        PRIORITY_NAT_RULE, service, k, p, install)));
        }
    }

    private void setShiftDomainRules(DeviceId deviceId, int installTable,
                                     int groupId, int priority,
                                     Service service, boolean install) {
        String serviceIp = service.getSpec().getClusterIP();
        // TODO: multi-ports case should be addressed
        Integer servicePort = service.getSpec().getPorts().get(0).getPort();

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_TCP)
                .matchIPDst(IpPrefix.valueOf(IpAddress.valueOf(serviceIp), 32))
                .matchTcpDst(TpPort.tpPort(servicePort))
                .build();

        ExtensionTreatment loadTreatment = buildLoadExtension(
                deviceService.getDevice(deviceId), "src", SHIFTED_IP_PREFIX);

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .extension(loadTreatment, deviceId)
                .group(GroupId.valueOf(groupId))
                .build();

        k8sFlowRuleService.setRule(
                appId,
                deviceId,
                selector,
                treatment,
                priority,
                installTable,
                install);
    }

    private void setUnshiftDomainRules(DeviceId deviceId, int installTable,
                                       int priority, Service service, String podIp,
                                       int podPort, boolean install) {
        String serviceIp = service.getSpec().getClusterIP();
        // TODO: multi-ports case should be addressed
        Integer servicePort = service.getSpec().getPorts().get(0).getPort();

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_TCP)
                .matchIPSrc(IpPrefix.valueOf(IpAddress.valueOf(podIp), 32))
                .matchTcpSrc(TpPort.tpPort(podPort))
                .build();

        ExtensionTreatment loadTreatment = buildLoadExtension(
                deviceService.getDevice(deviceId), "dst", "10.10");

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .extension(loadTreatment, deviceId)
                .setIpSrc(IpAddress.valueOf(serviceIp))
                .setTcpSrc(TpPort.tpPort(servicePort))
                .transition(ROUTING_TABLE)
                .build();

        k8sFlowRuleService.setRule(
                appId,
                deviceId,
                selector,
                treatment,
                priority,
                installTable,
                install);
    }

    private void setStatefulGroupFlowRules(DeviceId deviceId, long ctState,
                                           long ctMask, Service service,
                                           boolean install) {
        int groupId = (int) groupIdCounter.incrementAndGet();

        List<GroupBucket> buckets = Lists.newArrayList();

        String serviceName = service.getMetadata().getName();
        String serviceIp = service.getSpec().getClusterIP();

        // TODO: multi-ports case should be addressed
        Integer servicePort = service.getSpec().getPorts().get(0).getPort();

        List<Endpoints> endpointses = k8sEndpointsService.endpointses()
                .stream()
                .filter(ep -> serviceName.equals(ep.getMetadata().getName()))
                .collect(Collectors.toList());

        Map<String, String> nodeIpGatewayIpMap =
                nodeIpGatewayIpMap(k8sNodeService, k8sNetworkService);

        for (Endpoints endpoints : endpointses) {
            for (EndpointSubset endpointSubset : endpoints.getSubsets()) {
                List<EndpointPort> ports = endpointSubset.getPorts()
                        .stream()
                        .filter(p -> p.getProtocol().equals(TCP))
                        .collect(Collectors.toList());

                for (EndpointAddress address : endpointSubset.getAddresses()) {
                    String podIp = nodeIpGatewayIpMap.containsKey(address.getIp()) ?
                            nodeIpGatewayIpMap.get(address.getIp()) : address.getIp();

                    NiciraConnTrackTreatmentBuilder connTreatmentBuilder =
                            niciraConnTrackTreatmentBuilder(driverService, deviceId)
                                    .commit(true)
                                    .natAction(true)
                                    .natIp(IpAddress.valueOf(podIp))
                                    .natFlag(CT_NAT_DST_FLAG);

                    ports.forEach(p -> {
                        ExtensionTreatment ctNatTreatment = connTreatmentBuilder
                                .natPort(TpPort.tpPort(p.getPort())).build();
                        ExtensionTreatment resubmitTreatment = buildResubmitExtension(
                                deviceService.getDevice(deviceId), ROUTING_TABLE);
                        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                                .extension(ctNatTreatment, deviceId)
                                .extension(resubmitTreatment, deviceId)
                                .build();
                        buckets.add(buildGroupBucket(treatment, SELECT, (short) -1));
                    });
                }
            }
        }

        if (!buckets.isEmpty()) {
            k8sGroupRuleService.setRule(appId, deviceId, groupId, SELECT, buckets, install);

            setTrackNew(deviceId, ctState, ctMask, IpAddress.valueOf(serviceIp),
                    TpPort.tpPort(servicePort), NAT_TABLE, groupId,
                    PRIORITY_CT_RULE, install);
        }
    }

    private void setUntrack(DeviceId deviceId, long ctState, long ctMask,
                            String srcCidr, String dstCidr, int installTable,
                            int transitTable, int priority, boolean install) {
        ExtensionSelector esCtSate = RulePopulatorUtil
                .buildCtExtensionSelector(driverService, deviceId, ctState, ctMask);
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPSrc(IpPrefix.valueOf(srcCidr))
                .matchIPDst(IpPrefix.valueOf(dstCidr))
                .extension(esCtSate, deviceId)
                .build();

        NiciraConnTrackTreatmentBuilder connTreatmentBuilder =
                niciraConnTrackTreatmentBuilder(driverService, deviceId)
                        .natAction(false)
                        .commit(false)
                        .table((short) transitTable);

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .extension(connTreatmentBuilder.build(), deviceId)
                .build();

        k8sFlowRuleService.setRule(
                appId,
                deviceId,
                selector,
                treatment,
                priority,
                installTable,
                install);
    }

    private void setTrackNew(DeviceId deviceId, long ctState, long ctMask,
                             IpAddress dstIp, TpPort dstPort, int installTable,
                             int groupId, int priority, boolean install) {
        ExtensionSelector esCtSate = RulePopulatorUtil
                .buildCtExtensionSelector(driverService, deviceId, ctState, ctMask);
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(IpPrefix.valueOf(dstIp, HOST_CIDR_NUM))
                .matchIPProtocol(IPv4.PROTOCOL_TCP)
                .matchTcpDst(dstPort)
                .extension(esCtSate, deviceId)
                .build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .group(GroupId.valueOf(groupId))
                .build();

        k8sFlowRuleService.setRule(
                appId,
                deviceId,
                selector,
                treatment,
                priority,
                installTable,
                install);
    }

    private void setTrackEstablish(DeviceId deviceId, long ctState, long ctMask,
                                   int installTable, int transitTable,
                                   int priority, boolean install) {
        ExtensionSelector esCtSate = RulePopulatorUtil
                .buildCtExtensionSelector(driverService, deviceId, ctState, ctMask);
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .extension(esCtSate, deviceId)
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .transition(transitTable)
                .build();

        k8sFlowRuleService.setRule(
                appId,
                deviceId,
                selector,
                treatment,
                priority,
                installTable,
                install);
    }

    /**
     * Extracts properties from the component configuration context.
     *
     * @param context the component context
     */
    private void readComponentConfiguration(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();

        String updatedNatMode = Tools.get(properties, SERVICE_IP_NAT_MODE);
        serviceIpNatMode = updatedNatMode != null ?
                updatedNatMode : DEFAULT_SERVICE_IP_NAT_MODE_STR;
        log.info("Configured. Service IP NAT mode is {}", serviceIpNatMode);
    }

    private class InternalK8sServiceListener implements K8sServiceListener {

        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(K8sServiceEvent event) {
            switch (event.type()) {
                case K8S_SERVICE_CREATED:
                    eventExecutor.execute(() -> processServiceCreation(event.subject()));
                    break;
                case K8S_SERVICE_REMOVED:
                    eventExecutor.execute(() -> processServiceRemoval(event.subject()));
                    break;
                default:
                    // do nothing
                    break;
            }
        }

        private void processServiceCreation(Service service) {
            if (!isRelevantHelper()) {
                return;
            }

            long ctTrackNew = computeCtStateFlag(true, true, false);
            long ctMaskTrackNew = computeCtMaskFlag(true, true, false);

            k8sNodeService.completeNodes().forEach(n -> {
                if (NAT_STATEFUL.equals(serviceIpNatMode)) {
                    setStatefulGroupFlowRules(n.intgBridge(), ctTrackNew,
                            ctMaskTrackNew, service, true);
                } else if (NAT_STATELESS.equals(serviceIpNatMode)) {
                    setStatelessGroupFlowRules(n.intgBridge(), service, true);
                }
            });
        }

        private void processServiceRemoval(Service service) {
            if (!isRelevantHelper()) {
                return;
            }

            long ctTrackNew = computeCtStateFlag(true, true, false);
            long ctMaskTrackNew = computeCtMaskFlag(true, true, false);

            k8sNodeService.completeNodes().forEach(n -> {
                if (NAT_STATEFUL.equals(serviceIpNatMode)) {
                    setStatefulGroupFlowRules(n.intgBridge(), ctTrackNew,
                            ctMaskTrackNew, service, false);
                } else if (NAT_STATELESS.equals(serviceIpNatMode)) {
                    setStatelessGroupFlowRules(n.intgBridge(), service, false);
                }
            });
        }
    }

    private class InternalNodeEventListener implements K8sNodeListener {

        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(K8sNodeEvent event) {
            K8sNode k8sNode = event.subject();
            switch (event.type()) {
                case K8S_NODE_COMPLETE:
                    eventExecutor.execute(() -> processNodeCompletion(k8sNode));
                    break;
                case K8S_NODE_INCOMPLETE:
                    eventExecutor.execute(() -> processNodeIncompletion(k8sNode));
                    break;
                default:
                    break;
            }
        }

        private void processNodeCompletion(K8sNode node) {
            if (!isRelevantHelper()) {
                return;
            }

            if (NAT_STATEFUL.equals(serviceIpNatMode)) {
                setStatefulServiceNatRules(node.intgBridge(), true);
            } else if (NAT_STATELESS.equals(serviceIpNatMode)) {
                setStatelessServiceNatRules(node.intgBridge(), true);
            } else {
                log.warn("Service IP NAT mode was not configured!");
            }

        }

        private void processNodeIncompletion(K8sNode node) {
            if (!isRelevantHelper()) {
                return;
            }

            if (NAT_STATEFUL.equals(serviceIpNatMode)) {
                setStatefulServiceNatRules(node.intgBridge(), false);
            } else if (NAT_STATELESS.equals(serviceIpNatMode)) {
                setStatelessServiceNatRules(node.intgBridge(), false);
            } else {
                log.warn("Service IP NAT mode was not configured!");
            }

        }
    }
}
