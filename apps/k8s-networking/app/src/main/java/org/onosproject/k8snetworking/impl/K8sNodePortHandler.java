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

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.TpPort;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cfg.ConfigProperty;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.k8snetworking.api.K8sFlowRuleService;
import org.onosproject.k8snetworking.api.K8sNetworkService;
import org.onosproject.k8snetworking.api.K8sServiceEvent;
import org.onosproject.k8snetworking.api.K8sServiceListener;
import org.onosproject.k8snetworking.api.K8sServiceService;
import org.onosproject.k8snode.api.K8sNode;
import org.onosproject.k8snode.api.K8sNodeEvent;
import org.onosproject.k8snode.api.K8sNodeListener;
import org.onosproject.k8snode.api.K8sNodeService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static java.lang.Thread.sleep;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.k8snetworking.api.Constants.B_CLASS;
import static org.onosproject.k8snetworking.api.Constants.DST;
import static org.onosproject.k8snetworking.api.Constants.EXT_ENTRY_TABLE;
import static org.onosproject.k8snetworking.api.Constants.K8S_NETWORKING_APP_ID;
import static org.onosproject.k8snetworking.api.Constants.NODE_IP_PREFIX;
import static org.onosproject.k8snetworking.api.Constants.PRIORITY_CIDR_RULE;
import static org.onosproject.k8snetworking.api.Constants.PRIORITY_INTER_ROUTING_RULE;
import static org.onosproject.k8snetworking.api.Constants.PRIORITY_NODE_PORT_RULE;
import static org.onosproject.k8snetworking.api.Constants.ROUTING_TABLE;
import static org.onosproject.k8snetworking.api.Constants.SRC;
import static org.onosproject.k8snetworking.api.Constants.TUN_ENTRY_TABLE;
import static org.onosproject.k8snetworking.util.K8sNetworkingUtil.getBclassIpPrefixFromCidr;
import static org.onosproject.k8snetworking.util.K8sNetworkingUtil.getPropertyValue;
import static org.onosproject.k8snetworking.util.RulePopulatorUtil.buildLoadExtension;
import static org.onosproject.k8snode.api.K8sApiConfig.Mode.PASSTHROUGH;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides service port exposure using node port.
 */
@Component(immediate = true)
public class K8sNodePortHandler {

    private final Logger log = getLogger(getClass());

    private static final String NODE_PORT_TYPE = "NodePort";
    private static final String LOAD_BALANCER_TYPE = "LoadBalancer";
    private static final String TCP = "TCP";
    private static final String UDP = "UDP";
    private static final int HOST_CIDR = 32;
    private static final String SERVICE_CIDR = "serviceCidr";
    private static final String B_CLASS_SUFFIX = "0.0/16";

    private static final long SLEEP_MS = 3000;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sNodeService k8sNodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sNetworkService k8sNetworkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sServiceService k8sServiceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected K8sFlowRuleService k8sFlowRuleService;

    private final InternalK8sServiceListener k8sServiceListener =
            new InternalK8sServiceListener();
    private final InternalK8sNodeListener k8sNodeListener =
            new InternalK8sNodeListener();
    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));

    private ApplicationId appId;
    private NodeId localNodeId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(K8S_NETWORKING_APP_ID);

        localNodeId = clusterService.getLocalNode().id();
        leadershipService.runForLeadership(appId.name());
        k8sNodeService.addListener(k8sNodeListener);
        k8sServiceService.addListener(k8sServiceListener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        k8sNodeService.removeListener(k8sNodeListener);
        k8sServiceService.removeListener(k8sServiceListener);
        leadershipService.withdraw(appId.name());
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    private void processNodePortEvent(K8sNode k8sNode, Service service, boolean install) {

        String clusterIp = service.getSpec().getClusterIP();
        for (ServicePort servicePort : service.getSpec().getPorts()) {
            setNodeToServiceRules(k8sNode, clusterIp, servicePort, install);
            setServiceToNodeRules(k8sNode, clusterIp, servicePort, install);
        }
    }

    private void setIntgToExtRules(K8sNode k8sNode, String serviceCidr,
                                   boolean install) {
        // for local traffic, we add default flow rules for steering traffic from
        // integration bridge to external bridge through patch port
        // for remote traffic, we add default flow rules for steering traffic from
        // integration bridge to tun bridge through patch port
        k8sNodeService.completeNodes().forEach(n -> {
            String podCidr = k8sNetworkService.network(n.hostname()).cidr();
            String fullCidr = NODE_IP_PREFIX + "." +
                    podCidr.split("\\.")[2] + "." + B_CLASS_SUFFIX;

            TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                    .matchEthType(Ethernet.TYPE_IPV4)
                    .matchIPSrc(IpPrefix.valueOf(serviceCidr))
                    .matchIPDst(IpPrefix.valueOf(fullCidr));

            PortNumber output;
            if (n.hostname().equals(k8sNode.hostname())) {
                output = k8sNode.intgToExtPatchPortNum();
            } else {
                output = k8sNode.intgToTunPortNum();
            }

            TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder()
                    .setOutput(output);

            k8sFlowRuleService.setRule(
                    appId,
                    k8sNode.intgBridge(),
                    sBuilder.build(),
                    tBuilder.build(),
                    PRIORITY_CIDR_RULE,
                    ROUTING_TABLE,
                    install);
        });
    }

    private void setTunToIntgRules(K8sNode k8sNode, boolean install) {
        String podCidr = k8sNetworkService.network(k8sNode.hostname()).cidr();
        String fullCidr = NODE_IP_PREFIX + "." +
                podCidr.split("\\.")[2] + "." + B_CLASS_SUFFIX;

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(IpPrefix.valueOf(fullCidr))
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(k8sNode.tunToIntgPortNum())
                .build();

        k8sFlowRuleService.setRule(
                appId,
                k8sNode.tunBridge(),
                selector,
                treatment,
                PRIORITY_INTER_ROUTING_RULE,
                TUN_ENTRY_TABLE,
                install);
    }

    private void setNodeToServiceRules(K8sNode k8sNode,
                                       String clusterIp,
                                       ServicePort servicePort,
                                       boolean install) {
        String protocol = servicePort.getProtocol();
        int nodePort = servicePort.getNodePort();
        int svcPort = servicePort.getPort();
        DeviceId deviceId = k8sNode.extBridge();

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(IpPrefix.valueOf(k8sNode.nodeIp(), HOST_CIDR));

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder()
                .setIpDst(IpAddress.valueOf(clusterIp));

        if (TCP.equals(protocol)) {
            sBuilder.matchIPProtocol(IPv4.PROTOCOL_TCP)
                    .matchTcpDst(TpPort.tpPort(nodePort));
            tBuilder.setTcpDst(TpPort.tpPort(svcPort));
        } else if (UDP.equals(protocol)) {
            sBuilder.matchIPProtocol(IPv4.PROTOCOL_UDP)
                    .matchUdpDst(TpPort.tpPort(nodePort));
            tBuilder.setUdpDst(TpPort.tpPort(svcPort));
        }

        String podCidr = k8sNetworkService.network(k8sNode.hostname()).cidr();
        String prefix = NODE_IP_PREFIX + "." + podCidr.split("\\.")[2];

        ExtensionTreatment loadTreatment = buildLoadExtension(
                deviceService.getDevice(deviceId), B_CLASS, SRC, prefix);
        tBuilder.extension(loadTreatment, deviceId)
                .setOutput(k8sNode.extToIntgPatchPortNum());

        k8sFlowRuleService.setRule(
                appId,
                k8sNode.extBridge(),
                sBuilder.build(),
                tBuilder.build(),
                PRIORITY_NODE_PORT_RULE,
                EXT_ENTRY_TABLE,
                install);
    }

    private void setServiceToNodeRules(K8sNode k8sNode,
                                       String clusterIp,
                                       ServicePort servicePort,
                                       boolean install) {
        String protocol = servicePort.getProtocol();
        int nodePort = servicePort.getNodePort();
        int svcPort = servicePort.getPort();
        DeviceId deviceId = k8sNode.extBridge();

        String nodeIp = k8sNode.nodeIp().toString();
        String nodeIpPrefix = getBclassIpPrefixFromCidr(nodeIp);

        if (nodeIpPrefix == null) {
            return;
        }

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchInPort(k8sNode.extToIntgPatchPortNum())
                .matchIPSrc(IpPrefix.valueOf(IpAddress.valueOf(clusterIp), HOST_CIDR));

        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder()
                .setIpSrc(k8sNode.nodeIp())
                .setEthSrc(k8sNode.nodeMac());

        if (TCP.equals(protocol)) {
            sBuilder.matchIPProtocol(IPv4.PROTOCOL_TCP)
                    .matchTcpSrc(TpPort.tpPort(svcPort));
            tBuilder.setTcpSrc(TpPort.tpPort(nodePort));
        } else if (UDP.equals(protocol)) {
            sBuilder.matchIPProtocol(IPv4.PROTOCOL_UDP)
                    .matchUdpSrc(TpPort.tpPort(svcPort));
            tBuilder.setUdpSrc(TpPort.tpPort(nodePort));
        }

        ExtensionTreatment loadTreatment = buildLoadExtension(
                deviceService.getDevice(deviceId), B_CLASS, DST, nodeIpPrefix);
        tBuilder.extension(loadTreatment, deviceId);

        // in passthrough mode, we steer the traffic to the openstack intg bridge
        // in normal mode, we steer the traffic to the local port
        if (k8sNode.mode() == PASSTHROUGH) {
            PortNumber output = k8sNode.portNumByName(k8sNode.extBridge(),
                    k8sNode.k8sExtToOsPatchPortName());
            if (output == null) {
                log.warn("Kubernetes external to OpenStack patch port is null");
                return;
            }
            tBuilder.setOutput(output);
        } else {
            tBuilder.setOutput(PortNumber.LOCAL);
        }

        k8sFlowRuleService.setRule(
                appId,
                deviceId,
                sBuilder.build(),
                tBuilder.build(),
                PRIORITY_NODE_PORT_RULE,
                EXT_ENTRY_TABLE,
                install);
    }

    private String getServiceCidr() {
        Set<ConfigProperty> properties =
                configService.getProperties(K8sServiceHandler.class.getName());
        return getPropertyValue(properties, SERVICE_CIDR);
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
                default:
                    break;
            }
        }

        private void processServiceCreation(Service service) {
            if (!isRelevantHelper()) {
                return;
            }

            if (NODE_PORT_TYPE.equals(service.getSpec().getType()) ||
                    LOAD_BALANCER_TYPE.equals(service.getSpec().getType())) {
                k8sNodeService.completeNodes().forEach(n -> {
                    // we need to wait, until we resolve the valid MAC address of the node
                    while (k8sNodeService.node(n.hostname()).nodeMac() == null) {
                        log.warn("Node {} MAC address is not resolved, " +
                                 "wait until resolving it", n.hostname());
                        try {
                            sleep(SLEEP_MS);
                        } catch (InterruptedException e) {
                            log.error("Exception caused by", e);
                        }
                    }
                    K8sNode updatedNode = k8sNodeService.node(n.hostname());
                    processNodePortEvent(updatedNode, service, true);
                });
            }
        }
    }

    private class InternalK8sNodeListener implements K8sNodeListener {

        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(K8sNodeEvent event) {
            switch (event.type()) {
                case K8S_NODE_COMPLETE:
                    eventExecutor.execute(() -> processNodeCompletion(event.subject()));
                    break;
                case K8S_NODE_INCOMPLETE:
                default:
                    break;
            }
        }

        private void processNodeCompletion(K8sNode k8sNode) {
            if (!isRelevantHelper()) {
                return;
            }

            // we need to wait, until we resolve the valid MAC address of the node
            while (k8sNodeService.node(k8sNode.hostname()).nodeMac() == null) {
                log.warn("Node {} MAC address is not resolved, " +
                        "wait until resolving it", k8sNode.hostname());
                try {
                    sleep(SLEEP_MS);
                } catch (InterruptedException e) {
                    log.error("Exception caused by", e);
                }
            }

            K8sNode updatedNode = k8sNodeService.node(k8sNode.hostname());

            k8sServiceService.services().stream()
                    .filter(s -> NODE_PORT_TYPE.equals(s.getSpec().getType()) ||
                            LOAD_BALANCER_TYPE.equals(s.getSpec().getType()))
                    .forEach(s -> processNodePortEvent(updatedNode, s, true));

            setIntgToExtRules(updatedNode, getServiceCidr(), true);
            setTunToIntgRules(updatedNode, true);
        }
    }
}
