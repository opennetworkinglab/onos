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
package org.onosproject.openstacknetworking.web;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.onlab.util.ItemNotFoundException;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.openstacknetworking.api.Constants;
import org.onosproject.openstacknetworking.api.OpenstackNetworkAdminService;
import org.onosproject.openstacknetworking.api.OpenstackRouterAdminService;
import org.onosproject.openstacknetworking.api.OpenstackSecurityGroupAdminService;
import org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil;
import org.onosproject.openstacknode.api.NodeState;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeAdminService;
import org.onosproject.openstacknode.api.OpenstackNodeService;
import org.onosproject.rest.AbstractWebResource;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.network.NetFloatingIP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.addRouterIface;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.CONTROLLER;

/**
 * REST interface for synchronizing openstack network states and rules.
 */
@Path("management")
public class OpenstackManagementWebResource extends AbstractWebResource {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String FLOATINGIPS = "floatingips";

    private static final String DEVICE_OWNER_IFACE = "network:router_interface";
    private final ObjectNode root = mapper().createObjectNode();
    private final ArrayNode floatingipsNode = root.putArray(FLOATINGIPS);

    private final OpenstackSecurityGroupAdminService osSgAdminService =
            get(OpenstackSecurityGroupAdminService.class);
    private final OpenstackNetworkAdminService osNetAdminService =
            get(OpenstackNetworkAdminService.class);
    private final OpenstackRouterAdminService osRouterAdminService =
            get(OpenstackRouterAdminService.class);
    private final OpenstackNodeService osNodeService =
            get(OpenstackNodeService.class);
    private final OpenstackNodeAdminService osNodeAdminService =
            get(OpenstackNodeAdminService.class);
    private final FlowRuleService flowRuleService = get(FlowRuleService.class);
    private final CoreService coreService = get(CoreService.class);

    /**
     * Synchronizes the network states with openstack.
     *
     * @return 200 OK with sync result, 404 not found
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("sync/states")
    public Response syncStates() {

        Optional<OpenstackNode> node = osNodeService.nodes(CONTROLLER).stream().findFirst();
        if (!node.isPresent()) {
            throw new ItemNotFoundException("Auth info is not found");
        }

        OSClient osClient = OpenstackNetworkingUtil.getConnectedClient(node.get());

        if (osClient == null) {
            throw new ItemNotFoundException("Auth info is not correct");
        }

        osClient.networking().securitygroup().list().forEach(osSg -> {
            if (osSgAdminService.securityGroup(osSg.getId()) != null) {
                osSgAdminService.updateSecurityGroup(osSg);
            } else {
                osSgAdminService.createSecurityGroup(osSg);
            }
        });

        osClient.networking().network().list().forEach(osNet -> {
            if (osNetAdminService.network(osNet.getId()) != null) {
                osNetAdminService.updateNetwork(osNet);
            } else {
                osNetAdminService.createNetwork(osNet);
            }
        });

        osClient.networking().subnet().list().forEach(osSubnet -> {
            if (osNetAdminService.subnet(osSubnet.getId()) != null) {
                osNetAdminService.updateSubnet(osSubnet);
            } else {
                osNetAdminService.createSubnet(osSubnet);
            }
        });

        osClient.networking().port().list().forEach(osPort -> {
            if (osNetAdminService.port(osPort.getId()) != null) {
                osNetAdminService.updatePort(osPort);
            } else {
                osNetAdminService.createPort(osPort);
            }
        });

        osClient.networking().router().list().forEach(osRouter -> {
            if (osRouterAdminService.router(osRouter.getId()) != null) {
                osRouterAdminService.updateRouter(osRouter);
            } else {
                osRouterAdminService.createRouter(osRouter);
            }

            osNetAdminService.ports().stream()
                    .filter(osPort -> Objects.equals(osPort.getDeviceId(), osRouter.getId()) &&
                            Objects.equals(osPort.getDeviceOwner(), DEVICE_OWNER_IFACE))
                    .forEach(osPort -> addRouterIface(osPort, osRouterAdminService));
        });

        osClient.networking().floatingip().list().forEach(osFloating -> {
            if (osRouterAdminService.floatingIp(osFloating.getId()) != null) {
                osRouterAdminService.updateFloatingIp(osFloating);
            } else {
                osRouterAdminService.createFloatingIp(osFloating);
            }
        });

        return ok(mapper().createObjectNode()).build();
    }

    /**
     * Synchronizes the flow rules.
     *
     * @return 200 OK with sync result, 404 not found
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("sync/rules")
    public Response syncRules() {

        osNodeService.completeNodes().forEach(osNode -> {
            OpenstackNode updated = osNode.updateState(NodeState.INIT);
            osNodeAdminService.updateNode(updated);
        });

        return ok(mapper().createObjectNode()).build();
    }

    /**
     * Purges the network states.
     *
     * @return 200 OK with purge result, 404 not found
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("purge/states")
    public Response purgeStates() {

        osRouterAdminService.clear();
        osNetAdminService.clear();
        osSgAdminService.clear();

        return ok(mapper().createObjectNode()).build();
    }

    /**
     * Purges the flow rules installed by openstacknetworking.
     *
     * @return 200 OK with purge result, 404 not found
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("purge/rules")
    public Response purgeRules() {

        ApplicationId appId = coreService.getAppId(Constants.OPENSTACK_NETWORKING_APP_ID);
        if (appId == null) {
            throw new ItemNotFoundException("application not found");
        }
        flowRuleService.removeFlowRulesById(appId);

        return ok(mapper().createObjectNode()).build();
    }

    /**
     * Obtains a collection of all floating IPs.
     *
     * @return 200 OK with a collection of floating IPs, 404 not found
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("floatingips/all")
    public Response allFloatingIps() {

        List<NetFloatingIP> floatingIps =
                Lists.newArrayList(osRouterAdminService.floatingIps());
        floatingIps.stream()
                .sorted(Comparator.comparing(NetFloatingIP::getFloatingIpAddress))
                .forEach(fip -> floatingipsNode.add(fip.getFloatingIpAddress()));

        return ok(root).build();
    }

    /**
     * Obtains a collection of all floating IPs mapped with fixed IPs.
     *
     * @return 200 OK with a collection of floating IPs mapped with fixed IPs,
     *         404 not found
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("floatingips/mapped")
    public Response mappedFloatingIps() {

        List<NetFloatingIP> floatingIps =
                Lists.newArrayList(osRouterAdminService.floatingIps());

        floatingIps.stream()
                .filter(fip -> !Strings.isNullOrEmpty(fip.getFixedIpAddress()))
                .sorted(Comparator.comparing(NetFloatingIP::getFloatingIpAddress))
                .forEach(fip -> floatingipsNode.add(fip.getFloatingIpAddress()));

        return ok(root).build();
    }
}
