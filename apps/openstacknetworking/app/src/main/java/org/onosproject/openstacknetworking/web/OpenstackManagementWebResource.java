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
import org.onlab.packet.IpAddress;
import org.onlab.util.ItemNotFoundException;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.openstacknetworking.api.Constants;
import org.onosproject.openstacknetworking.api.OpenstackHaService;
import org.onosproject.openstacknetworking.api.OpenstackNetworkAdminService;
import org.onosproject.openstacknetworking.api.OpenstackRouterAdminService;
import org.onosproject.openstacknetworking.api.OpenstackSecurityGroupAdminService;
import org.onosproject.openstacknetworking.impl.OpenstackRoutingArpHandler;
import org.onosproject.openstacknetworking.impl.OpenstackRoutingSnatHandler;
import org.onosproject.openstacknetworking.impl.OpenstackSecurityGroupHandler;
import org.onosproject.openstacknetworking.impl.OpenstackSwitchingArpHandler;
import org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil;
import org.onosproject.openstacknode.api.NodeState;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeAdminService;
import org.onosproject.rest.AbstractWebResource;
import org.openstack4j.api.OSClient;
import org.openstack4j.model.network.NetFloatingIP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.lang.Thread.sleep;
import static java.util.stream.StreamSupport.stream;
import static javax.ws.rs.core.Response.status;
import static org.onlab.util.Tools.nullIsIllegal;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.addRouterIface;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.checkActivationFlag;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.checkArpMode;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.getPropertyValue;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.getPropertyValueAsBoolean;
import static org.onosproject.openstacknode.api.NodeState.COMPLETE;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.COMPUTE;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.CONTROLLER;
import static org.onosproject.openstacknode.api.OpenstackNode.NodeType.GATEWAY;

/**
 * REST interface for synchronizing openstack network states and rules.
 */
@Path("management")
public class OpenstackManagementWebResource extends AbstractWebResource {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String FLOATINGIPS = "floatingips";
    private static final String ARP_MODE_NAME = "arpMode";
    private static final String USE_SECURITY_GROUP_NAME = "useSecurityGroup";
    private static final String USE_STATEFUL_SNAT_NAME = "useStatefulSnat";

    private static final long SLEEP_MS = 3000; // we wait 3s for init each node
    private static final long TIMEOUT_MS = 10000; // we wait 10s

    private static final String DEVICE_OWNER_IFACE = "network:router_interface";

    private static final String ARP_MODE_REQUIRED = "ARP mode is not specified";
    private static final String STATEFUL_SNAT_REQUIRED = "Stateful SNAT flag nis not specified";

    private static final String SECURITY_GROUP_FLAG_REQUIRED = "Security Group flag is not specified";

    private static final String AUTH_INFO_NOT_FOUND = "Auth info is not found";
    private static final String AUTH_INFO_NOT_CORRECT = "Auth info is not correct";

    private static final String HTTP_HEADER_ACCEPT = "accept";
    private static final String HTTP_HEADER_VALUE_JSON = "application/json";

    private static final String IS_ACTIVE = "isActive";
    private static final String FLAG_TRUE = "true";
    private static final String FLAG_FALSE = "false";

    private static final String ACTIVE_IP = "activeIp";

    private final ObjectNode root = mapper().createObjectNode();
    private final ArrayNode floatingipsNode = root.putArray(FLOATINGIPS);

    private final OpenstackSecurityGroupAdminService osSgAdminService =
            get(OpenstackSecurityGroupAdminService.class);
    private final OpenstackNetworkAdminService osNetAdminService =
            get(OpenstackNetworkAdminService.class);
    private final OpenstackRouterAdminService osRouterAdminService =
            get(OpenstackRouterAdminService.class);
    private final OpenstackNodeAdminService osNodeAdminService =
            get(OpenstackNodeAdminService.class);
    private final OpenstackHaService osHaService = get(OpenstackHaService.class);
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

        Map<String, String> headerMap = new HashMap();
        headerMap.put(HTTP_HEADER_ACCEPT, HTTP_HEADER_VALUE_JSON);

        Optional<OpenstackNode> node = osNodeAdminService.nodes(CONTROLLER).stream().findFirst();
        if (!node.isPresent()) {
            log.error(AUTH_INFO_NOT_FOUND);
            throw new ItemNotFoundException(AUTH_INFO_NOT_FOUND);
        }

        OSClient osClient = OpenstackNetworkingUtil.getConnectedClient(node.get());

        if (osClient == null) {
            log.error(AUTH_INFO_NOT_CORRECT);
            throw new ItemNotFoundException(AUTH_INFO_NOT_CORRECT);
        }

        try {
            osClient.headers(headerMap).networking().securitygroup().list().forEach(osSg -> {
                if (osSgAdminService.securityGroup(osSg.getId()) != null) {
                    osSgAdminService.updateSecurityGroup(osSg);
                } else {
                    osSgAdminService.createSecurityGroup(osSg);
                }
            });
        } catch (Exception e) {
            log.warn("Failed to retrieve security group due to {}", e.getMessage());
            return Response.serverError().build();
        }

        try {
            osClient.headers(headerMap).networking().network().list().forEach(osNet -> {
                if (osNetAdminService.network(osNet.getId()) != null) {
                    osNetAdminService.updateNetwork(osNet);
                } else {
                    osNetAdminService.createNetwork(osNet);
                }
            });
        } catch (Exception e) {
            log.warn("Failed to retrieve network due to {}", e.getMessage());
            return Response.serverError().build();
        }

        try {
            osClient.headers(headerMap).networking().subnet().list().forEach(osSubnet -> {
                if (osNetAdminService.subnet(osSubnet.getId()) != null) {
                    osNetAdminService.updateSubnet(osSubnet);
                } else {
                    osNetAdminService.createSubnet(osSubnet);
                }
            });
        } catch (Exception e) {
            log.warn("Failed to retrieve subnet due to {}", e.getMessage());
            return Response.serverError().build();
        }

        try {
            osClient.headers(headerMap).networking().port().list().forEach(osPort -> {
                if (osNetAdminService.port(osPort.getId()) != null) {
                    osNetAdminService.updatePort(osPort);
                } else {
                    osNetAdminService.createPort(osPort);
                }
            });
        } catch (Exception e) {
            log.warn("Failed to retrieve port due to {}", e.getMessage());
            return Response.serverError().build();
        }

        try {
            osClient.headers(headerMap).networking().router().list().forEach(osRouter -> {
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
        } catch (Exception e) {
            log.warn("Failed to retrieve router due to {}", e.getMessage());
            return Response.serverError().build();
        }

        try {
            osClient.headers(headerMap).networking().floatingip().list().forEach(osFloating -> {
                if (osRouterAdminService.floatingIp(osFloating.getId()) != null) {
                    osRouterAdminService.updateFloatingIp(osFloating);
                } else {
                    osRouterAdminService.createFloatingIp(osFloating);
                }
            });
        } catch (Exception e) {
            log.warn("Failed to retrieve floating IP due to {}", e.getMessage());
            return Response.serverError().build();
        }

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

        syncRulesBase();
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

        if (purgeRulesBase()) {
            return ok(mapper().createObjectNode()).build();
        } else {
            return Response.serverError().build();
        }
    }

    /**
     * Configures the ARP mode (proxy | broadcast).
     *
     * @param arpmode ARP mode
     * @return 200 OK with config result, 404 not found
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("config/arpmode/{arpmode}")
    public Response configArpMode(@PathParam("arpmode") String arpmode) {

        String arpModeStr = nullIsIllegal(arpmode, ARP_MODE_REQUIRED);
        if (checkArpMode(arpModeStr)) {
            configArpModeBase(arpModeStr);

            ComponentConfigService service = get(ComponentConfigService.class);
            String switchingComponent = OpenstackSwitchingArpHandler.class.getName();
            String routingComponent = OpenstackRoutingArpHandler.class.getName();

            // we check the arpMode configured in each component, and purge and
            // reinstall all rules only if the arpMode is changed to the configured one
            while (true) {
                String switchingValue =
                        getPropertyValue(service.getProperties(switchingComponent), ARP_MODE_NAME);
                String routingValue =
                        getPropertyValue(service.getProperties(routingComponent), ARP_MODE_NAME);

                if (arpModeStr.equals(switchingValue) && arpModeStr.equals(routingValue)) {
                    break;
                }
            }

            purgeRulesBase();
            syncRulesBase();
        } else {
            throw new IllegalArgumentException("The ARP mode is not valid");
        }

        return ok(mapper().createObjectNode()).build();
    }

    /**
     * Configures the stateful SNAT flag (enable | disable).
     *
     * @param statefulSnat stateful SNAT flag
     * @return 200 OK with config result, 404 not found
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("config/statefulSnat/{statefulSnat}")
    public Response configStatefulSnat(@PathParam("statefulSnat") String statefulSnat) {
        String statefulSnatStr = nullIsIllegal(statefulSnat, STATEFUL_SNAT_REQUIRED);
        boolean flag = checkActivationFlag(statefulSnatStr);
        configStatefulSnatBase(flag);

        ComponentConfigService service = get(ComponentConfigService.class);
        String snatComponent = OpenstackRoutingSnatHandler.class.getName();

        while (true) {
            boolean snatValue =
                    getPropertyValueAsBoolean(
                            service.getProperties(snatComponent), USE_STATEFUL_SNAT_NAME);

            if (flag == snatValue) {
                break;
            }
        }

        purgeRulesBase();
        syncRulesBase();

        return ok(mapper().createObjectNode()).build();
    }

    /**
     * Configures the security group (enable | disable).
     *
     * @param securityGroup security group activation flag
     * @return 200 OK with config result, 404 not found
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("config/securityGroup/{securityGroup}")
    public Response configSecurityGroup(@PathParam("securityGroup") String securityGroup) {
        String securityGroupStr = nullIsIllegal(securityGroup, SECURITY_GROUP_FLAG_REQUIRED);

        boolean flag = checkActivationFlag(securityGroupStr);

        ComponentConfigService service = get(ComponentConfigService.class);
        String securityGroupComponent = OpenstackSecurityGroupHandler.class.getName();

        service.setProperty(securityGroupComponent, USE_SECURITY_GROUP_NAME, String.valueOf(flag));

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

    /**
     * Configures the HA active-standby status.
     *
     * @param flag active-standby status
     * @return 200 OK or 400 BAD_REQUEST
     */
    @PUT
    @Path("active/status/{flag}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateActiveStatus(@PathParam("flag") String flag) {

        log.info("Update active status to {}", flag);

        if (FLAG_TRUE.equalsIgnoreCase(flag)) {
            osHaService.setActive(true);
        }

        if (FLAG_FALSE.equalsIgnoreCase(flag)) {
            osHaService.setActive(false);
        }

        return status(Response.Status.OK).build();
    }

    /**
     * Configures the HA active-standby status.
     *
     * @return 200 OK with HA status.
     *         True if the node runs in active mode, false otherwise
     */
    @GET
    @Path("active/status")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getActiveStatus() {
        return ok(mapper().createObjectNode().put(IS_ACTIVE, osHaService.isActive())).build();
    }

    /**
     * Obtains the active node's IP address.
     *
     * @return 200 OK with active node's IP address.
     */
    @GET
    @Path("active/ip")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getActiveIp() {
        return ok(mapper().createObjectNode()
                .put(ACTIVE_IP, osHaService.getActiveIp().toString())).build();
    }

    /**
     * Configures the HA active IP address.
     *
     * @param ip IP address of active node
     * @return 200 OK or 400 BAD_REQUEST
     */
    @PUT
    @Path("active/ip/{ip}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateActiveIp(@PathParam("ip") String ip) {

        log.info("Update active IP address to {}", ip);

        osHaService.setActiveIp(IpAddress.valueOf(ip));

        return status(Response.Status.OK).build();
    }

    private void syncRulesBase() {
        // we first initialize the COMPUTE node, in order to feed all instance ports
        // by referring to ports' information obtained from neutron server
        osNodeAdminService.completeNodes(COMPUTE).forEach(this::syncRulesBaseForNode);
        osNodeAdminService.completeNodes(GATEWAY).forEach(this::syncRulesBaseForNode);
    }

    private void syncRulesBaseForNode(OpenstackNode osNode) {
        OpenstackNode updated = osNode.updateState(NodeState.INIT);
        osNodeAdminService.updateNode(updated);

        boolean result = true;
        long timeoutExpiredMs = System.currentTimeMillis() + TIMEOUT_MS;

        while (osNodeAdminService.node(osNode.hostname()).state() != COMPLETE) {

            long  waitMs = timeoutExpiredMs - System.currentTimeMillis();

            try {
                sleep(SLEEP_MS);
            } catch (InterruptedException e) {
                log.error("Exception caused during node synchronization...");
            }

            if (osNodeAdminService.node(osNode.hostname()).state() == COMPLETE) {
                break;
            } else {
                osNodeAdminService.updateNode(updated);
                log.info("Failed to synchronize flow rules, retrying...");
            }

            if (waitMs <= 0) {
                result = false;
                break;
            }
        }

        if (result) {
            log.info("Successfully synchronize flow rules for node {}!", osNode.hostname());
        } else {
            log.warn("Failed to synchronize flow rules for node {}.", osNode.hostname());
        }
    }

    private boolean purgeRulesBase() {
        ApplicationId appId = coreService.getAppId(Constants.OPENSTACK_NETWORKING_APP_ID);
        if (appId == null) {
            throw new ItemNotFoundException("application not found");
        }

        flowRuleService.removeFlowRulesById(appId);

        boolean result = true;
        long timeoutExpiredMs = System.currentTimeMillis() + TIMEOUT_MS;

        // we make sure all flow rules are removed from the store
        while (stream(flowRuleService.getFlowEntriesById(appId)
                                     .spliterator(), false).count() > 0) {

            long  waitMs = timeoutExpiredMs - System.currentTimeMillis();

            try {
                sleep(SLEEP_MS);
            } catch (InterruptedException e) {
                log.error("Exception caused during rule purging...");
            }

            if (stream(flowRuleService.getFlowEntriesById(appId)
                                      .spliterator(), false).count() == 0) {
                break;
            } else {
                flowRuleService.removeFlowRulesById(appId);
                log.info("Failed to purging flow rules, retrying rule purging...");
            }

            if (waitMs <= 0) {
                result = false;
                break;
            }
        }

        if (result) {
            log.info("Successfully purged flow rules!");
        } else {
            log.warn("Failed to purge flow rules.");
        }

        return result;
    }

    private void configArpModeBase(String arpMode) {
        ComponentConfigService service = get(ComponentConfigService.class);
        String switchingComponent = OpenstackSwitchingArpHandler.class.getName();
        String routingComponent = OpenstackRoutingArpHandler.class.getName();

        service.setProperty(switchingComponent, ARP_MODE_NAME, arpMode);
        service.setProperty(routingComponent, ARP_MODE_NAME, arpMode);
    }

    private void configStatefulSnatBase(boolean snatFlag) {
        ComponentConfigService service = get(ComponentConfigService.class);
        String snatComponent = OpenstackRoutingSnatHandler.class.getName();

        service.setProperty(snatComponent, USE_STATEFUL_SNAT_NAME, String.valueOf(snatFlag));
    }
}
