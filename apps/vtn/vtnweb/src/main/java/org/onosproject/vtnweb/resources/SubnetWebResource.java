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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.onlab.packet.IpAddress;
import org.onlab.packet.IpAddress.Version;
import org.onlab.packet.IpPrefix;
import org.onlab.util.ItemNotFoundException;
import org.onosproject.rest.AbstractWebResource;
import org.onosproject.vtnrsc.AllocationPool;
import org.onosproject.vtnrsc.DefaultAllocationPool;
import org.onosproject.vtnrsc.DefaultHostRoute;
import org.onosproject.vtnrsc.DefaultSubnet;
import org.onosproject.vtnrsc.HostRoute;
import org.onosproject.vtnrsc.Subnet;
import org.onosproject.vtnrsc.SubnetId;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.TenantNetworkId;
import org.onosproject.vtnrsc.Subnet.Mode;
import org.onosproject.vtnrsc.subnet.SubnetService;
import org.onosproject.vtnweb.web.SubnetCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

@Path("subnets")
public class SubnetWebResource extends AbstractWebResource {
    private final Logger log = LoggerFactory.getLogger(SubnetWebResource.class);
    public static final String SUBNET_NOT_CREATED = "Subnet failed to create!";
    public static final String SUBNET_NOT_FOUND = "Subnet is not found";
    public static final String JSON_NOT_NULL = "JsonNode can not be null";

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response listSubnets() {
        Iterable<Subnet> subnets = get(SubnetService.class).getSubnets();
        ObjectNode result = new ObjectMapper().createObjectNode();
        result.set("subnets", new SubnetCodec().encode(subnets, this));
        return ok(result.toString()).build();
    }

    @GET
    @Path("{subnetUUID}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getSubnet(@PathParam("subnetUUID") String id) {

        if (!get(SubnetService.class).exists(SubnetId.subnetId(id))) {
            return Response.status(NOT_FOUND)
                    .entity(SUBNET_NOT_FOUND).build();
        }
        Subnet sub = nullIsNotFound(get(SubnetService.class)
                                            .getSubnet(SubnetId.subnetId(id)),
                                    SUBNET_NOT_FOUND);

        ObjectNode result = new ObjectMapper().createObjectNode();
        result.set("subnet", new SubnetCodec().encode(sub, this));
        return ok(result.toString()).build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createSubnet(final InputStream input) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode subnode = mapper.readTree(input);
            Iterable<Subnet> subnets = createOrUpdateByInputStream(subnode);
            Boolean result = nullIsNotFound((get(SubnetService.class)
                                                    .createSubnets(subnets)),
                                            SUBNET_NOT_CREATED);

            if (!result) {
                return Response.status(INTERNAL_SERVER_ERROR)
                        .entity(SUBNET_NOT_CREATED).build();
            }
            return Response.status(202).entity(result.toString()).build();
        } catch (Exception e) {
            return Response.status(INTERNAL_SERVER_ERROR).entity(e.toString())
                    .build();
        }
    }

    @PUT
    @Path("{subnetUUID}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateSubnet(@PathParam("id") String id,
                                 final InputStream input) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode subnode = mapper.readTree(input);
            Iterable<Subnet> subnets = createOrUpdateByInputStream(subnode);
            Boolean result = nullIsNotFound(get(SubnetService.class)
                    .updateSubnets(subnets), SUBNET_NOT_FOUND);
            if (!result) {
                return Response.status(INTERNAL_SERVER_ERROR)
                        .entity(SUBNET_NOT_FOUND).build();
            }
            return Response.status(203).entity(result.toString()).build();
        } catch (Exception e) {
            return Response.status(INTERNAL_SERVER_ERROR).entity(e.toString())
                    .build();
        }
    }

    @DELETE
    @Path("{subnetUUID}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteSingleSubnet(@PathParam("subnetUUID") String id)
            throws IOException {
        try {
            SubnetId subId = SubnetId.subnetId(id);
            Set<SubnetId> subIds = new HashSet<>();
            subIds.add(subId);
            get(SubnetService.class).removeSubnets(subIds);
            return Response.noContent().entity("SUCCESS").build();
        } catch (Exception e) {
            return Response.status(INTERNAL_SERVER_ERROR).entity(e.toString())
                    .build();
        }
    }

    private Iterable<Subnet> createOrUpdateByInputStream(JsonNode subnode) {
        checkNotNull(subnode, JSON_NOT_NULL);
        Iterable<Subnet> subnets = null;
        JsonNode subnetNodes = subnode.get("subnets");
        if (subnetNodes == null) {
            subnetNodes = subnode.get("subnet");
        }
        log.debug("subnetNodes is {}", subnetNodes.toString());
        if (subnetNodes.isArray()) {
            subnets = changeJsonToSubs(subnetNodes);
        } else {
            subnets = changeJsonToSub(subnetNodes);
        }
        return subnets;
    }

    /**
     * Returns a collection of subnets from subnetNodes.
     *
     * @param subnetNodes the subnet json node
     * @return subnets a collection of subnets
     */
    public Iterable<Subnet> changeJsonToSubs(JsonNode subnetNodes) {
        checkNotNull(subnetNodes, JSON_NOT_NULL);
        Map<SubnetId, Subnet> subMap = new HashMap<>();
        for (JsonNode subnetNode : subnetNodes) {
            if (!subnetNode.hasNonNull("id")) {
                return null;
            }
            SubnetId id = SubnetId.subnetId(subnetNode.get("id").asText());
            String subnetName = subnetNode.get("name").asText();
            TenantId tenantId = TenantId
                    .tenantId(subnetNode.get("tenant_id").asText());
            TenantNetworkId networkId = TenantNetworkId
                    .networkId(subnetNode.get("network_id").asText());
            String version = subnetNode.get("ip_version").asText();
            Version ipVersion;
            switch (version) {
            case "4":
                ipVersion = Version.INET;
                break;
            case "6":
                ipVersion = Version.INET;
                break;
            default:
                throw new IllegalArgumentException("ipVersion should be 4 or 6.");
            }
            IpPrefix cidr = IpPrefix.valueOf(subnetNode.get("cidr").asText());
            IpAddress gatewayIp = IpAddress
                    .valueOf(subnetNode.get("gateway_ip").asText());
            Boolean dhcpEnabled = subnetNode.get("enable_dhcp").asBoolean();
            Boolean shared = subnetNode.get("shared").asBoolean();
            JsonNode hostRoutes = subnetNode.get("host_routes");
            Iterable<HostRoute> hostRoutesIt = jsonNodeToHostRoutes(hostRoutes);
            JsonNode allocationPools = subnetNode.get("allocation_pools");
            Iterable<AllocationPool> allocationPoolsIt = jsonNodeToAllocationPools(allocationPools);
            Mode ipV6AddressMode = Mode
                    .valueOf(subnetNode.get("ipv6_address_mode").asText());
            Mode ipV6RaMode = Mode
                    .valueOf(subnetNode.get("ipv6_ra_mode").asText());
            Subnet subnet = new DefaultSubnet(id, subnetName, networkId,
                                              tenantId, ipVersion, cidr,
                                              gatewayIp, dhcpEnabled, shared,
                                              Sets.newHashSet(hostRoutesIt), ipV6AddressMode,
                                              ipV6RaMode, Sets.newHashSet(allocationPoolsIt));
            subMap.put(id, subnet);
        }
        return Collections.unmodifiableCollection(subMap.values());
    }

    /**
     * Returns a collection of subnets from subnetNodes.
     *
     * @param subnetNodes the subnet json node
     * @return subnets a collection of subnets
     */
    public Iterable<Subnet> changeJsonToSub(JsonNode subnetNodes) {
        checkNotNull(subnetNodes, JSON_NOT_NULL);
        checkArgument(subnetNodes.get("enable_dhcp").isBoolean(), "enable_dhcp should be boolean");
        checkArgument(subnetNodes.get("shared").isBoolean(), "shared should be boolean");
        Map<SubnetId, Subnet> subMap = new HashMap<>();
        if (!subnetNodes.hasNonNull("id")) {
            return null;
        }
        SubnetId id = SubnetId.subnetId(subnetNodes.get("id").asText());
        String subnetName = subnetNodes.get("name").asText();
        TenantId tenantId = TenantId
                .tenantId(subnetNodes.get("tenant_id").asText());
        TenantNetworkId networkId = TenantNetworkId
                .networkId(subnetNodes.get("network_id").asText());
        String version = subnetNodes.get("ip_version").asText();
        Version ipVersion;
        switch (version) {
        case "4":
            ipVersion = Version.INET;
            break;
        case "6":
            ipVersion = Version.INET;
            break;
        default:
            throw new IllegalArgumentException("ipVersion should be 4 or 6.");
        }

        IpPrefix cidr = IpPrefix.valueOf(subnetNodes.get("cidr").asText());
        IpAddress gatewayIp = IpAddress
                .valueOf(subnetNodes.get("gateway_ip").asText());
        Boolean dhcpEnabled = subnetNodes.get("enable_dhcp").asBoolean();
        Boolean shared = subnetNodes.get("shared").asBoolean();
        JsonNode hostRoutes = subnetNodes.get("host_routes");
        Iterable<HostRoute> hostRoutesIt = jsonNodeToHostRoutes(hostRoutes);
        JsonNode allocationPools = subnetNodes.get("allocation_pools");
        Iterable<AllocationPool> allocationPoolsIt = jsonNodeToAllocationPools(allocationPools);

        Mode ipV6AddressMode = getMode(subnetNodes.get("ipv6_address_mode")
                .asText());
        Mode ipV6RaMode = getMode(subnetNodes.get("ipv6_ra_mode").asText());

        Subnet subnet = new DefaultSubnet(id, subnetName, networkId, tenantId,
                                          ipVersion, cidr, gatewayIp,
                                          dhcpEnabled, shared, Sets.newHashSet(hostRoutesIt),
                                          ipV6AddressMode, ipV6RaMode,
                                          Sets.newHashSet(allocationPoolsIt));
        subMap.put(id, subnet);
        return Collections.unmodifiableCollection(subMap.values());
    }

    /**
     * Gets ipv6_address_mode or ipv6_ra_mode type.
     *
     * @param mode the String value in JsonNode
     * @return ipV6Mode Mode of the ipV6Mode
     */
    private Mode getMode(String mode) {
        Mode ipV6Mode;
        if (mode == null) {
            return null;
        }
        switch (mode) {
        case "dhcpv6-stateful":
            ipV6Mode = Mode.DHCPV6_STATEFUL;
            break;
        case "dhcpv6-stateless":
            ipV6Mode = Mode.DHCPV6_STATELESS;
            break;
        case "slaac":
            ipV6Mode = Mode.SLAAC;
            break;
        default:
            ipV6Mode = null;
        }
        return ipV6Mode;
    }

    /**
     * Changes JsonNode alocPools to a collection of the alocPools.
     *
     * @param allocationPools the allocationPools JsonNode
     * @return a collection of allocationPools
     */
    public Iterable<AllocationPool> jsonNodeToAllocationPools(JsonNode allocationPools) {
        checkNotNull(allocationPools, JSON_NOT_NULL);
        ConcurrentMap<Integer, AllocationPool> alocplMaps = Maps
                .newConcurrentMap();
        Integer i = 0;
        for (JsonNode node : allocationPools) {
            IpAddress startIp = IpAddress.valueOf(node.get("start").asText());
            IpAddress endIp = IpAddress.valueOf(node.get("end").asText());
            AllocationPool alocPls = new DefaultAllocationPool(startIp, endIp);
            alocplMaps.putIfAbsent(i, alocPls);
            i++;
        }
        return Collections.unmodifiableCollection(alocplMaps.values());
    }

    /**
     * Changes hostRoutes JsonNode to a collection of the hostRoutes.
     *
     * @param hostRoutes the hostRoutes json node
     * @return a collection of hostRoutes
     */
    public Iterable<HostRoute> jsonNodeToHostRoutes(JsonNode hostRoutes) {
        checkNotNull(hostRoutes, JSON_NOT_NULL);
        ConcurrentMap<Integer, HostRoute> hostRouteMaps = Maps
                .newConcurrentMap();
        Integer i = 0;
        for (JsonNode node : hostRoutes) {
            IpAddress nexthop = IpAddress.valueOf(node.get("nexthop").asText());
            IpPrefix destination = IpPrefix.valueOf(node.get("destination")
                    .asText());
            HostRoute hostRoute = new DefaultHostRoute(nexthop, destination);
            hostRouteMaps.putIfAbsent(i, hostRoute);
            i++;
        }
        return Collections.unmodifiableCollection(hostRouteMaps.values());
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
