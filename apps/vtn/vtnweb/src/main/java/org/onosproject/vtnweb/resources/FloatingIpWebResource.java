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
import com.google.common.collect.Sets;
import org.onlab.packet.IpAddress;
import org.onlab.util.ItemNotFoundException;
import org.onosproject.rest.AbstractWebResource;
import org.onosproject.vtnrsc.DefaultFloatingIp;
import org.onosproject.vtnrsc.FloatingIp;
import org.onosproject.vtnrsc.FloatingIp.Status;
import org.onosproject.vtnrsc.FloatingIpId;
import org.onosproject.vtnrsc.RouterId;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.TenantNetworkId;
import org.onosproject.vtnrsc.VirtualPortId;
import org.onosproject.vtnrsc.floatingip.FloatingIpService;
import org.onosproject.vtnweb.web.FloatingIpCodec;
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CONFLICT;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

@Path("floatingips")
public class FloatingIpWebResource extends AbstractWebResource {
    private final Logger log = LoggerFactory
            .getLogger(FloatingIpWebResource.class);
    public static final String CREATE_FAIL = "Floating IP is failed to create!";
    public static final String UPDATE_FAIL = "Floating IP is failed to update!";
    public static final String DELETE_FAIL = "Floating IP is failed to delete!";
    public static final String GET_FAIL = "Floating IP is failed to get!";
    public static final String NOT_EXIST = "Floating IP does not exist!";
    public static final String DELETE_SUCCESS = "Floating IP delete success!";
    public static final String JSON_NOT_NULL = "JsonNode can not be null";

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response listFloatingIps() {
        Collection<FloatingIp> floatingIps = get(FloatingIpService.class)
                .getFloatingIps();
        ObjectNode result = new ObjectMapper().createObjectNode();
        result.set("floatingips",
                   new FloatingIpCodec().encode(floatingIps, this));
        return ok(result.toString()).build();
    }

    @GET
    @Path("{floatingIpUUID}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getFloatingIp(@PathParam("floatingIpUUID") String id,
                                  @QueryParam("fields") List<String> fields) {

        if (!get(FloatingIpService.class).exists(FloatingIpId.of(id))) {
            return Response.status(NOT_FOUND).entity(NOT_EXIST).build();
        }
        FloatingIp sub = nullIsNotFound(get(FloatingIpService.class)
                .getFloatingIp(FloatingIpId.of(id)), GET_FAIL);

        ObjectNode result = new ObjectMapper().createObjectNode();
        if (!fields.isEmpty()) {
            result.set("floatingip",
                       new FloatingIpCodec().extracFields(sub, this, fields));
        } else {
            result.set("floatingip", new FloatingIpCodec().encode(sub, this));
        }
        return ok(result.toString()).build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createFloatingIp(final InputStream input) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode subnode = mapper.readTree(input);
            Collection<FloatingIp> floatingIps = createOrUpdateByInputStream(subnode);
            Boolean result = nullIsNotFound((get(FloatingIpService.class)
                                                    .createFloatingIps(floatingIps)),
                                            CREATE_FAIL);
            if (!result) {
                return Response.status(CONFLICT).entity(CREATE_FAIL).build();
            }
            return Response.status(CREATED).entity(result.toString()).build();

        } catch (Exception e) {
            return Response.status(BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @PUT
    @Path("{floatingIpUUID}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateFloatingIp(@PathParam("floatingIpUUID") String id,
                                     final InputStream input) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode subnode = mapper.readTree(input);
            Collection<FloatingIp> floatingIps = createOrUpdateByInputStream(subnode);
            Boolean result = nullIsNotFound(get(FloatingIpService.class)
                    .updateFloatingIps(floatingIps), UPDATE_FAIL);
            if (!result) {
                return Response.status(CONFLICT).entity(UPDATE_FAIL).build();
            }
            return ok(result.toString()).build();
        } catch (Exception e) {
            return Response.status(BAD_REQUEST).entity(e.getMessage()).build();
        }
    }

    @DELETE
    @Path("{floatingIpUUID}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteSingleFloatingIp(@PathParam("floatingIpUUID") String id)
            throws IOException {
        try {
            FloatingIpId floatingIpId = FloatingIpId.of(id);
            Set<FloatingIpId> floatingIpIds = Sets.newHashSet(floatingIpId);
            Boolean result = nullIsNotFound(get(FloatingIpService.class)
                    .removeFloatingIps(floatingIpIds), DELETE_FAIL);
            if (!result) {
                return Response.status(CONFLICT).entity(DELETE_FAIL).build();
            }
            return Response.noContent().entity(DELETE_SUCCESS).build();
        } catch (Exception e) {
            return Response.status(NOT_FOUND).entity(e.getMessage()).build();
        }
    }

    private Collection<FloatingIp> createOrUpdateByInputStream(JsonNode subnode)
            throws Exception {
        checkNotNull(subnode, JSON_NOT_NULL);
        Collection<FloatingIp> floatingIps = null;
        JsonNode floatingIpNodes = subnode.get("floatingips");
        if (floatingIpNodes == null) {
            floatingIpNodes = subnode.get("floatingip");
        }
        log.debug("floatingNodes is {}", floatingIpNodes.toString());

        if (floatingIpNodes.isArray()) {
            throw new IllegalArgumentException("only singleton requests allowed");
        } else {
            floatingIps = changeJsonToSub(floatingIpNodes);
        }
        return floatingIps;
    }

    /**
     * Returns a collection of floatingIps from floatingIpNodes.
     *
     * @param floatingIpNodes the floatingIp json node
     * @return floatingIps a collection of floatingIp
     * @throws Exception when any argument is illegal
     */
    public Collection<FloatingIp> changeJsonToSub(JsonNode floatingIpNodes)
            throws Exception {
        checkNotNull(floatingIpNodes, JSON_NOT_NULL);
        Map<FloatingIpId, FloatingIp> subMap = new HashMap<FloatingIpId, FloatingIp>();
        if (!floatingIpNodes.hasNonNull("id")) {
            throw new IllegalArgumentException("id should not be null");
        } else if (floatingIpNodes.get("id").asText().isEmpty()) {
            throw new IllegalArgumentException("id should not be empty");
        }
        FloatingIpId id = FloatingIpId.of(floatingIpNodes.get("id")
                .asText());

        if (!floatingIpNodes.hasNonNull("tenant_id")) {
            throw new IllegalArgumentException("tenant_id should not be null");
        } else if (floatingIpNodes.get("tenant_id").asText().isEmpty()) {
            throw new IllegalArgumentException("tenant_id should not be empty");
        }
        TenantId tenantId = TenantId.tenantId(floatingIpNodes.get("tenant_id")
                .asText());

        if (!floatingIpNodes.hasNonNull("floating_network_id")) {
            throw new IllegalArgumentException(
                                          "floating_network_id should not be null");
        } else if (floatingIpNodes.get("floating_network_id").asText()
                .isEmpty()) {
            throw new IllegalArgumentException(
                                          "floating_network_id should not be empty");
        }
        TenantNetworkId networkId = TenantNetworkId.networkId(floatingIpNodes
                .get("floating_network_id").asText());

        VirtualPortId portId = null;
        if (floatingIpNodes.hasNonNull("port_id")) {
            portId = VirtualPortId.portId(floatingIpNodes.get("port_id")
                    .asText());
        }

        RouterId routerId = null;
        if (floatingIpNodes.hasNonNull("router_id")) {
            routerId = RouterId.valueOf(floatingIpNodes.get("router_id")
                    .asText());
        }

        IpAddress fixedIp = null;
        if (floatingIpNodes.hasNonNull("fixed_ip_address")) {
            fixedIp = IpAddress.valueOf(floatingIpNodes.get("fixed_ip_address")
                    .asText());
        }

        if (!floatingIpNodes.hasNonNull("floating_ip_address")) {
            throw new IllegalArgumentException(
                                          "floating_ip_address should not be null");
        } else if (floatingIpNodes.get("floating_ip_address").asText()
                .isEmpty()) {
            throw new IllegalArgumentException(
                                          "floating_ip_address should not be empty");
        }
        IpAddress floatingIp = IpAddress.valueOf(floatingIpNodes
                .get("floating_ip_address").asText());

        if (!floatingIpNodes.hasNonNull("status")) {
            throw new IllegalArgumentException("status should not be null");
        } else if (floatingIpNodes.get("status").asText().isEmpty()) {
            throw new IllegalArgumentException("status should not be empty");
        }
        Status status = Status.valueOf(floatingIpNodes.get("status").asText());

        DefaultFloatingIp floatingIpObj = new DefaultFloatingIp(id, tenantId,
                                                                networkId,
                                                                portId,
                                                                routerId,
                                                                floatingIp,
                                                                fixedIp, status);
        subMap.put(id, floatingIpObj);
        return Collections.unmodifiableCollection(subMap.values());
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
