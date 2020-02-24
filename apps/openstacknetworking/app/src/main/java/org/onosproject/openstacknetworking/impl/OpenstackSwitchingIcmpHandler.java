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
package org.onosproject.openstacknetworking.impl;

import com.google.common.collect.ImmutableSet;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpAddress;
import org.onlab.packet.VlanId;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cfg.ConfigProperty;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.Device;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.openstacknetworking.api.OpenstackFlowRuleService;
import org.onosproject.openstacknetworking.api.OpenstackNetwork.Type;
import org.onosproject.openstacknetworking.api.OpenstackNetworkService;
import org.onosproject.openstacknetworking.api.OpenstackRouterEvent;
import org.onosproject.openstacknetworking.api.OpenstackRouterListener;
import org.onosproject.openstacknetworking.api.OpenstackRouterService;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeEvent;
import org.onosproject.openstacknode.api.OpenstackNodeListener;
import org.onosproject.openstacknode.api.OpenstackNodeService;
import org.openstack4j.model.network.Network;
import org.openstack4j.model.network.Router;
import org.openstack4j.model.network.RouterInterface;
import org.openstack4j.model.network.Subnet;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.onlab.packet.ICMP.CODE_ECHO_REQEUST;
import static org.onlab.packet.ICMP.TYPE_ECHO_REPLY;
import static org.onlab.packet.ICMP.TYPE_ECHO_REQUEST;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.openstacknetworking.api.Constants.DEFAULT_GATEWAY_MAC;
import static org.onosproject.openstacknetworking.api.Constants.OPENSTACK_NETWORKING_APP_ID;
import static org.onosproject.openstacknetworking.api.Constants.PRIORITY_ICMP_RULE;
import static org.onosproject.openstacknetworking.api.Constants.ROUTING_TABLE;
import static org.onosproject.openstacknetworking.impl.OsgiPropertyConstants.USE_STATEFUL_SNAT;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.externalGatewayIp;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.getPropertyValueAsBoolean;
import static org.onosproject.openstacknetworking.util.RulePopulatorUtil.NXM_NX_IP_TTL;
import static org.onosproject.openstacknetworking.util.RulePopulatorUtil.NXM_OF_ICMP_TYPE;
import static org.onosproject.openstacknetworking.util.RulePopulatorUtil.buildLoadExtension;
import static org.onosproject.openstacknetworking.util.RulePopulatorUtil.buildMoveEthSrcToDstExtension;
import static org.onosproject.openstacknetworking.util.RulePopulatorUtil.buildMoveIpSrcToDstExtension;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.COMPUTE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Populates the ICMP flow rules for providing connectivity with gateways.
 */
@Component(immediate = true)
public class OpenstackSwitchingIcmpHandler {

    private final Logger log = getLogger(getClass());

    private static final int DEFAULT_TTL = 0xff;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DriverService driverService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackNodeService osNodeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackNetworkService osNetworkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackFlowRuleService osFlowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackRouterService osRouterService;

    private final ExecutorService eventExecutor = newSingleThreadExecutor(
            groupedThreads(this.getClass().getSimpleName(), "event-handler"));
    private final OpenstackRouterListener osRouterListener = new InternalRouterEventListener();
    private final OpenstackNodeListener osNodeListener = new InternalNodeEventListener();

    private ApplicationId appId;
    private NodeId localNodeId;

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(OPENSTACK_NETWORKING_APP_ID);
        localNodeId = clusterService.getLocalNode().id();
        osRouterService.addListener(osRouterListener);
        osNodeService.addListener(osNodeListener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        osRouterService.removeListener(osRouterListener);
        osNodeService.removeListener(osNodeListener);
        eventExecutor.shutdown();

        log.info("Stopped");
    }

    private boolean getStatefulSnatFlag() {
        Set<ConfigProperty> properties =
                configService.getProperties(OpenstackRoutingSnatHandler.class.getName());
        return getPropertyValueAsBoolean(properties, USE_STATEFUL_SNAT);
    }

    private void processRouterIntfEvent(Router osRouter, RouterInterface routerIface, boolean install) {
        if (!getStatefulSnatFlag()) {
            return;
        }

        Subnet osSubnet = osNetworkService.subnet(routerIface.getSubnetId());
        Type netType = osNetworkService.networkType(osSubnet.getNetworkId());
        String segId = osNetworkService.segmentId(osSubnet.getNetworkId());
        IpAddress gatewayIp = IpAddress.valueOf(osSubnet.getGateway());
        Set<Subnet> routableSubnets = routableSubnets(osRouter, osSubnet.getId());

        osNodeService.completeNodes(COMPUTE).stream()
                .filter(cNode -> cNode.dataIp() != null)
                .forEach(cNode -> {
                    setRoutableSubnetsIcmpRules(cNode, segId, osSubnet,
                            routableSubnets, gatewayIp, netType, install);
                });
    }

    private void processRouteGatewayEvent(Router osRouter, boolean install) {
        if (!getStatefulSnatFlag()) {
            return;
        }

        osNodeService.completeNodes(COMPUTE).stream()
                .filter(cNode -> cNode.dataIp() != null)
                .forEach(cNode -> {
                    setExtGatewayIcmpReplyRules(cNode, osRouter, install);
                });
    }

    private void setExtGatewayIcmpReplyRules(OpenstackNode osNode,
                                             Router osRouter,
                                             boolean install) {

        IpAddress natAddress = externalGatewayIp(osRouter, osNetworkService);
        if (natAddress == null) {
            return;
        }

        setGatewayIcmpReplyRule(osNode, null, natAddress, null, install);
    }

    private void setRoutableSubnetsIcmpRules(OpenstackNode osNode,
                                             String segmentId,
                                             Subnet updatedSubnet,
                                             Set<Subnet> routableSubnets,
                                             IpAddress gatewayIp,
                                             Type networkType,
                                             boolean install) {
        setGatewayIcmpReplyRule(osNode, segmentId, gatewayIp, networkType, install);

        routableSubnets.forEach(subnet -> {
            setGatewayIcmpReplyRule(osNode, segmentId,
                    IpAddress.valueOf(subnet.getGateway()), networkType, install);

            Network network = osNetworkService.network(subnet.getNetworkId());

            setGatewayIcmpReplyRule(osNode, network.getProviderSegID(),
                    IpAddress.valueOf(updatedSubnet.getGateway()), networkType, install);
        });
    }

    private Set<Subnet> routableSubnets(Router osRouter, String osSubnetId) {
        Set<Subnet> osSubnets = osRouterService.routerInterfaces(osRouter.getId())
                .stream()
                .filter(iface -> !Objects.equals(iface.getSubnetId(), osSubnetId))
                .map(iface -> osNetworkService.subnet(iface.getSubnetId()))
                .collect(Collectors.toSet());
        return ImmutableSet.copyOf(osSubnets);
    }

    private void setGatewayIcmpReplyRule(OpenstackNode osNode,
                                         String segmentId,
                                         IpAddress gatewayIp,
                                         Type networkType,
                                         boolean install) {
        TrafficSelector.Builder sBuilder = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPProtocol(IPv4.PROTOCOL_ICMP)
                .matchIcmpType(TYPE_ECHO_REQUEST)
                .matchIcmpCode(CODE_ECHO_REQEUST)
                .matchIPDst(gatewayIp.getIp4Address().toIpPrefix());

        if (segmentId != null) {
            switch (networkType) {
                case VXLAN:
                case GRE:
                case GENEVE:
                    sBuilder.matchTunnelId(Long.parseLong(segmentId));
                    break;
                case VLAN:
                    sBuilder.matchVlanId(VlanId.vlanId(segmentId));
                    break;
                default:
                    break;
            }
        }

        Device device = deviceService.getDevice(osNode.intgBridge());
        TrafficTreatment.Builder tBuilder = DefaultTrafficTreatment.builder()
                .extension(buildMoveEthSrcToDstExtension(device), device.id())
                .extension(buildMoveIpSrcToDstExtension(device), device.id())
                .extension(buildLoadExtension(device, NXM_NX_IP_TTL, DEFAULT_TTL), device.id())
                .extension(buildLoadExtension(device, NXM_OF_ICMP_TYPE, TYPE_ECHO_REPLY), device.id())
                .setIpSrc(gatewayIp)
                .setEthSrc(DEFAULT_GATEWAY_MAC)
                .setOutput(PortNumber.IN_PORT);

        osFlowRuleService.setRule(
                appId,
                osNode.intgBridge(),
                sBuilder.build(),
                tBuilder.build(),
                PRIORITY_ICMP_RULE,
                ROUTING_TABLE,
                install);
    }

    private class InternalRouterEventListener implements OpenstackRouterListener {
        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(OpenstackRouterEvent event) {
            switch (event.type()) {
                case OPENSTACK_ROUTER_INTERFACE_ADDED:
                    eventExecutor.execute(() -> processRouterIntfCreation(event));
                    break;
                case OPENSTACK_ROUTER_INTERFACE_REMOVED:
                    eventExecutor.execute(() -> processRouterIntfRemoval(event));
                    break;
                case OPENSTACK_ROUTER_GATEWAY_ADDED:
                    eventExecutor.execute(() -> processRouterGatewayAddition(event));
                    break;
                case OPENSTACK_ROUTER_GATEWAY_REMOVED:
                    eventExecutor.execute(() -> processRouterGatewayRemoval(event));
                    break;
                default:
                    // do nothing for the other events
                    break;
            }
        }

        private void processRouterIntfCreation(OpenstackRouterEvent event) {
            if (!isRelevantHelper()) {
                return;
            }

            log.debug("Router interface {} added to router {}",
                    event.routerIface().getPortId(),
                    event.routerIface().getId());

            processRouterIntfEvent(event.subject(), event.routerIface(), true);
        }

        private void processRouterIntfRemoval(OpenstackRouterEvent event) {
            if (!isRelevantHelper()) {
                return;
            }

            log.debug("Router interface {} removed from router {}",
                    event.routerIface().getPortId(),
                    event.routerIface().getId());

            processRouterIntfEvent(event.subject(), event.routerIface(), false);
        }

        private void processRouterGatewayAddition(OpenstackRouterEvent event) {
            if (!isRelevantHelper()) {
                return;
            }

            log.debug("Router external gateway {} added",
                    event.externalGateway().getNetworkId());

            processRouteGatewayEvent(event.subject(), true);
        }

        private void processRouterGatewayRemoval(OpenstackRouterEvent event) {
            if (!isRelevantHelper()) {
                return;
            }

            log.debug("Router external gateway {} removed",
                    event.externalGateway().getNetworkId());

            processRouteGatewayEvent(event.subject(), false);
        }
    }

    private class InternalNodeEventListener implements OpenstackNodeListener {

        private boolean isRelevantHelper() {
            return Objects.equals(localNodeId, leadershipService.getLeader(appId.name()));
        }

        @Override
        public void event(OpenstackNodeEvent event) {
            OpenstackNode osNode = event.subject();
            switch (event.type()) {
                case OPENSTACK_NODE_COMPLETE:
                case OPENSTACK_NODE_UPDATED:
                case OPENSTACK_NODE_REMOVED:
                    eventExecutor.execute(() -> {
                        if (!isRelevantHelper()) {
                            return;
                        }
                        reconfigureRouters(osNode);
                    });
                    break;
                case OPENSTACK_NODE_INCOMPLETE:
                default:
                    break;
            }
        }

        private void reconfigureRouters(OpenstackNode osNode) {
            osRouterService.routers().forEach(osRouter -> {
                osRouterService.routerInterfaces(osRouter.getId()).forEach(iface -> {
                    processRouterIntfEvent(osRouter, iface, true);
                });
                processRouteGatewayEvent(osRouter, true);
            });
            log.debug("Reconfigure routers for {}", osNode.hostname());
        }
    }
}
