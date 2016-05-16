/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.cordvtn.impl;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onosproject.cordvtn.api.CordVtnConfig;
import org.onosproject.cordvtn.api.CordVtnNode;
import org.onosproject.cordvtn.api.CordVtnService;
import org.onosproject.cordvtn.api.Instance;
import org.onosproject.cordvtn.api.InstanceHandler;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.Host;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.host.HostListener;
import org.onosproject.net.host.HostService;
import org.onosproject.xosclient.api.OpenStackAccess;
import org.onosproject.xosclient.api.VtnService;
import org.onosproject.xosclient.api.VtnServiceApi;
import org.onosproject.xosclient.api.VtnServiceId;
import org.onosproject.xosclient.api.XosAccess;
import org.onosproject.xosclient.api.XosClientService;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.cordvtn.impl.CordVtnPipeline.*;
import static org.onosproject.xosclient.api.VtnService.NetworkType.MANAGEMENT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides default virtual network connectivity for service instances.
 */
@Component(immediate = true)
public abstract class CordVtnInstanceHandler implements InstanceHandler {

    protected final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry configRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected XosClientService xosClient;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CordVtnNodeManager nodeManager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CordVtnPipeline pipeline;

    protected static final String OPENSTACK_ACCESS_ERROR = "OpenStack access is not configured";
    protected static final String XOS_ACCESS_ERROR = "XOS access is not configured";

    protected XosAccess xosAccess = null;
    protected OpenStackAccess osAccess = null;
    protected ApplicationId appId;
    protected VtnService.ServiceType serviceType;
    protected ExecutorService eventExecutor;

    protected HostListener hostListener = new InternalHostListener();
    protected NetworkConfigListener configListener = new InternalConfigListener();

    protected void activate() {
        // sub class should set service type and event executor in its activate method
        appId = coreService.registerApplication(CordVtnService.CORDVTN_APP_ID);

        hostService.addListener(hostListener);
        configRegistry.addListener(configListener);

        log.info("Started");
    }

    protected void deactivate() {
        hostService.removeListener(hostListener);
        configRegistry.removeListener(configListener);
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    @Override
    public void instanceDetected(Instance instance) {
        log.info("Instance is detected {}", instance);

        VtnService service = getVtnService(instance.serviceId());
        if (service == null) {
            log.warn("Failed to get VtnService for {}", instance);
            return;
        }

        if (service.networkType().equals(MANAGEMENT)) {
            managementNetworkRules(instance, service, true);
        }

        defaultConnectionRules(instance, service, true);
    }

    @Override
    public void instanceRemoved(Instance instance) {
        log.info("Instance is removed {}", instance);

        VtnService service = getVtnService(instance.serviceId());
        if (service == null) {
            log.warn("Failed to get VtnService for {}", instance);
            return;
        }

        if (service.networkType().equals(MANAGEMENT)) {
            managementNetworkRules(instance, service, false);
        }

        // TODO check if any stale management network rules are
        defaultConnectionRules(instance, service, false);
    }

    protected VtnService getVtnService(VtnServiceId serviceId) {
        checkNotNull(osAccess, OPENSTACK_ACCESS_ERROR);
        checkNotNull(xosAccess, XOS_ACCESS_ERROR);

        // TODO remove openstack access when XOS provides all information
        VtnServiceApi serviceApi = xosClient.getClient(xosAccess).vtnService();
        VtnService service = serviceApi.service(serviceId, osAccess);
        if (service == null) {
            log.warn("Failed to get VtnService for {}", serviceId);
        }
        return service;
    }

    protected Set<Instance> getInstances(VtnServiceId serviceId) {
        return StreamSupport.stream(hostService.getHosts().spliterator(), false)
                .filter(host -> Objects.equals(
                        serviceId.id(),
                        host.annotations().value(Instance.SERVICE_ID)))
                .map(Instance::of)
                .collect(Collectors.toSet());
    }

    private void defaultConnectionRules(Instance instance, VtnService service, boolean install) {
        long vni = service.vni();
        Ip4Prefix serviceIpRange = service.subnet().getIp4Prefix();

        inPortRule(instance, install);
        dstIpRule(instance, vni, install);
        tunnelInRule(instance, vni, install);

        if (install) {
            directAccessRule(serviceIpRange, serviceIpRange, true);
            serviceIsolationRule(serviceIpRange, true);
        } else if (getInstances(service.id()).isEmpty()) {
            directAccessRule(serviceIpRange, serviceIpRange, false);
            serviceIsolationRule(serviceIpRange, false);
        }
    }

    private void managementNetworkRules(Instance instance, VtnService service, boolean install) {

        managementPerInstanceRule(instance, install);
        if (install) {
            managementBaseRule(instance, service, true);
        } else if (!hostService.getConnectedHosts(instance.deviceId()).stream()
                .filter(host -> Instance.of(host).serviceId().equals(service.id()))
                .findAny()
                .isPresent()) {
            managementBaseRule(instance, service, false);
        }
    }

    private void managementBaseRule(Instance instance, VtnService service, boolean install) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_ARP)
                .matchArpTpa(service.serviceIp().getIp4Address())
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.LOCAL)
                .build();

        FlowRule flowRule = DefaultFlowRule.builder()
                .fromApp(appId)
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(PRIORITY_MANAGEMENT)
                .forDevice(instance.deviceId())
                .forTable(TABLE_ZERO)
                .makePermanent()
                .build();

        pipeline.processFlowRule(install, flowRule);

        selector = DefaultTrafficSelector.builder()
                .matchInPort(PortNumber.LOCAL)
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(service.subnet())
                .build();

        treatment = DefaultTrafficTreatment.builder()
                .transition(TABLE_DST_IP)
                .build();

        flowRule = DefaultFlowRule.builder()
                .fromApp(appId)
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(PRIORITY_MANAGEMENT)
                .forDevice(instance.deviceId())
                .forTable(TABLE_ZERO)
                .makePermanent()
                .build();

        pipeline.processFlowRule(install, flowRule);

        selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(service.serviceIp().toIpPrefix())
                .build();

        treatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.LOCAL)
                .build();

        flowRule = DefaultFlowRule.builder()
                .fromApp(appId)
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(PRIORITY_MANAGEMENT)
                .forDevice(instance.deviceId())
                .forTable(TABLE_ACCESS_TYPE)
                .makePermanent()
                .build();

        pipeline.processFlowRule(install, flowRule);
    }

    private void managementPerInstanceRule(Instance instance, boolean install) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchInPort(PortNumber.LOCAL)
                .matchEthType(Ethernet.TYPE_ARP)
                .matchArpTpa(instance.ipAddress().getIp4Address())
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(instance.portNumber())
                .build();

        FlowRule flowRule = DefaultFlowRule.builder()
                .fromApp(appId)
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(PRIORITY_MANAGEMENT)
                .forDevice(instance.deviceId())
                .forTable(TABLE_ZERO)
                .makePermanent()
                .build();

        pipeline.processFlowRule(install, flowRule);
    }

    private void inPortRule(Instance instance, boolean install) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchInPort(instance.portNumber())
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPSrc(instance.ipAddress().toIpPrefix())
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .transition(TABLE_ACCESS_TYPE)
                .build();


        FlowRule flowRule = DefaultFlowRule.builder()
                .fromApp(appId)
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(PRIORITY_DEFAULT)
                .forDevice(instance.deviceId())
                .forTable(TABLE_IN_PORT)
                .makePermanent()
                .build();

        pipeline.processFlowRule(install, flowRule);

        selector = DefaultTrafficSelector.builder()
                .matchInPort(instance.portNumber())
                .build();

        treatment = DefaultTrafficTreatment.builder()
                .transition(TABLE_IN_SERVICE)
                .build();

        flowRule = DefaultFlowRule.builder()
                .fromApp(appId)
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(PRIORITY_LOW)
                .forDevice(instance.deviceId())
                .forTable(TABLE_IN_PORT)
                .makePermanent()
                .build();

        pipeline.processFlowRule(install, flowRule);
    }

    private void dstIpRule(Instance instance, long vni, boolean install) {
        Ip4Address tunnelIp = nodeManager.dpIp(instance.deviceId()).getIp4Address();

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(instance.ipAddress().toIpPrefix())
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setEthDst(instance.mac())
                .setOutput(instance.portNumber())
                .build();

        FlowRule flowRule = DefaultFlowRule.builder()
                .fromApp(appId)
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(PRIORITY_DEFAULT)
                .forDevice(instance.deviceId())
                .forTable(TABLE_DST_IP)
                .makePermanent()
                .build();

        pipeline.processFlowRule(install, flowRule);

        for (CordVtnNode node : nodeManager.completeNodes()) {
            if (node.intBrId().equals(instance.deviceId())) {
                continue;
            }

            ExtensionTreatment tunnelDst = pipeline.tunnelDstTreatment(node.intBrId(), tunnelIp);
            if (tunnelDst == null) {
                continue;
            }

            treatment = DefaultTrafficTreatment.builder()
                    .setEthDst(instance.mac())
                    .setTunnelId(vni)
                    .extension(tunnelDst, node.intBrId())
                    .setOutput(nodeManager.tunnelPort(node.intBrId()))
                    .build();

            flowRule = DefaultFlowRule.builder()
                    .fromApp(appId)
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .withPriority(PRIORITY_DEFAULT)
                    .forDevice(node.intBrId())
                    .forTable(TABLE_DST_IP)
                    .makePermanent()
                    .build();

            pipeline.processFlowRule(install, flowRule);
        }
    }

    private void tunnelInRule(Instance instance, long vni, boolean install) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchTunnelId(vni)
                .matchEthDst(instance.mac())
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(instance.portNumber())
                .build();

        FlowRule flowRule = DefaultFlowRule.builder()
                .fromApp(appId)
                .withSelector(selector)
                .withTreatment(treatment)
                .withPriority(PRIORITY_DEFAULT)
                .forDevice(instance.deviceId())
                .forTable(TABLE_TUNNEL_IN)
                .makePermanent()
                .build();

        pipeline.processFlowRule(install, flowRule);
    }

    private void directAccessRule(Ip4Prefix srcRange, Ip4Prefix dstRange, boolean install) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPSrc(srcRange)
                .matchIPDst(dstRange)
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .transition(TABLE_DST_IP)
                .build();


        nodeManager.completeNodes().stream().forEach(node -> {
            FlowRule flowRuleDirect = DefaultFlowRule.builder()
                    .fromApp(appId)
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .withPriority(PRIORITY_DEFAULT)
                    .forDevice(node.intBrId())
                    .forTable(TABLE_ACCESS_TYPE)
                    .makePermanent()
                    .build();

            pipeline.processFlowRule(install, flowRuleDirect);
        });
    }

    private void serviceIsolationRule(Ip4Prefix dstRange, boolean install) {
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(dstRange)
                .build();

        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .drop()
                .build();

        nodeManager.completeNodes().stream().forEach(node -> {
            FlowRule flowRuleDirect = DefaultFlowRule.builder()
                    .fromApp(appId)
                    .withSelector(selector)
                    .withTreatment(treatment)
                    .withPriority(PRIORITY_LOW)
                    .forDevice(node.intBrId())
                    .forTable(TABLE_ACCESS_TYPE)
                    .makePermanent()
                    .build();

            pipeline.processFlowRule(install, flowRuleDirect);
        });
    }

    protected void readConfiguration() {
        CordVtnConfig config = configRegistry.getConfig(appId, CordVtnConfig.class);
        if (config == null) {
            log.debug("No configuration found");
            return;
        }
        osAccess = config.openstackAccess();
        xosAccess = config.xosAccess();
    }

    public class InternalHostListener implements HostListener {

        @Override
        public void event(HostEvent event) {
            Host host = event.subject();
            if (!mastershipService.isLocalMaster(host.location().deviceId())) {
                // do not allow to proceed without mastership
                return;
            }

            Instance instance = Instance.of(host);
            if (!Objects.equals(instance.serviceType(), serviceType)) {
                // not my service instance, do nothing
                return;
            }

            switch (event.type()) {
                case HOST_UPDATED:
                case HOST_ADDED:
                    eventExecutor.execute(() -> instanceDetected(instance));
                    break;
                case HOST_REMOVED:
                    eventExecutor.execute(() -> instanceRemoved(instance));
                    break;
                default:
                    break;
            }
        }
    }

    public class InternalConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            if (!event.configClass().equals(CordVtnConfig.class)) {
                return;
            }

            switch (event.type()) {
                case CONFIG_ADDED:
                case CONFIG_UPDATED:
                    readConfiguration();
                    break;
                default:
                    break;
            }
        }
    }
}
