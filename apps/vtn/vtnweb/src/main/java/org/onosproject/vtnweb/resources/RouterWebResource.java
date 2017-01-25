/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.vtnweb.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.onlab.packet.IpAddress;
import org.onlab.util.ItemNotFoundException;
import org.onosproject.rest.AbstractWebResource;
import org.onosproject.vtnrsc.DefaultRouter;
import org.onosproject.vtnrsc.FixedIp;
import org.onosproject.vtnrsc.Router;
import org.onosproject.vtnrsc.Router.Status;
import org.onosproject.vtnrsc.RouterGateway;
import org.onosproject.vtnrsc.RouterId;
import org.onosproject.vtnrsc.RouterInterface;
import org.onosproject.vtnrsc.SubnetId;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.TenantNetworkId;
import org.onosproject.vtnrsc.VirtualPortId;
import org.onosproject.vtnrsc.router.RouterService;
import org.onosproject.vtnrsc.routerinterface.RouterInterfaceService;
import org.onosproject.vtnweb.web.RouterCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

@Path("routers")
public class RouterWebResource extends AbstractWebResource {
    private final Logger log = LoggerFactory.getLogger(RouterWebResource.class);
    public static final String CREATE_FAIL = "Router is failed to create!";
    public static final String UPDATE_FAIL = "Router is failed to update!";
    public static final String GET_FAIL = "Router is failed to get!";
    public static final String NOT_EXIST = "Router does not exist!";
    public static final String DELETE_SUCCESS = "Router delete success!";
    public static final String JSON_NOT_NULL = "JsonNode can not be null";
    public static final String INTFACR_ADD_SUCCESS = "Interface add success";
    public static final String INTFACR_DEL_SUCCESS = "Interface delete success";

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response listRouters() {
        Collection<Router> routers = get(RouterService.class).getRouters();
        ObjectNode result = new ObjectMapper().createObjectNode();
        result.set("routers", new RouterCodec().encode(routers, this));
        return ok(result.toString()).build();
    }

    @GET
    @Path("{routerUUID}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getRouter(@PathParam("routerUUID") String id,
                              @QueryParam("fields") List<String> fields) {

        if (!get(RouterService.class).exists(RouterId.valueOf(id))) {
            return Response.status(NOT_FOUND)
                    .entity("The Router does not exists").build();
        }
        Router sub = nullIsNotFound(get(RouterService.class)
                .getRouter(RouterId.valueOf(id)), NOT_EXIST);

        ObjectNode result = new ObjectMapper().createObjectNode();
        if (!fields.isEmpty()) {
            result.set("router",
                       new RouterCodec().extracFields(sub, this, fields));
        } else {
            result.set("router", new RouterCodec().encode(sub, this));
        }
        return ok(result.toString()).build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createRouter(final InputStream input) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode subnode = mapper.readTree(input);
            Collection<Router> routers = createOrUpdateByInputStream(subnode);

            Boolean result = nullIsNotFound((get(RouterService.class)
                    .createRouters(routers)), CREATE_FAIL);
            if (!result) {
                return Response.status(CONFLICT).entity(CREATE_FAIL).build();
            }
            return Response.status(CREATED).entity(result.toString()).build();

        } catch (Exception e) {
            return Response.status(BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @PUT
    @Path("{routerUUID}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateRouter(@PathParam("routerUUID") String id,
                                 final InputStream input) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode subnode = mapper.readTree(input);
            Collection<Router> routers = changeUpdateJsonToSub(subnode, id);
            Boolean result = nullIsNotFound(get(RouterService.class)
                    .updateRouters(routers), UPDATE_FAIL);
            if (!result) {
                return Response.status(CONFLICT).entity(UPDATE_FAIL).build();
            }
            return ok(result.toString()).build();
        } catch (Exception e) {
            return Response.status(BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @DELETE
    @Path("{routerUUID}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteSingleRouter(@PathParam("routerUUID") String id)
            throws IOException {
        try {
            RouterId routerId = RouterId.valueOf(id);
            Set<RouterId> routerIds = Sets.newHashSet(routerId);
            get(RouterService.class).removeRouters(routerIds);
            return Response.noContent().entity(DELETE_SUCCESS).build();
        } catch (Exception e) {
            return Response.status(BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @PUT
    @Path("{routerUUID}/add_router_interface")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addRouterInterface(@PathParam("routerUUID") String id,
                                       final InputStream input) {
        if (!get(RouterService.class).exists(RouterId.valueOf(id))) {
            return Response.status(NOT_FOUND).entity(NOT_EXIST).build();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode subnode = mapper.readTree(input);
            if (!subnode.hasNonNull("id")) {
                throw new IllegalArgumentException("id should not be null");
            } else if (subnode.get("id").asText().isEmpty()) {
                throw new IllegalArgumentException("id should not be empty");
            }
            RouterId routerId = RouterId.valueOf(id);
            if (!subnode.hasNonNull("subnet_id")) {
                throw new IllegalArgumentException("subnet_id should not be null");
            } else if (subnode.get("subnet_id").asText().isEmpty()) {
                throw new IllegalArgumentException("subnet_id should not be empty");
            }
            SubnetId subnetId = SubnetId
                    .subnetId(subnode.get("subnet_id").asText());
            if (!subnode.hasNonNull("tenant_id")) {
                throw new IllegalArgumentException("tenant_id should not be null");
            } else if (subnode.get("tenant_id").asText().isEmpty()) {
                throw new IllegalArgumentException("tenant_id should not be empty");
            }
            TenantId tenentId = TenantId
                    .tenantId(subnode.get("tenant_id").asText());
            if (!subnode.hasNonNull("port_id")) {
                throw new IllegalArgumentException("port_id should not be null");
            } else if (subnode.get("port_id").asText().isEmpty()) {
                throw new IllegalArgumentException("port_id should not be empty");
            }
            VirtualPortId portId = VirtualPortId
                    .portId(subnode.get("port_id").asText());
            RouterInterface routerInterface = RouterInterface
                    .routerInterface(subnetId, portId, routerId, tenentId);
            get(RouterInterfaceService.class)
                    .addRouterInterface(routerInterface);
            return ok(INTFACR_ADD_SUCCESS).build();
        } catch (Exception e) {
            return Response.status(BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @PUT
    @Path("{routerUUID}/remove_router_interface")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response removeRouterInterface(@PathParam("routerUUID") String id,
                                          final InputStream input) {
        if (!get(RouterService.class).exists(RouterId.valueOf(id))) {
            return Response.status(NOT_FOUND).entity(NOT_EXIST).build();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode subnode = mapper.readTree(input);
            if (!subnode.hasNonNull("id")) {
                throw new IllegalArgumentException("id should not be null");
            } else if (subnode.get("id").asText().isEmpty()) {
                throw new IllegalArgumentException("id should not be empty");
            }
            RouterId routerId = RouterId.valueOf(id);
            if (!subnode.hasNonNull("subnet_id")) {
                throw new IllegalArgumentException("subnet_id should not be null");
            } else if (subnode.get("subnet_id").asText().isEmpty()) {
                throw new IllegalArgumentException("subnet_id should not be empty");
            }
            SubnetId subnetId = SubnetId
                    .subnetId(subnode.get("subnet_id").asText());
            if (!subnode.hasNonNull("port_id")) {
                throw new IllegalArgumentException("port_id should not be null");
            } else if (subnode.get("port_id").asText().isEmpty()) {
                throw new IllegalArgumentException("port_id should not be empty");
            }
            VirtualPortId portId = VirtualPortId
                    .portId(subnode.get("port_id").asText());
            if (!subnode.hasNonNull("tenant_id")) {
                throw new IllegalArgumentException("tenant_id should not be null");
            } else if (subnode.get("tenant_id").asText().isEmpty()) {
                throw new IllegalArgumentException("tenant_id should not be empty");
            }
            TenantId tenentId = TenantId
                    .tenantId(subnode.get("tenant_id").asText());
            RouterInterface routerInterface = RouterInterface
                    .routerInterface(subnetId, portId, routerId, tenentId);
            get(RouterInterfaceService.class)
                    .removeRouterInterface(routerInterface);
            return ok(INTFACR_DEL_SUCCESS).build();
        } catch (Exception e) {
            return Response.status(BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    private Collection<Router> createOrUpdateByInputStream(JsonNode subnode)
            throws Exception {
        checkNotNull(subnode, JSON_NOT_NULL);
        JsonNode routerNode = subnode.get("routers");
        if (routerNode == null) {
            routerNode = subnode.get("router");
        }
        log.debug("routerNode is {}", routerNode.toString());

        if (routerNode.isArray()) {
            throw new Exception("only singleton requests allowed");
        } else {
            return changeJsonToSub(routerNode);
        }
    }

    /**
     * Returns a collection of floatingIps from floatingIpNodes.
     *
     * @param routerNode the router json node
     * @return routers a collection of router
     * @throws Exception when any argument is illegal
     */
    public Collection<Router> changeJsonToSub(JsonNode routerNode)
            throws Exception {
        checkNotNull(routerNode, JSON_NOT_NULL);
        Map<RouterId, Router> subMap = new HashMap<RouterId, Router>();
        if (!routerNode.hasNonNull("id")) {
            throw new IllegalArgumentException("id should not be null");
        } else if (routerNode.get("id").asText().isEmpty()) {
            throw new IllegalArgumentException("id should not be empty");
        }
        RouterId id = RouterId.valueOf(routerNode.get("id").asText());

        if (!routerNode.hasNonNull("tenant_id")) {
            throw new IllegalArgumentException("tenant_id should not be null");
        } else if (routerNode.get("tenant_id").asText().isEmpty()) {
            throw new IllegalArgumentException("tenant_id should not be empty");
        }
        TenantId tenantId = TenantId
                .tenantId(routerNode.get("tenant_id").asText());

        VirtualPortId gwPortId = null;
        if (routerNode.hasNonNull("gw_port_id")) {
            gwPortId = VirtualPortId
                    .portId(routerNode.get("gw_port_id").asText());
        }

        if (!routerNode.hasNonNull("status")) {
            throw new IllegalArgumentException("status should not be null");
        } else if (routerNode.get("status").asText().isEmpty()) {
            throw new IllegalArgumentException("status should not be empty");
        }
        Status status = Status.valueOf(routerNode.get("status").asText());

        String routerName = null;
        if (routerNode.hasNonNull("name")) {
            routerName = routerNode.get("name").asText();
        }

        boolean adminStateUp = true;
        checkArgument(routerNode.get("admin_state_up").isBoolean(),
                      "admin_state_up should be boolean");
        if (routerNode.hasNonNull("admin_state_up")) {
            adminStateUp = routerNode.get("admin_state_up").asBoolean();
        }
        boolean distributed = false;
        if (routerNode.hasNonNull("distributed")) {
            distributed = routerNode.get("distributed").asBoolean();
        }
        RouterGateway gateway = null;
        if (routerNode.hasNonNull("external_gateway_info")) {
            gateway = jsonNodeToGateway(routerNode
                    .get("external_gateway_info"));
        }
        List<String> routes = new ArrayList<String>();
        DefaultRouter routerObj = new DefaultRouter(id, routerName,
                                                    adminStateUp, status,
                                                    distributed, gateway,
                                                    gwPortId, tenantId, routes);
        subMap.put(id, routerObj);
        return Collections.unmodifiableCollection(subMap.values());
    }

    /**
     * Returns a collection of floatingIps from floatingIpNodes.
     *
     * @param subnode the router json node
     * @param routerId the router identify
     * @return routers a collection of router
     * @throws Exception when any argument is illegal
     */
    public Collection<Router> changeUpdateJsonToSub(JsonNode subnode,
                                                    String routerId)
                                                            throws Exception {
        checkNotNull(subnode, JSON_NOT_NULL);
        checkNotNull(routerId, "routerId should not be null");
        Map<RouterId, Router> subMap = new HashMap<RouterId, Router>();
        JsonNode routerNode = subnode.get("router");
        RouterId id = RouterId.valueOf(routerId);
        Router sub = nullIsNotFound(get(RouterService.class).getRouter(id),
                                    NOT_EXIST);
        TenantId tenantId = sub.tenantId();

        VirtualPortId gwPortId = null;
        if (routerNode.hasNonNull("gw_port_id")) {
            gwPortId = VirtualPortId
                    .portId(routerNode.get("gw_port_id").asText());
        }
        Status status = sub.status();

        String routerName = routerNode.get("name").asText();

        checkArgument(routerNode.get("admin_state_up").isBoolean(),
                      "admin_state_up should be boolean");
        boolean adminStateUp = routerNode.get("admin_state_up").asBoolean();

        boolean distributed = sub.distributed();
        if (routerNode.hasNonNull("distributed")) {
            distributed = routerNode.get("distributed").asBoolean();
        }
        RouterGateway gateway = sub.externalGatewayInfo();
        if (routerNode.hasNonNull("external_gateway_info")) {
            gateway = jsonNodeToGateway(routerNode
                    .get("external_gateway_info"));
        }
        List<String> routes = new ArrayList<String>();
        DefaultRouter routerObj = new DefaultRouter(id, routerName,
                                                    adminStateUp, status,
                                                    distributed, gateway,
                                                    gwPortId, tenantId, routes);
        subMap.put(id, routerObj);
        return Collections.unmodifiableCollection(subMap.values());
    }

    /**
     * Changes JsonNode Gateway to the Gateway.
     *
     * @param gateway the gateway JsonNode
     * @return gateway
     */
    private RouterGateway jsonNodeToGateway(JsonNode gateway) {
        checkNotNull(gateway, JSON_NOT_NULL);
        if (!gateway.hasNonNull("network_id")) {
            throw new IllegalArgumentException("network_id should not be null");
        } else if (gateway.get("network_id").asText().isEmpty()) {
            throw new IllegalArgumentException("network_id should not be empty");
        }
        TenantNetworkId networkId = TenantNetworkId
                .networkId(gateway.get("network_id").asText());

        if (!gateway.hasNonNull("enable_snat")) {
            throw new IllegalArgumentException("enable_snat should not be null");
        } else if (gateway.get("enable_snat").asText().isEmpty()) {
            throw new IllegalArgumentException("enable_snat should not be empty");
        }
        checkArgument(gateway.get("enable_snat").isBoolean(),
                      "enable_snat should be boolean");
        boolean enableSnat = gateway.get("enable_snat").asBoolean();

        if (!gateway.hasNonNull("external_fixed_ips")) {
            throw new IllegalArgumentException("external_fixed_ips should not be null");
        } else if (gateway.get("external_fixed_ips").isNull()) {
            throw new IllegalArgumentException("external_fixed_ips should not be empty");
        }
        Iterable<FixedIp> fixedIpList = jsonNodeToFixedIp(gateway
                .get("external_fixed_ips"));
        RouterGateway gatewayObj = RouterGateway
                .routerGateway(networkId, enableSnat, Sets.newHashSet(fixedIpList));
        return gatewayObj;
    }

    /**
     * Changes JsonNode fixedIp to a collection of the fixedIp.
     *
     * @param fixedIp the allocationPools JsonNode
     * @return a collection of fixedIp
     */
    private Iterable<FixedIp> jsonNodeToFixedIp(JsonNode fixedIp) {
        checkNotNull(fixedIp, JSON_NOT_NULL);
        ConcurrentMap<Integer, FixedIp> fixedIpMaps = Maps.newConcurrentMap();
        Integer i = 0;
        for (JsonNode node : fixedIp) {
            if (!node.hasNonNull("subnet_id")) {
                throw new IllegalArgumentException("subnet_id should not be null");
            } else if (node.get("subnet_id").asText().isEmpty()) {
                throw new IllegalArgumentException("subnet_id should not be empty");
            }
            SubnetId subnetId = SubnetId
                    .subnetId(node.get("subnet_id").asText());
            if (!node.hasNonNull("ip_address")) {
                throw new IllegalArgumentException("ip_address should not be null");
            } else if (node.get("ip_address").asText().isEmpty()) {
                throw new IllegalArgumentException("ip_address should not be empty");
            }
            IpAddress ipAddress = IpAddress
                    .valueOf(node.get("ip_address").asText());
            FixedIp fixedIpObj = FixedIp.fixedIp(subnetId, ipAddress);

            fixedIpMaps.putIfAbsent(i, fixedIpObj);
            i++;
        }
        return Collections.unmodifiableCollection(fixedIpMaps.values());
    }

    /**
     * Returns the specified item if that items is null; otherwise throws not
     * found exception.
     *
     * @param item item to check
     * @param <T> item type
     * @param message not found message
     * @return item if not null
     * @throws org.onlab.util.ItemNotFoundException if item is null
     */
    protected <T> T nullIsNotFound(T item, String message) {
        if (item == null) {
            throw new ItemNotFoundException(message);
        }
        return item;
    }
}
