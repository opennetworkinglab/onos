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
package org.onosproject.openstacknetworking.routing;

import com.google.common.collect.ImmutableSet;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.util.Tools;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.GroupId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.openstackinterface.OpenstackInterfaceService;
import org.onosproject.openstackinterface.OpenstackNetwork;
import org.onosproject.openstackinterface.OpenstackPort;
import org.onosproject.openstackinterface.OpenstackRouter;
import org.onosproject.openstackinterface.OpenstackRouterInterface;
import org.onosproject.openstackinterface.OpenstackSubnet;
import org.onosproject.openstacknetworking.AbstractVmHandler;
import org.onosproject.openstacknetworking.Constants;
import org.onosproject.openstacknetworking.OpenstackRoutingService;
import org.onosproject.openstacknetworking.RulePopulatorUtil;
import org.onosproject.openstacknode.OpenstackNode;
import org.onosproject.openstacknode.OpenstackNodeEvent;
import org.onosproject.openstacknode.OpenstackNodeListener;
import org.onosproject.openstacknode.OpenstackNodeService;
import org.onosproject.scalablegateway.api.GatewayNode;
import org.onosproject.scalablegateway.api.ScalableGatewayService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.openstacknetworking.Constants.*;
import static org.onosproject.openstacknetworking.RulePopulatorUtil.buildExtension;
import static org.onosproject.openstacknode.OpenstackNodeService.NodeType.COMPUTE;
import static org.onosproject.openstacknode.OpenstackNodeService.NodeType.GATEWAY;

@Component(immediate = true)
@Service
public class OpenstackRoutingManager extends AbstractVmHandler implements OpenstackRoutingService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackInterfaceService openstackService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackNodeService nodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ScalableGatewayService gatewayService;

    private final ExecutorService eventExecutor = newSingleThreadScheduledExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler", log));
    private final InternalNodeListener nodeListener = new InternalNodeListener();

    private ApplicationId appId;

    @Activate
    protected void activate() {
        super.activate();
        appId = coreService.registerApplication(ROUTING_APP_ID);
        nodeService.addListener(nodeListener);
    }

    @Deactivate
    protected void deactivate() {
        nodeService.removeListener(nodeListener);
        log.info("stopped");
    }

    @Override
    public void createRouter(OpenstackRouter osRouter) {
    }

    @Override
    public void updateRouter(OpenstackRouter osRouter) {
        if (osRouter.gatewayExternalInfo().externalFixedIps().size() > 0) {
            openstackService.ports().stream()
                    .filter(osPort -> osPort.deviceOwner().equals(DEVICE_OWNER_ROUTER_INTERFACE) &&
                            osPort.deviceId().equals(osRouter.id()))
                    .forEach(osPort -> {
                        String subnetId = osPort.fixedIps().keySet().stream().findFirst().get();
                        setExternalConnection(osRouter, subnetId);
                    });

            log.info("Connected external gateway {} to router {}",
                     osRouter.gatewayExternalInfo().externalFixedIps(),
                     osRouter.name());
        } else {
            openstackService.ports().stream()
                    .filter(osPort -> osPort.deviceOwner().equals(DEVICE_OWNER_ROUTER_INTERFACE) &&
                            osPort.deviceId().equals(osRouter.id()))
                    .forEach(osPort -> unsetExternalConnection(osRouter, osPort.networkId()));

            log.info("Disconnected external gateway from router {}",
                     osRouter.name());
        }
    }

    @Override
    public void removeRouter(String osRouterId) {
        // TODO implement this
    }

    @Override
    public void addRouterInterface(OpenstackRouterInterface routerIface) {
        OpenstackRouter osRouter = openstackRouter(routerIface.id());
        OpenstackPort osPort = openstackService.port(routerIface.portId());
        if (osRouter == null || osPort == null) {
            log.warn("Failed to add router interface {}", routerIface);
            return;
        }

        setGatewayIcmp(Ip4Address.valueOf(openstackService.subnet(routerIface.subnetId()).gatewayIp()));

        setRoutes(osRouter, Optional.empty());
        if (osRouter.gatewayExternalInfo().externalFixedIps().size() > 0) {
            String subnetId = osPort.fixedIps().keySet().stream().findFirst().get();
            setExternalConnection(osRouter, subnetId);
        }
        log.info("Connected {} to router {}", osPort.fixedIps(), osRouter.name());
    }

    @Override
    public void removeRouterInterface(OpenstackRouterInterface routerIface) {
        OpenstackRouter osRouter = openstackService.router(routerIface.id());
        if (osRouter == null) {
            log.warn("Failed to remove router interface {}", routerIface);
            return;
        }

        OpenstackSubnet osSubnet = openstackService.subnet(routerIface.subnetId());
        OpenstackNetwork osNet = openstackService.network(osSubnet.networkId());

        unsetGatewayIcmp(Ip4Address.valueOf(openstackService.subnet(routerIface.subnetId()).gatewayIp()));

        unsetRoutes(osRouter, osSubnet);

        if (osRouter.gatewayExternalInfo().externalFixedIps().size() > 0) {
            unsetExternalConnection(osRouter, osNet.id());
        }
        log.info("Disconnected {} from router {}", osSubnet.cidr(), osRouter.name());
    }

    private void setGatewayIcmp(Ip4Address gatewayIp) {
        if (gatewayIp == null) {
            return;
        }
        gatewayService.getGatewayDeviceIds().stream().forEach(deviceId -> {
            populateGatewayIcmpRule(gatewayIp, deviceId);
        });
    }

    private void populateGatewayIcmpRule(Ip4Address gatewayIp, DeviceId deviceId) {
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        sBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_ICMP)
                .matchIPDst(gatewayIp.toIpPrefix());

        tBuilder.setOutput(PortNumber.CONTROLLER);

        ForwardingObjective fo = DefaultForwardingObjective.builder()
                .withSelector(sBuilder.build())
                .withTreatment(tBuilder.build())
                .withPriority(GATEWAY_ICMP_PRIORITY)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .fromApp(appId)
                .add();

        flowObjectiveService.forward(deviceId, fo);
    }

    private void unsetGatewayIcmp(Ip4Address gatewayIp) {
        if (gatewayIp == null) {
            return;
        }
        gatewayService.getGatewayDeviceIds().forEach(deviceId -> {
            removeGatewayIcmpRule(gatewayIp, deviceId);
        });
    }

    private void removeGatewayIcmpRule(Ip4Address gatewayIp, DeviceId deviceId) {
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();

        sBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_ICMP)
                .matchIPDst(gatewayIp.toIpPrefix());

        RulePopulatorUtil.removeRule(flowObjectiveService, appId, deviceId, sBuilder.build(),
                ForwardingObjective.Flag.VERSATILE, GATEWAY_ICMP_PRIORITY);

    }
    private void setExternalConnection(OpenstackRouter osRouter, String osSubNetId) {
        if (!osRouter.gatewayExternalInfo().isEnablePnat()) {
            log.debug("Source NAT is disabled");
            return;
        }

        OpenstackSubnet osSubNet = openstackService.subnet(osSubNetId);
        OpenstackNetwork osNet = openstackService.network(osSubNet.networkId());
        populateExternalRules(osNet, osSubNet);
    }

    private void unsetExternalConnection(OpenstackRouter osRouter, String osNetId) {
        if (!osRouter.gatewayExternalInfo().isEnablePnat()) {
            log.debug("Source NAT is disabled");
            return;
        }

        // FIXME router interface is subnet specific, not network
        OpenstackNetwork osNet = openstackService.network(osNetId);
        removeExternalRules(osNet);
    }

    private void setRoutes(OpenstackRouter osRouter, Optional<Host> host) {
        Set<OpenstackSubnet> routableSubNets = routableSubNets(osRouter.id());
        if (routableSubNets.size() < 2) {
            // no other subnet interface is connected to this router, do nothing
            return;
        }

        Set<String> routableSubNetIds = routableSubNets.stream()
                .map(OpenstackSubnet::id)
                .collect(Collectors.toSet());


        Set<Host> hosts = host.isPresent() ? ImmutableSet.of(host.get()) :
                Tools.stream(hostService.getHosts())
                        .filter(h -> routableSubNetIds.contains(h.annotations().value(SUBNET_ID)))
                        .collect(Collectors.toSet());

        hosts.forEach(h -> populateRoutingRules(h, routableSubNets));
    }

    private void unsetRoutes(OpenstackRouter osRouter, OpenstackSubnet osSubNet) {
        Set<OpenstackSubnet> routableSubNets = routableSubNets(osRouter.id());
        Tools.stream(hostService.getHosts())
                .filter(h -> Objects.equals(
                        h.annotations().value(NETWORK_ID), osSubNet.id()))
                .forEach(h -> removeRoutingRules(h, routableSubNets));

        routableSubNets.forEach(n -> {
            Tools.stream(hostService.getHosts())
                    .filter(h -> Objects.equals(
                            h.annotations().value(SUBNET_ID),
                            n.id()))
                    .forEach(h -> removeRoutingRules(h, ImmutableSet.of(osSubNet)));
            log.debug("Removed between {} to {}", n.name(), osSubNet.name());
        });
    }

    private OpenstackRouter openstackRouter(String routerId) {
        return openstackService.routers().stream().filter(r ->
                r.id().equals(routerId)).iterator().next();
    }

    private Optional<OpenstackPort> routerIfacePort(String osNetId) {
        // FIXME router interface is subnet specific, not network
        return openstackService.ports().stream()
                .filter(p -> p.deviceOwner().equals(DEVICE_OWNER_ROUTER_INTERFACE) &&
                        p.networkId().equals(osNetId))
                .findAny();
    }

    private Set<OpenstackSubnet> routableSubNets(String osRouterId) {
        return openstackService.ports().stream()
                .filter(p -> p.deviceOwner().equals(DEVICE_OWNER_ROUTER_INTERFACE) &&
                        p.deviceId().equals(osRouterId))
                .map(p -> openstackService.subnet(p.fixedIps().keySet().stream().findFirst().get()))
                .collect(Collectors.toSet());
    }

    private void populateExternalRules(OpenstackNetwork osNet, OpenstackSubnet osSubNet) {
        populateCnodeToGateway(Long.valueOf(osNet.segmentId()), osSubNet.cidr());
        populateGatewayToController(Long.valueOf(osNet.segmentId()), osSubNet.cidr());
    }

    private void removeExternalRules(OpenstackNetwork osNet) {
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        sBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchTunnelId(Long.valueOf(osNet.segmentId()))
                .matchEthDst(Constants.DEFAULT_GATEWAY_MAC);

        nodeService.completeNodes().forEach(node -> {
            ForwardingObjective.Flag flag = node.type().equals(GATEWAY) ?
                    ForwardingObjective.Flag.VERSATILE :
                    ForwardingObjective.Flag.SPECIFIC;

            RulePopulatorUtil.removeRule(
                    flowObjectiveService,
                    appId,
                    node.intBridge(),
                    sBuilder.build(),
                    flag,
                    ROUTING_RULE_PRIORITY);
        });
    }

    private void populateRoutingRules(Host host, Set<OpenstackSubnet> osSubNets) {
        String osSubNetId = host.annotations().value(SUBNET_ID);
        if (osSubNetId == null) {
            return;
        }

        DeviceId localDevice = host.location().deviceId();
        PortNumber localPort = host.location().port();
        if (!nodeService.dataIp(localDevice).isPresent()) {
            log.warn("Failed to populate L3 rules");
            return;
        }

        Map<String, String> vniMap = new HashMap<>();
        openstackService.networks().stream().forEach(n -> vniMap.put(n.id(), n.segmentId()));

        // TODO improve pipeline, do we have to install access rules between networks
        // for every single VMs?
        osSubNets.stream().filter(osSubNet -> !osSubNet.id().equals(osSubNetId)).forEach(osSubNet -> {
            populateRoutingRulestoSameNode(
                    host.ipAddresses().stream().findFirst().get().getIp4Address(),
                    host.mac(),
                    localPort, localDevice,
                    Long.valueOf(vniMap.get(osSubNet.networkId())),
                    osSubNet.cidr());

            nodeService.completeNodes().stream()
                    .filter(node -> node.type().equals(COMPUTE))
                    .filter(node -> !node.intBridge().equals(localDevice))
                    .forEach(node -> populateRoutingRulestoDifferentNode(
                            host.ipAddresses().stream().findFirst().get().getIp4Address(),
                            Long.valueOf(vniMap.get(osSubNet.networkId())),
                            node.intBridge(),
                            nodeService.dataIp(localDevice).get().getIp4Address(),
                            osSubNet.cidr()));
        });
    }

    private void removeRoutingRules(Host host, Set<OpenstackSubnet> osSubNets) {
        String osSubNetId = host.annotations().value(SUBNET_ID);
        if (osSubNetId == null) {
            return;
        }

        Map<String, String> vniMap = new HashMap<>();
        openstackService.networks().stream().forEach(n -> vniMap.put(n.id(), n.segmentId()));

        osSubNets.stream().filter(osSubNet -> !osSubNet.id().equals(osSubNetId)).forEach(osSubNet -> {
            TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
            sBuilder.matchEthType(Ethernet.TYPE_IPV4)
                    .matchIPDst(host.ipAddresses().stream().findFirst().get().toIpPrefix())
                    .matchIPSrc(IpPrefix.valueOf(osSubNet.cidr()))
                    .matchTunnelId(Long.valueOf(vniMap.get(osSubNet.networkId())));

            nodeService.completeNodes().stream()
                    .filter(node -> node.type().equals(COMPUTE))
                    .forEach(node -> RulePopulatorUtil.removeRule(
                            flowObjectiveService,
                            appId,
                            node.intBridge(),
                            sBuilder.build(),
                            ForwardingObjective.Flag.SPECIFIC,
                            EW_ROUTING_RULE_PRIORITY));
        });
        log.debug("Removed routing rule from {} to {}", host, osSubNets);
    }

    private void populateGatewayToController(long vni, String subNetCidr) {
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        sBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchTunnelId(vni)
                .matchIPSrc(IpPrefix.valueOf(subNetCidr))
                .matchEthDst(Constants.DEFAULT_GATEWAY_MAC);
        tBuilder.setOutput(PortNumber.CONTROLLER);

        ForwardingObjective fo = DefaultForwardingObjective.builder()
                .withSelector(sBuilder.build())
                .withTreatment(tBuilder.build())
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .withPriority(ROUTING_RULE_PRIORITY)
                .fromApp(appId)
                .add();

        gatewayService.getGatewayDeviceIds().forEach(deviceId -> flowObjectiveService.forward(deviceId, fo));
    }

    private void populateCnodeToGateway(long vni, String subnetCidr) {
        nodeService.completeNodes().stream()
                .filter(node -> node.type().equals(COMPUTE))
                .forEach(node -> populateRuleToGateway(
                        node.intBridge(),
                        gatewayService.getGatewayGroupId(node.intBridge()),
                        vni, subnetCidr));
    }

    private void populateRuleToGateway(DeviceId deviceId, GroupId groupId, long vni, String cidr) {
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        sBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchTunnelId(vni)
                .matchIPSrc(IpPrefix.valueOf(cidr))
                .matchEthDst(Constants.DEFAULT_GATEWAY_MAC);

        tBuilder.group(groupId);
        ForwardingObjective fo = DefaultForwardingObjective.builder()
                .withSelector(sBuilder.build())
                .withTreatment(tBuilder.build())
                .withFlag(ForwardingObjective.Flag.SPECIFIC)
                .withPriority(ROUTING_RULE_PRIORITY)
                .fromApp(appId)
                .add();

        flowObjectiveService.forward(deviceId, fo);
    }

    private void populateRoutingRulestoDifferentNode(Ip4Address vmIp, long vni,
                                                     DeviceId deviceId, Ip4Address hostIp,
                                                     String cidr) {
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        sBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchTunnelId(vni)
                .matchIPSrc(IpPrefix.valueOf(cidr))
                .matchIPDst(vmIp.toIpPrefix());
        tBuilder.extension(buildExtension(deviceService, deviceId, hostIp), deviceId)
                .setOutput(nodeService.tunnelPort(deviceId).get());

        ForwardingObjective fo = DefaultForwardingObjective.builder()
                .withSelector(sBuilder.build())
                .withTreatment(tBuilder.build())
                .withPriority(EW_ROUTING_RULE_PRIORITY)
                .withFlag(ForwardingObjective.Flag.SPECIFIC)
                .fromApp(appId)
                .add();

        flowObjectiveService.forward(deviceId, fo);
    }

    private void populateRoutingRulestoSameNode(Ip4Address vmIp, MacAddress vmMac,
                                                PortNumber port, DeviceId deviceId, long vni,
                                                String cidr) {
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        // FIXME: we need to check the VNI of the dest IP also just in case...
        sBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(vmIp.toIpPrefix())
                .matchIPSrc(IpPrefix.valueOf(cidr))
                .matchTunnelId(vni);

        tBuilder.setEthDst(vmMac)
                .setOutput(port);

        ForwardingObjective fo = DefaultForwardingObjective.builder()
                .withSelector(sBuilder.build())
                .withTreatment(tBuilder.build())
                .withPriority(EW_ROUTING_RULE_PRIORITY)
                .withFlag(ForwardingObjective.Flag.SPECIFIC)
                .fromApp(appId)
                .add();

        flowObjectiveService.forward(deviceId, fo);
    }

    private void reloadRoutingRules() {
        eventExecutor.execute(() -> openstackService.ports().stream()
                .filter(osPort -> osPort.deviceOwner().equals(DEVICE_OWNER_ROUTER_INTERFACE))
                .forEach(osPort -> {
                    OpenstackRouter osRouter = openstackRouter(osPort.deviceId());

                    setGatewayIcmp(Ip4Address.valueOf(openstackService
                            .subnet(osPort.fixedIps().keySet().stream().findAny().get()).gatewayIp()));

                    setRoutes(osRouter, Optional.empty());
                    if (osRouter.gatewayExternalInfo().externalFixedIps().size() > 0) {
                        String subnetId = osPort.fixedIps().keySet().stream().findFirst().get();
                        setExternalConnection(osRouter, subnetId);
                    }
                }));
    }

    @Override
    protected void hostDetected(Host host) {
        String osNetId = host.annotations().value(NETWORK_ID);
        Optional<OpenstackPort> routerIface = routerIfacePort(osNetId);
        if (!routerIface.isPresent()) {
            return;
        }
        eventExecutor.execute(() -> setRoutes(
                openstackRouter(routerIface.get().deviceId()),
                Optional.of(host)));
    }

    @Override
    protected void hostRemoved(Host host) {
        String osNetId = host.annotations().value(NETWORK_ID);
        Optional<OpenstackPort> routerIface = routerIfacePort(osNetId);
        if (!routerIface.isPresent()) {
            return;
        }
        Set<OpenstackSubnet> routableSubNets = routableSubNets(routerIface.get().deviceId());
        eventExecutor.execute(() -> removeRoutingRules(host, routableSubNets));
    }

    private class InternalNodeListener implements OpenstackNodeListener {

        @Override
        public void event(OpenstackNodeEvent event) {
            OpenstackNode node = event.node();

            switch (event.type()) {
                case COMPLETE:
                    log.info("COMPLETE node {} detected", node.hostname());
                    eventExecutor.execute(() -> {
                        if (node.type() == GATEWAY) {
                            GatewayNode gnode = GatewayNode.builder()
                                    .gatewayDeviceId(node.intBridge())
                                    .dataIpAddress(node.dataIp().getIp4Address())
                                    .uplinkIntf(node.externalPortName().get())
                                    .build();
                            gatewayService.addGatewayNode(gnode);
                        }
                        reloadRoutingRules();
                    });
                    break;
                case INIT:
                case DEVICE_CREATED:
                case INCOMPLETE:
                    eventExecutor.execute(() -> {
                        if (node.type() == GATEWAY) {
                            GatewayNode gnode = GatewayNode.builder()
                                    .gatewayDeviceId(node.intBridge())
                                    .dataIpAddress(node.dataIp().getIp4Address())
                                    .uplinkIntf(node.externalPortName().get())
                                    .build();
                            gatewayService.deleteGatewayNode(gnode);
                        }
                        reloadRoutingRules();
                    });
                default:
                    break;
            }
        }
    }
}
