/*
 * Copyright 2015 Open Networking Laboratory
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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

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

import org.onlab.util.ItemNotFoundException;
import org.onosproject.rest.AbstractWebResource;
import org.onosproject.vtnrsc.DefaultTenantNetwork;
import org.onosproject.vtnrsc.PhysicalNetwork;
import org.onosproject.vtnrsc.SegmentationId;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.TenantNetwork;
import org.onosproject.vtnrsc.TenantNetworkId;
import org.onosproject.vtnrsc.TenantNetwork.State;
import org.onosproject.vtnrsc.TenantNetwork.Type;
import org.onosproject.vtnrsc.tenantnetwork.TenantNetworkService;
import org.onosproject.vtnrsc.web.TenantNetworkCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;

/**
 * REST resource for interacting with the inventory of networks.
 */
@Path("networks")
public class TenantNetworkWebResource extends AbstractWebResource {
    public static final String NETWORK_NOT_FOUND = "Network is not found";
    public static final String NETWORK_ID_EXIST = "Network id is existed";
    public static final String NETWORK_ID_NOT_EXIST = "Network id is not existed";
    public static final String CREATE_NETWORK = "create network";
    public static final String UPDATE_NETWORK = "update network";
    public static final String DELETE_NETWORK = "delete network";
    public static final String JSON_NOT_NULL = "JsonNode can not be null";

    protected static final Logger log = LoggerFactory
            .getLogger(TenantNetworkWebResource.class);
    private final ConcurrentMap<TenantNetworkId, TenantNetwork> networksMap = Maps
            .newConcurrentMap();

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getNetworks(@QueryParam("id") String queryId,
                                @QueryParam("name") String queryName,
                                @QueryParam("admin_state_up") String queryadminStateUp,
                                @QueryParam("status") String querystate,
                                @QueryParam("shared") String queryshared,
                                @QueryParam("tenant_id") String querytenantId,
                                @QueryParam("router:external") String routerExternal,
                                @QueryParam("provider:network_type") String type,
                                @QueryParam("provider:physical_network") String physicalNetwork,
                                @QueryParam("provider:segmentation_id") String segmentationId) {
        Iterable<TenantNetwork> networks = get(TenantNetworkService.class)
                .getNetworks();
        Iterator<TenantNetwork> networkors = networks.iterator();
        while (networkors.hasNext()) {
            TenantNetwork network = networkors.next();
            if ((queryId == null || queryId.equals(network.id().toString()))
                    && (queryName == null || queryName.equals(network.name()))
                    && (queryadminStateUp == null || queryadminStateUp
                            .equals(network.adminStateUp()))
                    && (querystate == null || querystate.equals(network.state()
                            .toString()))
                    && (queryshared == null || queryshared.equals(network
                            .shared()))
                    && (querytenantId == null || querytenantId.equals(network
                            .tenantId().toString()))
                    && (routerExternal == null || routerExternal.equals(network
                            .routerExternal()))
                    && (type == null || type.equals(network.type()))
                    && (physicalNetwork == null || physicalNetwork
                            .equals(network.physicalNetwork()))
                    && (segmentationId == null || segmentationId.equals(network
                            .segmentationId()))) {
                networksMap.putIfAbsent(network.id(), network);
            }
        }
        networks = Collections.unmodifiableCollection(networksMap.values());
        ObjectNode result = new ObjectMapper().createObjectNode();
        result.set("networks", new TenantNetworkCodec().encode(networks, this));

        return ok(result.toString()).build();
    }

    private State isState(String state) {
        if (state.equals("ACTIVE")) {
            return TenantNetwork.State.ACTIVE;
        } else if (state.equals("BUILD")) {
            return TenantNetwork.State.BUILD;
        } else if (state.equals("DOWN")) {
            return TenantNetwork.State.DOWN;
        } else if (state.equals("ERROR")) {
            return TenantNetwork.State.ERROR;
        } else {
            return null;
        }
    }

    private Type isType(String type) {
        if (type.equals("LOCAL")) {
            return TenantNetwork.Type.LOCAL;
        } else {
            return null;
        }
    }

    @GET
    @Path("{id}")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getNetwork(@PathParam("id") String id) {

        if (!get(TenantNetworkService.class).exists(TenantNetworkId
                                                            .networkId(id))) {
            return Response.status(NOT_FOUND)
                    .entity(NETWORK_NOT_FOUND).build();
        }
        TenantNetwork network = nullIsNotFound(get(TenantNetworkService.class)
                .getNetwork(TenantNetworkId.networkId(id)), NETWORK_NOT_FOUND);
        ObjectNode result = new ObjectMapper().createObjectNode();
        result.set("network", new TenantNetworkCodec().encode(network, this));

        return ok(result.toString()).build();

    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createNetworks(InputStream input) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode cfg = mapper.readTree(input);
            JsonNode nodes = null;
            Iterable<TenantNetwork> networks = null;
            if (cfg.get("network") != null) {
                nodes = cfg.get("network");
                if (nodes.isArray()) {
                    networks = changeJson2objs(nodes);
                } else {
                    networks = changeJson2obj(CREATE_NETWORK, null, nodes);
                }
            } else if (cfg.get("networks") != null) {
                nodes = cfg.get("networks");
                networks = changeJson2objs(nodes);
            }
            Boolean issuccess = nullIsNotFound((get(TenantNetworkService.class)
                                                       .createNetworks(networks)),
                                               NETWORK_NOT_FOUND);

            if (!issuccess) {
                return Response.status(INTERNAL_SERVER_ERROR)
                        .entity(NETWORK_ID_EXIST).build();
            }
            return Response.status(OK).entity(issuccess.toString()).build();
        } catch (Exception e) {
            log.error("Creates tenantNetwork exception {}.", e.toString());
            return Response.status(INTERNAL_SERVER_ERROR).entity(e.toString())
                    .build();
        }
    }

    @PUT
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateNetworks(@PathParam("id") String id, InputStream input) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode cfg = mapper.readTree(input);
            JsonNode nodes = null;
            Iterable<TenantNetwork> networks = null;
            if (cfg.get("network") != null) {
                nodes = cfg.get("network");
                if (nodes.isArray()) {
                    networks = changeJson2objs(nodes);
                } else {
                    networks = changeJson2obj(UPDATE_NETWORK,
                                              TenantNetworkId.networkId(id),
                                              nodes);
                }
            } else if (cfg.get("networks") != null) {
                nodes = cfg.get("networks");
                networks = changeJson2objs(nodes);
            }
            Boolean issuccess = nullIsNotFound((get(TenantNetworkService.class)
                                                       .updateNetworks(networks)),
                                               NETWORK_NOT_FOUND);
            if (!issuccess) {
                return Response.status(INTERNAL_SERVER_ERROR)
                        .entity(NETWORK_ID_NOT_EXIST).build();
            }
            return Response.status(OK).entity(issuccess.toString()).build();
        } catch (Exception e) {
            log.error("Updates tenantNetwork failed because of exception {}.",
                      e.toString());
            return Response.status(INTERNAL_SERVER_ERROR).entity(e.toString())
                    .build();
        }
    }

    @DELETE
    @Path("{id}")
    public Response deleteNetworks(@PathParam("id") String id) {
        log.debug("Deletes network by identifier {}.", id);
        Set<TenantNetworkId> networkSet = new HashSet<>();
        networkSet.add(TenantNetworkId.networkId(id));
        Boolean issuccess = nullIsNotFound(get(TenantNetworkService.class)
                .removeNetworks(networkSet), NETWORK_NOT_FOUND);
        if (!issuccess) {
            log.debug("Network identifier {} is not existed", id);
            return Response.status(INTERNAL_SERVER_ERROR)
                    .entity(NETWORK_ID_NOT_EXIST).build();
        }
        return Response.status(OK).entity(issuccess.toString()).build();
    }

    /**
     * Returns a collection of tenantNetworks.
     *
     * @param flag the flag
     * @param networkId network identifier
     * @param node the network json node
     * @return a collection of tenantNetworks
     */
    public Iterable<TenantNetwork> changeJson2obj(String flag,
                                                  TenantNetworkId networkId,
                                                  JsonNode node) {
        checkNotNull(node, JSON_NOT_NULL);
        TenantNetwork network = null;
        ConcurrentMap<TenantNetworkId, TenantNetwork> networksMap = Maps
                .newConcurrentMap();
        if (node != null) {
            checkArgument(node.get("admin_state_up").isBoolean(), "admin_state_up should be boolean");
            checkArgument(node.get("shared").isBoolean(), "shared should be boolean");
            checkArgument(node.get("router:external").isBoolean(), "router:external should be boolean");
            String name = node.get("name").asText();
            boolean adminStateUp = node.get("admin_state_up").asBoolean();
            String state = node.get("status").asText();
            boolean shared = node.get("shared").asBoolean();
            String tenantId = node.get("tenant_id").asText();
            boolean routerExternal = node.get("router:external").asBoolean();
            String type = node.get("provider:network_type").asText();
            String physicalNetwork = node.get("provider:physical_network")
                    .asText();
            String segmentationId = node.get("provider:segmentation_id")
                    .asText();
            TenantNetworkId id = null;
            if (flag == CREATE_NETWORK) {
                id = TenantNetworkId.networkId(node.get("id").asText());
            } else if (flag == UPDATE_NETWORK) {
                id = networkId;
            }
            network = new DefaultTenantNetwork(
                                               id,
                                               name,
                                               adminStateUp,
                                               isState(state),
                                               shared,
                                               TenantId.tenantId(tenantId),
                                               routerExternal,
                                               isType(type),
                                               PhysicalNetwork
                                                       .physicalNetwork(physicalNetwork),
                                               SegmentationId
                                                       .segmentationId(segmentationId));
            networksMap.putIfAbsent(id, network);
        }
        return Collections.unmodifiableCollection(networksMap.values());
    }

    /**
     * Returns a collection of tenantNetworks.
     *
     * @param nodes the network jsonnodes
     * @return a collection of tenantNetworks
     */
    public Iterable<TenantNetwork> changeJson2objs(JsonNode nodes) {
        checkNotNull(nodes, JSON_NOT_NULL);
        TenantNetwork network = null;
        ConcurrentMap<TenantNetworkId, TenantNetwork> networksMap = Maps
                .newConcurrentMap();
        if (nodes != null) {
            for (JsonNode node : nodes) {
                String id = node.get("id").asText();
                String name = node.get("name").asText();
                boolean adminStateUp = node.get("admin_state_up").asBoolean();
                String state = node.get("status").asText();
                boolean shared = node.get("shared").asBoolean();
                String tenantId = node.get("tenant_id").asText();
                boolean routerExternal = node.get("router:external")
                        .asBoolean();
                String type = node.get("provider:network_type").asText();
                String physicalNetwork = node.get("provider:physical_network")
                        .asText();
                String segmentationId = node.get("provider:segmentation_id")
                        .asText();
                network = new DefaultTenantNetwork(
                                                   TenantNetworkId
                                                           .networkId(id),
                                                   name,
                                                   adminStateUp,
                                                   isState(state),
                                                   shared,
                                                   TenantId.tenantId(tenantId),
                                                   routerExternal,
                                                   isType(type),
                                                   PhysicalNetwork
                                                           .physicalNetwork(physicalNetwork),
                                                   SegmentationId
                                                           .segmentationId(segmentationId));
                networksMap.putIfAbsent(TenantNetworkId.networkId(id), network);
            }
        }
        return Collections.unmodifiableCollection(networksMap.values());
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
