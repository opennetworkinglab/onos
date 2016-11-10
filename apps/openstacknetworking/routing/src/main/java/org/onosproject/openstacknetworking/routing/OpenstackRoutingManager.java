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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.util.Tools;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.GroupId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.openstackinterface.OpenstackInterfaceService;
import org.onosproject.openstackinterface.OpenstackNetwork;
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
    protected OpenstackInterfaceService openstackService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackNodeService nodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ScalableGatewayService gatewayService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

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
        super.deactivate();
        nodeService.removeListener(nodeListener);
        log.info("stopped");
    }

    @Override
    protected void hostDetected(Host host) {
        // Installs forwarding flow rules to VMs in different nodes and different subnets
        // that are connected via a router
        Optional<OpenstackRouter> routerOfTheHost = getRouter(host);

        if (!routerOfTheHost.isPresent()) {
            return;
        }

        routableSubNets(routerOfTheHost.get().id()).stream()
                .filter(subnet -> !subnet.id().equals(host.annotations().value(SUBNET_ID)))
                .forEach(subnet -> setForwardingRulesAmongHostsInDifferentCnodes(host, getHosts(subnet), true));
    }

    @Override
    protected void hostRemoved(Host host) {
        // Removes forwarding flow rules to VMs in different nodes and different subnets
        // that are connected via a router
        Optional<OpenstackRouter> routerOfTheHost = getRouter(host);
        if (!routerOfTheHost.isPresent()) {
            return;
        }

        routableSubNets(routerOfTheHost.get().id()).stream()
                .filter(subnet -> !subnet.id().equals(host.annotations().value(SUBNET_ID)))
                .forEach(subnet -> setForwardingRulesAmongHostsInDifferentCnodes(host, getHosts(subnet), false));
    }

    @Override
    public void createRouter(OpenstackRouter osRouter) {
    }

    @Override
    public void updateRouter(OpenstackRouter osRouter) {
        if (osRouter.gatewayExternalInfo().externalFixedIps().size() > 0) {
            routableSubNets(osRouter.id()).stream()
                    .forEach(subnet -> setExternalConnection(osRouter, subnet, true));

            log.info("Connected external gateway {} to router {}",
                     osRouter.gatewayExternalInfo().externalFixedIps(),
                     osRouter.name());
        } else {
            routableSubNets(osRouter.id()).stream()
                    .forEach(subnet -> setExternalConnection(osRouter, subnet, false));

            log.info("Disconnected external gateway from router {}",
                     osRouter.name());
        }
    }

    @Override
    public void removeRouter(String osRouterId) {
        // Nothing to do
        // All interfaces need to be removed before the router is removed,
        // and all related flow rues are removed when the interfaces are removed.
    }

    @Override
    public void addRouterInterface(OpenstackRouterInterface routerIfaceAdded) {
        OpenstackRouter osRouter = openstackRouter(routerIfaceAdded.id());
        OpenstackSubnet osSubnetAdded = openstackService.subnet(routerIfaceAdded.subnetId());
        if (osRouter == null || osSubnetAdded == null) {
            log.warn("Failed to add router interface {}", routerIfaceAdded);
            return;
        }
        handleRouterInterfaces(osRouter, osSubnetAdded);
    }

    @Override
    public void removeRouterInterface(OpenstackRouterInterface routerIface) {
        OpenstackRouter osRouter = openstackService.router(routerIface.id());
        OpenstackSubnet osSubnetRemoved = openstackService.subnet(routerIface.subnetId());
        if (osRouter == null) {
            log.warn("Failed to remove router interface {}", routerIface);
            return;
        }
        handleRouterInterfacesRemoved(osRouter, osSubnetRemoved);

        log.info("Disconnected {} from router {}", osSubnetRemoved.cidr(), osRouter.name());
    }

    private void handleRouterInterfaces(OpenstackRouter osRouter, OpenstackSubnet osSubnetAdded) {
        OpenstackNetwork osNetworkAdded = openstackService.network(osSubnetAdded.networkId());
        if (osNetworkAdded == null) {  // in case of external network subnet
            return;
        }

        // Sets flow rules for routing among subnets connected to a router.
        setRoutesAmongSubnets(osRouter, osSubnetAdded, true);

        // Sets flow rules for forwarding "packets going to external networks" to gateway nodes.
        if (osRouter.gatewayExternalInfo().externalFixedIps().size() > 0) {
            setExternalConnection(osRouter, osSubnetAdded, true);
        }

        // Sets flow rules to handle ping to the virtual gateway.
        Ip4Address vGatewayIp = Ip4Address.valueOf(osSubnetAdded.gatewayIp());
        gatewayService.getGatewayDeviceIds()
                .forEach(deviceId -> setGatewayIcmpRule(vGatewayIp, deviceId, true));

        // Sets east-west routing rules for VMs in different Cnode to Switching Table.
        setForwardingRulesForEastWestRouting(osRouter, osSubnetAdded, true);

    }

    private void handleRouterInterfacesRemoved(OpenstackRouter osRouter, OpenstackSubnet osSubnetRemoved) {

        // Removes flow rules for routing among subnets connected to a router.
        setRoutesAmongSubnets(osRouter, osSubnetRemoved, false);

        // Removes flow rules for forwarding "packets going to external networks" to gateway nodes.
        if (osRouter.gatewayExternalInfo().externalFixedIps().size() > 0) {
            setExternalConnection(osRouter, osSubnetRemoved, false);
        }

        // Removes flow rules to handle ping to the virtual gateway.
        Ip4Address vGatewayIp = Ip4Address.valueOf(osSubnetRemoved.gatewayIp());
        gatewayService.getGatewayDeviceIds()
                .forEach(deviceId -> setGatewayIcmpRule(vGatewayIp, deviceId, false));

        // Removes east-west routing rules for VMs in different Cnode to Switching Table.
        setForwardingRulesForEastWestRouting(osRouter, osSubnetRemoved, false);

        // Resets east-west routing rules for VMs in different Cnode to Switching Table.
        routableSubNets(osRouter.id()).stream()
                .forEach(subnet -> setForwardingRulesForEastWestRouting(osRouter, subnet, true));
    }

    private void setRoutesAmongSubnets(OpenstackRouter osRouter, OpenstackSubnet osSubnetAdded, boolean install) {
        Set<OpenstackSubnet> routableSubNets = routableSubNets(osRouter.id());
        if (routableSubNets.size() < 2) {
            // no other subnet interface is connected to this router, do nothing
            return;
        }

        Map<String, String> vniMap = new HashMap<>();
        openstackService.networks().forEach(n -> vniMap.put(n.id(), n.segmentId()));

        routableSubNets.stream()
                .filter(subnet -> !subnet.id().equals(osSubnetAdded.id()))
                .filter(subnet -> vniMap.get(subnet.networkId()) != null)
                .forEach(subnet -> nodeService.completeNodes().stream()
                        .filter(node -> node.type().equals(COMPUTE))
                        .forEach(node -> {
                                setRoutingRules(node.intBridge(),
                                        Integer.parseInt(vniMap.get(subnet.networkId())),
                                        Integer.parseInt(vniMap.get(osSubnetAdded.networkId())),
                                        subnet, osSubnetAdded, install);
                                setRoutingRules(node.intBridge(),
                                        Integer.parseInt(vniMap.get(osSubnetAdded.networkId())),
                                        Integer.parseInt(vniMap.get(subnet.networkId())),
                                        osSubnetAdded, subnet, install);
                                }
                        ));
    }

    private void setRoutingRules(DeviceId deviceId, int srcVni, int dstVni,
                                 OpenstackSubnet subnetSrc, OpenstackSubnet subnetDst, boolean install) {

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        sBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchTunnelId(srcVni)
                .matchIPSrc(IpPrefix.valueOf(subnetSrc.cidr()))
                .matchIPDst(IpPrefix.valueOf(subnetDst.cidr()));

        tBuilder.setTunnelId(dstVni);

        RulePopulatorUtil.setRule(flowObjectiveService, appId, deviceId, sBuilder.build(),
                tBuilder.build(), ForwardingObjective.Flag.SPECIFIC, EW_ROUTING_RULE_PRIORITY, install);

        // Flow rules for destination is in different subnet and different node,
        // because VNI is converted to destination VNI in the source VM node.
        sBuilder = DefaultTrafficSelector.builder();
        tBuilder = DefaultTrafficTreatment.builder();

        sBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchTunnelId(dstVni)
                .matchIPSrc(IpPrefix.valueOf(subnetSrc.cidr()))
                .matchIPDst(IpPrefix.valueOf(subnetDst.cidr()));

        tBuilder.setTunnelId(dstVni);

        RulePopulatorUtil.setRule(flowObjectiveService, appId, deviceId, sBuilder.build(),
                tBuilder.build(), ForwardingObjective.Flag.SPECIFIC, EW_ROUTING_RULE_PRIORITY, install);
    }

    private void setExternalConnection(OpenstackRouter osRouter, OpenstackSubnet osSubNet, boolean install) {
        if (!osRouter.gatewayExternalInfo().isEnablePnat()) {
            log.debug("Source NAT is disabled");
            return;
        }

        //OpenstackSubnet osSubNet = openstackService.subnet(osSubNetId);
        OpenstackNetwork osNet = openstackService.network(osSubNet.networkId());

        nodeService.completeNodes().stream()
                .filter(node -> node.type().equals(COMPUTE))
                .forEach(node -> setRulesToGateway(
                        node.intBridge(),
                        gatewayService.getGatewayGroupId(node.intBridge()),
                        Long.valueOf(osNet.segmentId()), osSubNet.cidr(), install));

        // Is this for PNAT ??
        setRulesForGatewayToController(Long.valueOf(osNet.segmentId()), osSubNet.cidr(), install);
    }

    private void setRulesToGateway(DeviceId deviceId, GroupId groupId, long vni, String cidr, boolean install) {
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        sBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchTunnelId(vni)
                .matchIPSrc(IpPrefix.valueOf(cidr))
                .matchEthDst(Constants.DEFAULT_GATEWAY_MAC);

        tBuilder.group(groupId);

        RulePopulatorUtil.setRule(flowObjectiveService, appId, deviceId, sBuilder.build(),
                tBuilder.build(), ForwardingObjective.Flag.SPECIFIC, ROUTING_RULE_PRIORITY, install);
    }

    private void setRulesForGatewayToController(long vni, String subNetCidr, boolean install) {
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        sBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchTunnelId(vni)
                .matchIPSrc(IpPrefix.valueOf(subNetCidr))
                .matchEthDst(Constants.DEFAULT_GATEWAY_MAC);
        tBuilder.setOutput(PortNumber.CONTROLLER);

        gatewayService.getGatewayDeviceIds().forEach(deviceId ->
                RulePopulatorUtil.setRule(flowObjectiveService, appId, deviceId, sBuilder.build(),
                        tBuilder.build(), ForwardingObjective.Flag.VERSATILE, ROUTING_RULE_PRIORITY, install));
    }

    private void setGatewayIcmpRule(Ip4Address gatewayIp, DeviceId deviceId, boolean install) {
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        sBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_ICMP)
                .matchIPDst(gatewayIp.toIpPrefix());

        tBuilder.setOutput(PortNumber.CONTROLLER);

        RulePopulatorUtil.setRule(flowObjectiveService, appId, deviceId, sBuilder.build(),
                tBuilder.build(), ForwardingObjective.Flag.VERSATILE, GATEWAY_ICMP_PRIORITY, install);
    }

    private void setForwardingRulesForEastWestRouting(OpenstackRouter router, OpenstackSubnet subnetAdded,
                                                      boolean install) {

        Set<OpenstackSubnet> subnets = routableSubNets(router.id());

        Set<Host> hosts = Tools.stream(hostService.getHosts())
                .filter(h -> getVni(h).equals(openstackService.network(subnetAdded.networkId()).segmentId()))
                .collect(Collectors.toSet());

        subnets.stream()
                .filter(subnet -> !subnet.id().equals(subnetAdded.id()))
                .forEach(subnet -> getHosts(subnet)
                        .forEach(h -> setForwardingRulesAmongHostsInDifferentCnodes(h, hosts, install)));
    }

    private void setForwardingRulesAmongHostsInDifferentCnodes(Host host, Set<Host> remoteHosts, boolean install) {
        Ip4Address localVmIp = getIp(host);
        DeviceId localDeviceId = host.location().deviceId();
        Optional<IpAddress> localDataIp = nodeService.dataIp(localDeviceId);

        if (!localDataIp.isPresent()) {
            log.debug("Failed to get data IP for device {}",
                    host.location().deviceId());
            return;
        }

        remoteHosts.stream()
                .filter(remoteHost -> !host.location().deviceId().equals(remoteHost.location().deviceId()))
                .forEach(remoteVm -> {
                    Optional<IpAddress> remoteDataIp = nodeService.dataIp(remoteVm.location().deviceId());
                    if (remoteDataIp.isPresent()) {
                        setVxLanFlowRule(getVni(remoteVm),
                                localDeviceId,
                                remoteDataIp.get().getIp4Address(),
                                getIp(remoteVm), install);

                        setVxLanFlowRule(getVni(host),
                                remoteVm.location().deviceId(),
                                localDataIp.get().getIp4Address(),
                                localVmIp, install);
                    }
                });
    }

    private void setVxLanFlowRule(String vni, DeviceId deviceId, Ip4Address remoteIp,
                                  Ip4Address vmIp, boolean install) {
        Optional<PortNumber> tunnelPort = nodeService.tunnelPort(deviceId);
        if (!tunnelPort.isPresent()) {
            log.warn("Failed to get tunnel port from {}", deviceId);
            return;
        }

        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder();

        sBuilder.matchEthType(Ethernet.TYPE_IPV4)
                .matchTunnelId(Long.parseLong(vni))
                .matchIPDst(vmIp.toIpPrefix());
        tBuilder.extension(buildExtension(deviceService, deviceId, remoteIp), deviceId)
                .setOutput(tunnelPort.get());

        RulePopulatorUtil.setRule(flowObjectiveService, appId, deviceId, sBuilder.build(),
                tBuilder.build(), ForwardingObjective.Flag.SPECIFIC, SWITCHING_RULE_PRIORITY, install);
    }


    private OpenstackRouter openstackRouter(String routerId) {
        return openstackService.routers().stream().filter(r ->
                r.id().equals(routerId)).iterator().next();
    }

    @Override
    public void reinstallVmFlow(Host host) {
        // TODO: implements later
    }

    @Override
    public void purgeVmFlow(Host host) {
        // TODO: implements later
    }

    private class InternalNodeListener implements OpenstackNodeListener {

        @Override
        public void event(OpenstackNodeEvent event) {
            OpenstackNode node = event.node();

            switch (event.type()) {
                case COMPLETE:
                case INCOMPLETE:
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
                    });
                    openstackService.routers().stream()
                            .forEach(router -> routableSubNets(router.id()).stream()
                                        .forEach(subnet -> handleRouterInterfaces(router, subnet)));
                    break;
                case INIT:
                case DEVICE_CREATED:
                default:
                    break;
            }
        }
    }
}
