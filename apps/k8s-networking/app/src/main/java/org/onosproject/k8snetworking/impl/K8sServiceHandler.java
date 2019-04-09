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
import io.fabric8.kubernetes.api.model.EndpointAddress;
import io.fabric8.kubernetes.api.model.EndpointPort;
import io.fabric8.kubernetes.api.model.EndpointSubset;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
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
import org.onosproject.k8snetworking.api.K8sPodEvent;
import org.onosproject.k8snetworking.api.K8sPodListener;
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
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
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
import static org.onosproject.k8snetworking.api.Constants.DST;
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
import static org.onosproject.k8snetworking.api.Constants.SRC;
import static org.onosproject.k8snetworking.impl.OsgiPropertyConstants.SERVICE_IP_NAT_MODE;
import static org.onosproject.k8snetworking.impl.OsgiPropertyConstants.SERVICE_IP_NAT_MODE_DEFAULT;
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
@Component(
    immediate = true,
    property = {
            SERVICE_IP_NAT_MODE + "=" + SERVICE_IP_NAT_MODE_DEFAULT
    }
)
public class K8sServiceHandler {

    private final Logger log = getLogger(getClass());

    private static final int HOST_CIDR_NUM = 32;

    private static final String NONE = "None";
    private static final String CLUSTER_IP = "ClusterIP";
    private static final String TCP = "TCP";
    private static final String UDP = "UDP";

    private static final String GROUP_ID_COUNTER_NAME = "group-id-counter";

    private static final String IP_ADDRESS = "ipAddress";

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sNetworkService k8sNetworkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sFlowRuleService k8sFlowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sGroupRuleService k8sGroupRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sNodeService k8sNodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sEndpointsService k8sEndpointsService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sServiceService k8sServiceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sPodService k8sPodService;

    /** Service IP address translation mode. */
    private String serviceIpNatMode = SERVICE_IP_NAT_MODE_DEFAULT;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));
    private final InternalNodeEventListener internalNodeEventListener =
            new InternalNodeEventListener();
    private final InternalK8sServiceListener internalK8sServiceListener =
            new InternalK8sServiceListener();
    private final InternalK8sPodListener internalK8sPodListener =
            new InternalK8sPodListener();

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
        k8sPodService.addListener(internalK8sPodListener);

        groupIdCounter = storageService.getAtomicCounter(GROUP_ID_COUNTER_NAME);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        leadershipService.withdraw(appId.name());
        k8sPodService.removeListener(internalK8sPodListener);
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

    private String servicePortStr(String ip, int port, String protocol) {
        return ip + "_" + port + "_" + protocol;
    }

    /**
     * Obtains the service port to endpoint address paired map.
     *
     * @param service   kubernetes service
     * @return a map where key is kubernetes service port, and value is the
     * endpoint addresses that are associated with the service port
     */
    private Map<ServicePort, Set<String>> getSportEpAddressMap(Service service) {

        Map<ServicePort, Set<String>> map = Maps.newConcurrentMap();

        String serviceName = service.getMetadata().getName();
        List<Endpoints> endpointses = k8sEndpointsService.endpointses()
                .stream()
                .filter(ep -> serviceName.equals(ep.getMetadata().getName()))
                .collect(Collectors.toList());

        service.getSpec().getPorts().stream()
                .filter(Objects::nonNull)
                .filter(sp -> sp.getTargetPort() != null)
                .filter(sp -> sp.getTargetPort().getIntVal() != null)
                .forEach(sp -> {
            int targetPort = sp.getTargetPort().getIntVal();
            String targetProtocol = sp.getProtocol();

            for (Endpoints endpoints : endpointses) {
                for (EndpointSubset endpointSubset : endpoints.getSubsets()) {
                    for (EndpointPort endpointPort : endpointSubset.getPorts()) {
                        if (targetProtocol.equals(endpointPort.getProtocol()) &&
                                targetPort == endpointPort.getPort()) {
                            Set<String> addresses = endpointSubset.getAddresses()
                                    .stream().map(EndpointAddress::getIp)
                                    .collect(Collectors.toSet());
                            map.put(sp, addresses);
                        }
                    }
                }
            }
        });

        return map;
    }

    private void setGroupBuckets(DeviceId deviceId, Service service, Pod pod, boolean install) {

        if (pod.getMetadata().getAnnotations() == null) {
            return;
        }

        String podIpStr = pod.getMetadata().getAnnotations().get(IP_ADDRESS);

        Map<ServicePort, Set<String>> spEpasMap = getSportEpAddressMap(service);
        Map<ServicePort, List<GroupBucket>> spGrpBkts = Maps.newConcurrentMap();

        spEpasMap.forEach((sp, epas) -> {
            List<GroupBucket> bkts = Lists.newArrayList();

            if (install) {
                if (epas.contains(podIpStr)) {
                    bkts = buildBuckets(deviceId, podIpStr, sp);
                }
            } else {
                bkts = buildBuckets(deviceId, podIpStr, sp);
            }

            spGrpBkts.put(sp, bkts);
        });

        String serviceIp = service.getSpec().getClusterIP();
        spGrpBkts.forEach((sp, bkts) -> {
            String svcStr = servicePortStr(serviceIp, sp.getPort(), sp.getProtocol());
            int groupId = svcStr.hashCode();

            k8sGroupRuleService.setBuckets(appId, deviceId, groupId, bkts, install);
        });
    }

    private List<GroupBucket> buildBuckets(DeviceId deviceId,
                                           String podIpStr,
                                           ServicePort sp) {
        List<GroupBucket> bkts = Lists.newArrayList();

        ExtensionTreatment resubmitTreatment = buildResubmitExtension(
                deviceService.getDevice(deviceId), ROUTING_TABLE);
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder()
                .setIpDst(IpAddress.valueOf(podIpStr))
                .extension(resubmitTreatment, deviceId);

        if (TCP.equals(sp.getProtocol())) {
            tBuilder.setTcpDst(TpPort.tpPort(sp.getTargetPort().getIntVal()));
        } else if (UDP.equals(sp.getProtocol())) {
            tBuilder.setUdpDst(TpPort.tpPort(sp.getTargetPort().getIntVal()));
        }

        bkts.add(buildGroupBucket(tBuilder.build(), SELECT, (short) -1));

        return bkts;
    }

    private synchronized void setStatelessGroupFlowRules(DeviceId deviceId,
                                                         Service service,
                                                         boolean install) {
        Map<ServicePort, Set<String>> spEpasMap = getSportEpAddressMap(service);
        Map<String, String> nodeIpGatewayIpMap =
                nodeIpGatewayIpMap(k8sNodeService, k8sNetworkService);
        Map<ServicePort, List<GroupBucket>> spGrpBkts = Maps.newConcurrentMap();

        spEpasMap.forEach((sp, epas) -> {
            List<GroupBucket> bkts = Lists.newArrayList();
            epas.forEach(epa -> {
                String podIp = nodeIpGatewayIpMap.getOrDefault(epa, epa);
                ExtensionTreatment resubmitTreatment = buildResubmitExtension(
                        deviceService.getDevice(deviceId), ROUTING_TABLE);
                TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder()
                        .setIpDst(IpAddress.valueOf(podIp))
                        .extension(resubmitTreatment, deviceId);

                if (TCP.equals(sp.getProtocol())) {
                    tBuilder.setTcpDst(TpPort.tpPort(sp.getTargetPort().getIntVal()));
                } else if (UDP.equals(sp.getProtocol())) {
                    tBuilder.setUdpDst(TpPort.tpPort(sp.getTargetPort().getIntVal()));
                }

                bkts.add(buildGroupBucket(tBuilder.build(), SELECT, (short) -1));
            });
            spGrpBkts.put(sp, bkts);
        });

        String serviceIp = service.getSpec().getClusterIP();
        spGrpBkts.forEach((sp, bkts) -> {
            String svcStr = servicePortStr(serviceIp, sp.getPort(), sp.getProtocol());
            int groupId = svcStr.hashCode();

            if (install) {

                // add group table rules
                k8sGroupRuleService.setRule(appId, deviceId, groupId,
                        SELECT, bkts, true);

                log.info("Adding group rule {}", groupId);

                // if we failed to add group rule, we will not install flow rules
                // as this might cause rule inconsistency
                if (k8sGroupRuleService.hasGroup(deviceId, groupId)) {
                    // add flow rules for shifting IP domain
                    setShiftDomainRules(deviceId, SERVICE_TABLE, groupId,
                            PRIORITY_NAT_RULE, serviceIp, sp.getPort(),
                            sp.getProtocol(), true);
                }
            } else {
                // remove flow rules for shifting IP domain
                setShiftDomainRules(deviceId, SERVICE_TABLE, groupId,
                        PRIORITY_NAT_RULE, serviceIp, sp.getPort(),
                        sp.getProtocol(), false);

                // remove group table rules
                k8sGroupRuleService.setRule(appId, deviceId, groupId,
                        SELECT, bkts, false);

                log.info("Removing group rule {}", groupId);
            }
        });

        spEpasMap.forEach((sp, epas) ->
            // add flow rules for unshifting IP domain
            epas.forEach(epa -> {
                String podIp = nodeIpGatewayIpMap.getOrDefault(epa, epa);
                setUnshiftDomainRules(deviceId, POD_TABLE,
                PRIORITY_NAT_RULE, serviceIp, sp.getPort(), sp.getProtocol(),
                        podIp, sp.getTargetPort().getIntVal(), install);
            }
        ));
    }

    private void setShiftDomainRules(DeviceId deviceId, int installTable,
                                     int groupId, int priority, String serviceIp,
                                     int servicePort, String protocol, boolean install) {
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(IpPrefix.valueOf(IpAddress.valueOf(serviceIp), HOST_CIDR_NUM));

        if (TCP.equals(protocol)) {
            sBuilder.matchIPProtocol(IPv4.PROTOCOL_TCP)
                    .matchTcpDst(TpPort.tpPort(servicePort));
        } else if (UDP.equals(protocol)) {
            sBuilder.matchIPProtocol(IPv4.PROTOCOL_UDP)
                    .matchUdpDst(TpPort.tpPort(servicePort));
        }

        ExtensionTreatment loadTreatment = buildLoadExtension(
                deviceService.getDevice(deviceId), SRC, SHIFTED_IP_PREFIX);

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .extension(loadTreatment, deviceId)
                .group(GroupId.valueOf(groupId))
                .build();

        k8sFlowRuleService.setRule(
                appId,
                deviceId,
                sBuilder.build(),
                treatment,
                priority,
                installTable,
                install);
    }

    private void setUnshiftDomainRules(DeviceId deviceId, int installTable,
                                       int priority, String serviceIp,
                                       int servicePort, String protocol,
                                       String podIp, int podPort, boolean install) {
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPSrc(IpPrefix.valueOf(IpAddress.valueOf(podIp), HOST_CIDR_NUM));

        if (TCP.equals(protocol)) {
            sBuilder.matchIPProtocol(IPv4.PROTOCOL_TCP)
                    .matchTcpSrc(TpPort.tpPort(podPort));
        } else if (UDP.equals(protocol)) {
            sBuilder.matchIPProtocol(IPv4.PROTOCOL_UDP)
                    .matchUdpSrc(TpPort.tpPort(podPort));
        }

        String podIpPrefix = podIp.split("\\.")[0] +
                                            "." + podIp.split("\\.")[1];

        ExtensionTreatment loadTreatment = buildLoadExtension(
                deviceService.getDevice(deviceId), DST, podIpPrefix);

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder()
                .extension(loadTreatment, deviceId)
                .setIpSrc(IpAddress.valueOf(serviceIp))
                .transition(ROUTING_TABLE);

        if (TCP.equals(protocol)) {
            tBuilder.setTcpSrc(TpPort.tpPort(servicePort));
        } else if (UDP.equals(protocol)) {
            tBuilder.setUdpSrc(TpPort.tpPort(servicePort));
        }

        k8sFlowRuleService.setRule(
                appId,
                deviceId,
                sBuilder.build(),
                tBuilder.build(),
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
        serviceIpNatMode = updatedNatMode != null ? updatedNatMode : SERVICE_IP_NAT_MODE_DEFAULT;
        log.info("Configured. Service IP NAT mode is {}", serviceIpNatMode);
    }

    private void setServiceNatRules(DeviceId deviceId, boolean install) {
        if (NAT_STATEFUL.equals(serviceIpNatMode)) {
            setStatefulServiceNatRules(deviceId, install);
        } else if (NAT_STATELESS.equals(serviceIpNatMode)) {
            setStatelessServiceNatRules(deviceId, install);
        } else {
            log.warn("Service IP NAT mode was not configured!");
        }
    }

    private class InternalK8sServiceListener implements K8sServiceListener {

        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(K8sServiceEvent event) {
            switch (event.type()) {
                case K8S_SERVICE_CREATED:
                case K8S_SERVICE_UPDATED:
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

            if (NAT_STATEFUL.equals(serviceIpNatMode)) {
                long ctTrackNew = computeCtStateFlag(true, true, false);
                long ctMaskTrackNew = computeCtMaskFlag(true, true, false);

                k8sNodeService.completeNodes().forEach(n ->
                        setStatefulGroupFlowRules(n.intgBridge(), ctTrackNew,
                                ctMaskTrackNew, service, true));
            } else if (NAT_STATELESS.equals(serviceIpNatMode)) {
                k8sNodeService.completeNodes().forEach(n ->
                        setStatelessGroupFlowRules(n.intgBridge(), service, true));
            }
        }

        private void processServiceRemoval(Service service) {
            if (!isRelevantHelper()) {
                return;
            }

            if (NAT_STATEFUL.equals(serviceIpNatMode)) {
                long ctTrackNew = computeCtStateFlag(true, true, false);
                long ctMaskTrackNew = computeCtMaskFlag(true, true, false);

                k8sNodeService.completeNodes().forEach(n ->
                        setStatefulGroupFlowRules(n.intgBridge(), ctTrackNew,
                                ctMaskTrackNew, service, false));
            } else if (NAT_STATELESS.equals(serviceIpNatMode)) {
                k8sNodeService.completeNodes().forEach(n ->
                        setStatelessGroupFlowRules(n.intgBridge(), service, false));
            }
        }
    }

    private class InternalK8sPodListener implements K8sPodListener {

        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(K8sPodEvent event) {
            switch (event.type()) {
                case K8S_POD_UPDATED:
                    eventExecutor.execute(() -> processPodUpdate(event.subject()));
                    break;
                case K8S_POD_REMOVED:
                    eventExecutor.execute(() -> processPodRemoval(event.subject()));
                    break;
                default:
                    break;
            }
        }

        private void processPodUpdate(Pod pod) {
            if (!isRelevantHelper()) {
                return;
            }

            setServiceRuleFromPod(pod, true);
        }

        private void processPodRemoval(Pod pod) {
            if (!isRelevantHelper()) {
                return;
            }

            setServiceRuleFromPod(pod, false);
        }

        private void setServiceRuleFromPod(Pod pod, boolean install) {
            k8sServiceService.services().forEach(s -> {
                pod.getMetadata().getLabels().forEach((pk, pv) -> {
                    Map<String, String> selectors = s.getSpec().getSelector();
                    if (selectors != null && selectors.containsKey(pk)) {
                        if (pv.equals(selectors.get(pk))) {
                            k8sNodeService.completeNodes().forEach(n ->
                                setGroupBuckets(n.intgBridge(), s, pod, install));
                        }
                    }
                });
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
                case K8S_NODE_REMOVED:
                default:
                    break;
            }
        }

        private void processNodeCompletion(K8sNode node) {
            if (!isRelevantHelper()) {
                return;
            }

            setServiceNatRules(node.intgBridge(), true);
        }
    }
}
