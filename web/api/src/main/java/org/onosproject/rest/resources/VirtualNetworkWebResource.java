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

package org.onosproject.rest.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.TenantId;
import org.onosproject.incubator.net.virtual.VirtualDevice;
import org.onosproject.incubator.net.virtual.VirtualHost;
import org.onosproject.incubator.net.virtual.VirtualLink;
import org.onosproject.incubator.net.virtual.VirtualNetwork;
import org.onosproject.incubator.net.virtual.VirtualNetworkAdminService;
import org.onosproject.incubator.net.virtual.VirtualNetworkService;
import org.onosproject.incubator.net.virtual.VirtualPort;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.rest.AbstractWebResource;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Query and Manage Virtual Network elements.
 */
@Path("vnets")
public class VirtualNetworkWebResource extends AbstractWebResource {

    private static final String MISSING_FIELD = "Missing ";
    private static final String INVALID_FIELD = "Invalid ";

    private final VirtualNetworkAdminService vnetAdminService = get(VirtualNetworkAdminService.class);
    private final VirtualNetworkService vnetService = get(VirtualNetworkService.class);

    @Context
    private UriInfo uriInfo;

    // VirtualNetwork

    /**
     * Returns all virtual networks.
     *
     * @return 200 OK with set of virtual networks
     * @onos.rsModel VirtualNetworks
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVirtualNetworks() {
        Set<TenantId> tenantIds = vnetAdminService.getTenantIds();
        List<VirtualNetwork> allVnets = tenantIds.stream()
                .map(tenantId -> vnetService.getVirtualNetworks(tenantId))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        return ok(encodeArray(VirtualNetwork.class, "vnets", allVnets)).build();
    }

    /**
     * Returns the virtual networks with the specified tenant identifier.
     *
     * @param tenantId tenant identifier
     * @return 200 OK with a virtual network, 404 not found
     * @onos.rsModel VirtualNetworks
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{tenantId}")
    public Response getVirtualNetworkById(@PathParam("tenantId") String tenantId) {
        final TenantId existingTid = TenantWebResource.getExistingTenantId(vnetAdminService,
                                                                           TenantId.tenantId(tenantId));
        Set<VirtualNetwork> vnets = vnetService.getVirtualNetworks(existingTid);
        return ok(encodeArray(VirtualNetwork.class, "vnets", vnets)).build();
    }

    /**
     * Creates a virtual network from the JSON input stream.
     *
     * @param stream tenant identifier JSON stream
     * @return status of the request - CREATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid
     * @onos.rsModel TenantId
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createVirtualNetwork(InputStream stream) {
        try {
            final TenantId tid = TenantId.tenantId(getFromJsonStream(stream, "id").asText());
            VirtualNetwork newVnet = vnetAdminService.createVirtualNetwork(tid);
            UriBuilder locationBuilder = uriInfo.getBaseUriBuilder()
                    .path("vnets")
                    .path(newVnet.id().toString());
            return Response
                    .created(locationBuilder.build())
                    .build();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Removes the virtual network with the specified network identifier.
     *
     * @param networkId network identifier
     * @return 204 NO CONTENT
     */
    @DELETE
    @Path("{networkId}")
    public Response removeVirtualNetwork(@PathParam("networkId") long networkId) {
        NetworkId nid = NetworkId.networkId(networkId);
        vnetAdminService.removeVirtualNetwork(nid);
        return Response.noContent().build();
    }

    // VirtualDevice

    /**
     * Returns all virtual network devices in a virtual network.
     *
     * @param networkId network identifier
     * @return 200 OK with set of virtual devices, 404 not found
     * @onos.rsModel VirtualDevices
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{networkId}/devices")
    public Response getVirtualDevices(@PathParam("networkId") long networkId) {
        NetworkId nid = NetworkId.networkId(networkId);
        Set<VirtualDevice> vdevs = vnetService.getVirtualDevices(nid);
        return ok(encodeArray(VirtualDevice.class, "devices", vdevs)).build();
    }

    /**
     * Creates a virtual device from the JSON input stream.
     *
     * @param networkId network identifier
     * @param stream    virtual device JSON stream
     * @return status of the request - CREATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid
     * @onos.rsModel VirtualDevice
     */
    @POST
    @Path("{networkId}/devices")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createVirtualDevice(@PathParam("networkId") long networkId,
                                        InputStream stream) {
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            final VirtualDevice vdevReq = codec(VirtualDevice.class).decode(jsonTree, this);
            JsonNode specifiedNetworkId = jsonTree.get("networkId");
            if (specifiedNetworkId == null || specifiedNetworkId.asLong() != (networkId)) {
                throw new IllegalArgumentException(INVALID_FIELD + "networkId");
            }
            final VirtualDevice vdevRes = vnetAdminService.createVirtualDevice(vdevReq.networkId(),
                                                                               vdevReq.id());
            UriBuilder locationBuilder = uriInfo.getBaseUriBuilder()
                    .path("vnets").path(specifiedNetworkId.asText())
                    .path("devices").path(vdevRes.id().toString());
            return Response
                    .created(locationBuilder.build())
                    .build();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Removes the virtual network device from the virtual network.
     *
     * @param networkId network identifier
     * @param deviceId  device identifier
     * @return 204 NO CONTENT
     */
    @DELETE
    @Path("{networkId}/devices/{deviceId}")
    public Response removeVirtualDevice(@PathParam("networkId") long networkId,
                                        @PathParam("deviceId") String deviceId) {
        NetworkId nid = NetworkId.networkId(networkId);
        DeviceId did = DeviceId.deviceId(deviceId);
        vnetAdminService.removeVirtualDevice(nid, did);
        return Response.noContent().build();
    }

    // VirtualPort

    /**
     * Returns all virtual network ports in a virtual device in a virtual network.
     *
     * @param networkId network identifier
     * @param deviceId  virtual device identifier
     * @return 200 OK with set of virtual ports, 404 not found
     * @onos.rsModel VirtualPorts
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{networkId}/devices/{deviceId}/ports")
    public Response getVirtualPorts(@PathParam("networkId") long networkId,
                                    @PathParam("deviceId") String deviceId) {
        NetworkId nid = NetworkId.networkId(networkId);
        Iterable<VirtualPort> vports = vnetService.getVirtualPorts(nid, DeviceId.deviceId(deviceId));
        return ok(encodeArray(VirtualPort.class, "ports", vports)).build();
    }

    /**
     * Creates a virtual network port in a virtual device in a virtual network.
     *
     * @param networkId    network identifier
     * @param virtDeviceId virtual device identifier
     * @param stream       virtual port JSON stream
     * @return status of the request - CREATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid
     * @onos.rsModel VirtualPort
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{networkId}/devices/{deviceId}/ports")
    public Response createVirtualPort(@PathParam("networkId") long networkId,
                                      @PathParam("deviceId") String virtDeviceId,
                                      InputStream stream) {
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
//            final VirtualPort vportReq = codec(VirtualPort.class).decode(jsonTree, this);
            JsonNode specifiedNetworkId = jsonTree.get("networkId");
            JsonNode specifiedDeviceId = jsonTree.get("deviceId");
            if (specifiedNetworkId == null || specifiedNetworkId.asLong() != (networkId)) {
                throw new IllegalArgumentException(INVALID_FIELD + "networkId");
            }
            if (specifiedDeviceId == null || !specifiedDeviceId.asText().equals(virtDeviceId)) {
                throw new IllegalArgumentException(INVALID_FIELD + "deviceId");
            }
            JsonNode specifiedPortNum = jsonTree.get("portNum");
            JsonNode specifiedPhysDeviceId = jsonTree.get("physDeviceId");
            JsonNode specifiedPhysPortNum = jsonTree.get("physPortNum");
            final NetworkId nid = NetworkId.networkId(networkId);
            DeviceId vdevId = DeviceId.deviceId(virtDeviceId);

            ConnectPoint realizedBy = new ConnectPoint(DeviceId.deviceId(specifiedPhysDeviceId.asText()),
                                              PortNumber.portNumber(specifiedPhysPortNum.asText()));
            VirtualPort vport = vnetAdminService.createVirtualPort(nid, vdevId,
                                    PortNumber.portNumber(specifiedPortNum.asText()), realizedBy);
            UriBuilder locationBuilder = uriInfo.getBaseUriBuilder()
                    .path("vnets").path(specifiedNetworkId.asText())
                    .path("devices").path(specifiedDeviceId.asText())
                    .path("ports").path(vport.number().toString());
            return Response
                    .created(locationBuilder.build())
                    .build();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Removes the virtual network port from the virtual device in a virtual network.
     *
     * @param networkId network identifier
     * @param deviceId  virtual device identifier
     * @param portNum   virtual port number
     * @return 204 NO CONTENT
     */
    @DELETE
    @Path("{networkId}/devices/{deviceId}/ports/{portNum}")
    public Response removeVirtualPort(@PathParam("networkId") long networkId,
                                      @PathParam("deviceId") String deviceId,
                                      @PathParam("portNum") long portNum) {
        NetworkId nid = NetworkId.networkId(networkId);
        vnetAdminService.removeVirtualPort(nid, DeviceId.deviceId(deviceId),
                                           PortNumber.portNumber(portNum));
        return Response.noContent().build();
    }

    // VirtualLink

    /**
     * Returns all virtual network links in a virtual network.
     *
     * @param networkId network identifier
     * @return 200 OK with set of virtual network links
     * @onos.rsModel VirtualLinks
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{networkId}/links")
    public Response getVirtualLinks(@PathParam("networkId") long networkId) {
        NetworkId nid = NetworkId.networkId(networkId);
        Set<VirtualLink> vlinks = vnetService.getVirtualLinks(nid);
        return ok(encodeArray(VirtualLink.class, "links", vlinks)).build();
    }

    /**
     * Creates a virtual network link from the JSON input stream.
     *
     * @param networkId network identifier
     * @param stream    virtual link JSON stream
     * @return status of the request - CREATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid
     * @onos.rsModel VirtualLink
     */
    @POST
    @Path("{networkId}/links")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createVirtualLink(@PathParam("networkId") long networkId,
                                      InputStream stream) {
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            JsonNode specifiedNetworkId = jsonTree.get("networkId");
            if (specifiedNetworkId == null || specifiedNetworkId.asLong() != (networkId)) {
                throw new IllegalArgumentException(INVALID_FIELD + "networkId");
            }
            final VirtualLink vlinkReq = codec(VirtualLink.class).decode(jsonTree, this);
            vnetAdminService.createVirtualLink(vlinkReq.networkId(),
                                               vlinkReq.src(), vlinkReq.dst());
            UriBuilder locationBuilder = uriInfo.getBaseUriBuilder()
                    .path("vnets").path(specifiedNetworkId.asText())
                    .path("links");
            return Response
                    .created(locationBuilder.build())
                    .build();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Removes the virtual network link from the JSON input stream.
     *
     * @param networkId network identifier
     * @param stream    virtual link JSON stream
     * @return 204 NO CONTENT
     * @onos.rsModel VirtualLink
     */
    @DELETE
    @Path("{networkId}/links")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response removeVirtualLink(@PathParam("networkId") long networkId,
                                      InputStream stream) {
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            JsonNode specifiedNetworkId = jsonTree.get("networkId");
            if (specifiedNetworkId != null &&
                    specifiedNetworkId.asLong() != (networkId)) {
                throw new IllegalArgumentException(INVALID_FIELD + "networkId");
            }
            final VirtualLink vlinkReq = codec(VirtualLink.class).decode(jsonTree, this);
            vnetAdminService.removeVirtualLink(vlinkReq.networkId(),
                                               vlinkReq.src(), vlinkReq.dst());
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }

        return Response.noContent().build();
    }

    /**
     * Returns all virtual network hosts in a virtual network.
     *
     * @param networkId network identifier
     * @return 200 OK with set of virtual network hosts
     * @onos.rsModel VirtualHosts
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{networkId}/hosts")
    public Response getVirtualHosts(@PathParam("networkId") long networkId) {
        NetworkId nid = NetworkId.networkId(networkId);
        Set<VirtualHost> vhosts = vnetService.getVirtualHosts(nid);
        return ok(encodeArray(VirtualHost.class, "hosts", vhosts)).build();
    }

    /**
     * Creates a virtual network host from the JSON input stream.
     *
     * @param networkId network identifier
     * @param stream    virtual host JSON stream
     * @return status of the request - CREATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid
     * @onos.rsModel VirtualHostPut
     */
    @POST
    @Path("{networkId}/hosts")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createVirtualHost(@PathParam("networkId") long networkId,
                                      InputStream stream) {
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            JsonNode specifiedNetworkId = jsonTree.get("networkId");
            if (specifiedNetworkId == null || specifiedNetworkId.asLong() != (networkId)) {
                throw new IllegalArgumentException(INVALID_FIELD + "networkId");
            }
            final VirtualHost vhostReq = codec(VirtualHost.class).decode(jsonTree, this);
            vnetAdminService.createVirtualHost(vhostReq.networkId(), vhostReq.id(),
                                               vhostReq.mac(), vhostReq.vlan(),
                                               vhostReq.location(), vhostReq.ipAddresses());
            UriBuilder locationBuilder = uriInfo.getBaseUriBuilder()
                    .path("vnets").path(specifiedNetworkId.asText())
                    .path("hosts");
            return Response
                    .created(locationBuilder.build())
                    .build();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Removes the virtual network host from the JSON input stream.
     *
     * @param networkId network identifier
     * @param stream    virtual host JSON stream
     * @return 204 NO CONTENT
     * @onos.rsModel VirtualHost
     */
    @DELETE
    @Path("{networkId}/hosts")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response removeVirtualHost(@PathParam("networkId") long networkId,
                                      InputStream stream) {
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            JsonNode specifiedNetworkId = jsonTree.get("networkId");
            if (specifiedNetworkId != null &&
                    specifiedNetworkId.asLong() != (networkId)) {
                throw new IllegalArgumentException(INVALID_FIELD + "networkId");
            }
            final VirtualHost vhostReq = codec(VirtualHost.class).decode(jsonTree, this);
            vnetAdminService.removeVirtualHost(vhostReq.networkId(), vhostReq.id());
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }

        return Response.noContent().build();
    }

    /**
     * Get the tenant identifier from the JSON stream.
     *
     * @param stream        TenantId JSON stream
     * @param jsonFieldName field name
     * @return JsonNode
     * @throws IOException if unable to parse the request
     */
    private JsonNode getFromJsonStream(InputStream stream, String jsonFieldName) throws IOException {
        ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
        JsonNode jsonNode = jsonTree.get(jsonFieldName);

        if (jsonNode == null) {
            throw new IllegalArgumentException(MISSING_FIELD + jsonFieldName);
        }
        return jsonNode;
    }
}
