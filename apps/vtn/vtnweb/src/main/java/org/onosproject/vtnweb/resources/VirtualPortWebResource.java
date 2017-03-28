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
import org.onlab.packet.MacAddress;
import org.onlab.util.ItemNotFoundException;
import org.onosproject.net.DeviceId;
import org.onosproject.rest.AbstractWebResource;
import org.onosproject.vtnrsc.AllowedAddressPair;
import org.onosproject.vtnrsc.BindingHostId;
import org.onosproject.vtnrsc.DefaultVirtualPort;
import org.onosproject.vtnrsc.FixedIp;
import org.onosproject.vtnrsc.SecurityGroup;
import org.onosproject.vtnrsc.SubnetId;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.TenantNetworkId;
import org.onosproject.vtnrsc.VirtualPort;
import org.onosproject.vtnrsc.VirtualPort.State;
import org.onosproject.vtnrsc.VirtualPortId;
import org.onosproject.vtnrsc.virtualport.VirtualPortService;
import org.onosproject.vtnweb.web.VirtualPortCodec;
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;

/**
 * REST resource for interacting with the inventory of infrastructure
 * virtualPort.
 */
@Path("ports")
public class VirtualPortWebResource extends AbstractWebResource {
    public static final String VPORT_NOT_FOUND = "VirtualPort is not found";
    public static final String VPORT_ID_EXIST = "VirtualPort id is exist";
    public static final String VPORT_ID_NOT_EXIST = "VirtualPort id is not exist";
    public static final String JSON_NOT_NULL = "JsonNode can not be null";
    protected static final Logger log = LoggerFactory
            .getLogger(VirtualPortService.class);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getPorts() {
        Iterable<VirtualPort> virtualPorts = get(VirtualPortService.class)
                .getPorts();
        ObjectNode result = new ObjectMapper().createObjectNode();
        result.set("ports", new VirtualPortCodec().encode(virtualPorts, this));
        return ok(result.toString()).build();
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getportsById(@PathParam("id") String id) {

        if (!get(VirtualPortService.class).exists(VirtualPortId.portId(id))) {
            return Response.status(NOT_FOUND)
                    .entity(VPORT_NOT_FOUND).build();
        }
        VirtualPort virtualPort = nullIsNotFound(get(VirtualPortService.class)
                .getPort(VirtualPortId.portId(id)), VPORT_NOT_FOUND);
        ObjectNode result = new ObjectMapper().createObjectNode();
        result.set("port", new VirtualPortCodec().encode(virtualPort, this));
        return ok(result.toString()).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createPorts(InputStream input) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode cfg = mapper.readTree(input);
            Iterable<VirtualPort> vPorts = createOrUpdateByInputStream(cfg);
            Boolean issuccess = nullIsNotFound(get(VirtualPortService.class)
                    .createPorts(vPorts), VPORT_NOT_FOUND);
            if (!issuccess) {
                return Response.status(INTERNAL_SERVER_ERROR)
                        .entity(VPORT_ID_NOT_EXIST).build();
            }
            return Response.status(OK).entity(issuccess.toString()).build();
        } catch (Exception e) {
            log.error("Creates VirtualPort failed because of exception {}",
                      e.toString());
            return Response.status(INTERNAL_SERVER_ERROR).entity(e.toString())
                    .build();
        }
    }

    @DELETE
    @Path("{portUUID}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deletePorts(@PathParam("portUUID") String id) {
        Set<VirtualPortId> vPortIds = new HashSet<>();
        try {
            if (id != null) {
                vPortIds.add(VirtualPortId.portId(id));
            }
            Boolean issuccess = nullIsNotFound(get(VirtualPortService.class)
                    .removePorts(vPortIds), VPORT_NOT_FOUND);
            if (!issuccess) {
                return Response.status(INTERNAL_SERVER_ERROR)
                        .entity(VPORT_ID_NOT_EXIST).build();
            }
            return ok(issuccess.toString()).build();
        } catch (Exception e) {
            log.error("Deletes VirtualPort failed because of exception {}",
                      e.toString());
            return Response.status(INTERNAL_SERVER_ERROR).entity(e.toString())
                    .build();
        }
    }

    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updatePorts(@PathParam("id") String id, InputStream input) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode cfg = mapper.readTree(input);
            Iterable<VirtualPort> vPorts = createOrUpdateByInputStream(cfg);
            Boolean issuccess = nullIsNotFound(get(VirtualPortService.class)
                    .updatePorts(vPorts), VPORT_NOT_FOUND);
            if (!issuccess) {
                return Response.status(INTERNAL_SERVER_ERROR)
                        .entity(VPORT_ID_NOT_EXIST).build();
            }
            return Response.status(OK).entity(issuccess.toString()).build();
        } catch (Exception e) {
            log.error("Updates failed because of exception {}", e.toString());
            return Response.status(INTERNAL_SERVER_ERROR).entity(e.toString())
                    .build();
        }
    }

    /**
     * Returns a Object of the currently known infrastructure virtualPort.
     *
     * @param vPortNode the virtualPort json node
     * @return a collection of virtualPorts
     */
    public Iterable<VirtualPort> createOrUpdateByInputStream(JsonNode vPortNode) {
        checkNotNull(vPortNode, JSON_NOT_NULL);
        JsonNode vPortNodes = vPortNode.get("ports");
        if (vPortNodes == null) {
            vPortNodes = vPortNode.get("port");
        }
        if (vPortNodes.isArray()) {
            return changeJsonToPorts(vPortNodes);
        } else {
            return changeJsonToPort(vPortNodes);
        }
    }

    /**
     * Returns the iterable collection of virtualports from subnetNodes.
     *
     * @param vPortNodes the virtualPort json node
     * @return virtualPorts a collection of virtualPorts
     */
    public Iterable<VirtualPort> changeJsonToPorts(JsonNode vPortNodes) {
        checkNotNull(vPortNodes, JSON_NOT_NULL);
        Map<VirtualPortId, VirtualPort> portMap = new HashMap<>();
        Map<String, String> strMap = new HashMap<>();
        for (JsonNode vPortnode : vPortNodes) {
            VirtualPortId id = VirtualPortId.portId(vPortnode.get("id")
                    .asText());
            String name = vPortnode.get("name").asText();
            TenantId tenantId = TenantId.tenantId(vPortnode.get("tenant_id")
                    .asText());
            TenantNetworkId networkId = TenantNetworkId.networkId(vPortnode
                    .get("network_id").asText());
            checkArgument(vPortnode.get("admin_state_up").isBoolean(), "admin_state_up should be boolean");
            Boolean adminStateUp = vPortnode.get("admin_state_up").asBoolean();
            String state = vPortnode.get("status").asText();
            MacAddress macAddress = MacAddress.valueOf(vPortnode
                    .get("mac_address").asText());
            DeviceId deviceId = DeviceId.deviceId(vPortnode.get("device_id")
                    .asText());
            String deviceOwner = vPortnode.get("device_owner").asText();
            JsonNode fixedIpNodes = vPortNodes.get("fixed_ips");
            Set<FixedIp> fixedIps = new HashSet<>();
            for (JsonNode fixedIpNode : fixedIpNodes) {
                FixedIp fixedIp = jsonNodeToFixedIps(fixedIpNode);
                fixedIps.add(fixedIp);
            }

            BindingHostId bindingHostId = BindingHostId
                    .bindingHostId(vPortnode.get("binding:host_id").asText());
            String bindingVnicType = vPortnode.get("binding:vnic_type")
                    .asText();
            String bindingVifType = vPortnode.get("binding:vif_type").asText();
            String bindingVifDetails = vPortnode.get("binding:vif_details")
                    .asText();
            JsonNode allowedAddressPairJsonNode = vPortnode
                    .get("allowed_address_pairs");
            Collection<AllowedAddressPair> allowedAddressPairs =
                    jsonNodeToAllowedAddressPair(allowedAddressPairJsonNode);
            JsonNode securityGroupNode = vPortnode.get("security_groups");
            Collection<SecurityGroup> securityGroups = jsonNodeToSecurityGroup(securityGroupNode);
            strMap.put("name", name);
            strMap.put("deviceOwner", deviceOwner);
            strMap.put("bindingVnicType", bindingVnicType);
            strMap.put("bindingVifType", bindingVifType);
            strMap.put("bindingVifDetails", bindingVifDetails);
            VirtualPort vPort = new DefaultVirtualPort(id, networkId,
                                                       adminStateUp, strMap,
                                                       isState(state),
                                                       macAddress, tenantId,
                                                       deviceId, fixedIps,
                                                       bindingHostId,
                                                       Sets.newHashSet(allowedAddressPairs),
                                                       Sets.newHashSet(securityGroups));
            portMap.put(id, vPort);
        }
        return Collections.unmodifiableCollection(portMap.values());
    }

    /**
     * Returns a collection of virtualPorts from subnetNodes.
     *
     * @param vPortNodes the virtualPort json node
     * @return virtualPorts a collection of virtualPorts
     */
    public Iterable<VirtualPort> changeJsonToPort(JsonNode vPortNodes) {
        checkNotNull(vPortNodes, JSON_NOT_NULL);
        Map<VirtualPortId, VirtualPort> vportMap = new HashMap<>();
        Map<String, String> strMap = new HashMap<>();
        VirtualPortId id = VirtualPortId.portId(vPortNodes.get("id").asText());
        String name = vPortNodes.get("name").asText();
        TenantId tenantId = TenantId.tenantId(vPortNodes.get("tenant_id")
                .asText());
        TenantNetworkId networkId = TenantNetworkId.networkId(vPortNodes
                .get("network_id").asText());
        Boolean adminStateUp = vPortNodes.get("admin_state_up").asBoolean();
        String state = vPortNodes.get("status").asText();
        MacAddress macAddress = MacAddress.valueOf(vPortNodes
                .get("mac_address").asText());
        DeviceId deviceId = DeviceId.deviceId(vPortNodes.get("device_id")
                .asText());
        String deviceOwner = vPortNodes.get("device_owner").asText();
        JsonNode fixedIpNodes = vPortNodes.get("fixed_ips");
        Set<FixedIp> fixedIps = new HashSet<>();
        for (JsonNode fixedIpNode : fixedIpNodes) {
            FixedIp fixedIp = jsonNodeToFixedIps(fixedIpNode);
            fixedIps.add(fixedIp);
        }

        BindingHostId bindingHostId = BindingHostId
                .bindingHostId(vPortNodes.get("binding:host_id").asText());
        String bindingVnicType = vPortNodes.get("binding:vnic_type").asText();
        String bindingVifType = vPortNodes.get("binding:vif_type").asText();
        String bindingVifDetails = vPortNodes.get("binding:vif_details")
                .asText();
        JsonNode allowedAddressPairJsonNode = vPortNodes
                .get("allowed_address_pairs");
        Collection<AllowedAddressPair> allowedAddressPairs =
                jsonNodeToAllowedAddressPair(allowedAddressPairJsonNode);
        JsonNode securityGroupNode = vPortNodes.get("security_groups");
        Collection<SecurityGroup> securityGroups = jsonNodeToSecurityGroup(securityGroupNode);
        strMap.put("name", name);
        strMap.put("deviceOwner", deviceOwner);
        strMap.put("bindingVnicType", bindingVnicType);
        strMap.put("bindingVifType", bindingVifType);
        strMap.put("bindingVifDetails", bindingVifDetails);
        VirtualPort vPort = new DefaultVirtualPort(id, networkId, adminStateUp,
                                                   strMap, isState(state),
                                                   macAddress, tenantId,
                                                   deviceId, fixedIps,
                                                   bindingHostId,
                                                   Sets.newHashSet(allowedAddressPairs),
                                                   Sets.newHashSet(securityGroups));
        vportMap.put(id, vPort);

        return Collections.unmodifiableCollection(vportMap.values());
    }

    /**
     * Returns a Object of the currently known infrastructure virtualPort.
     *
     * @param allowedAddressPairs the allowedAddressPairs json node
     * @return a collection of allowedAddressPair
     */
    public Collection<AllowedAddressPair> jsonNodeToAllowedAddressPair(JsonNode allowedAddressPairs) {
        checkNotNull(allowedAddressPairs, JSON_NOT_NULL);
        ConcurrentMap<Integer, AllowedAddressPair> allowMaps = Maps
                .newConcurrentMap();
        int i = 0;
        for (JsonNode node : allowedAddressPairs) {
            IpAddress ip = IpAddress.valueOf(node.get("ip_address").asText());
            MacAddress mac = MacAddress.valueOf(node.get("mac_address")
                    .asText());
            AllowedAddressPair allows = AllowedAddressPair
                    .allowedAddressPair(ip, mac);
            allowMaps.put(i, allows);
            i++;
        }
        log.debug("The jsonNode of allowedAddressPairallow is {}"
                + allowedAddressPairs.toString());
        return Collections.unmodifiableCollection(allowMaps.values());
    }

    /**
     * Returns a collection of virtualPorts.
     *
     * @param securityGroups the virtualPort jsonnode
     * @return a collection of securityGroups
     */
    public Collection<SecurityGroup> jsonNodeToSecurityGroup(JsonNode securityGroups) {
        checkNotNull(securityGroups, JSON_NOT_NULL);
        ConcurrentMap<Integer, SecurityGroup> securMaps = Maps
                .newConcurrentMap();
        int i = 0;
        for (JsonNode node : securityGroups) {
            SecurityGroup securityGroup = SecurityGroup
                    .securityGroup(node.asText());
            securMaps.put(i, securityGroup);
            i++;
        }
        return Collections.unmodifiableCollection(securMaps.values());
    }

    /**
     * Returns a collection of fixedIps.
     *
     * @param fixedIpNode the fixedIp jsonnode
     * @return a collection of SecurityGroup
     */
    public FixedIp jsonNodeToFixedIps(JsonNode fixedIpNode) {
        SubnetId subnetId = SubnetId.subnetId(fixedIpNode.get("subnet_id")
                .asText());
        IpAddress ipAddress = IpAddress.valueOf(fixedIpNode.get("ip_address")
                .asText());
        FixedIp fixedIps = FixedIp.fixedIp(subnetId, ipAddress);
        return fixedIps;
    }

    /**
     * Returns VirtualPort State.
     *
     * @param state the virtualport state
     * @return the virtualPort state
     */
    private State isState(String state) {
        if ("ACTIVE".equals(state)) {
            return VirtualPort.State.ACTIVE;
        } else {
            return VirtualPort.State.DOWN;
        }

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
