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
package org.onosproject.rest.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.host.DefaultHostDescription;
import org.onosproject.net.host.HostProvider;
import org.onosproject.net.host.HostProviderRegistry;
import org.onosproject.net.host.HostProviderService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.rest.AbstractWebResource;

import javax.ws.rs.Consumes;
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
import java.net.URI;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.onlab.util.Tools.nullIsNotFound;
import static org.onosproject.net.HostId.hostId;

/**
 * Manage inventory of end-station hosts.
 */
@Path("hosts")
public class HostsWebResource extends AbstractWebResource {

    @Context
    private UriInfo uriInfo;
    private static final String HOST_NOT_FOUND = "Host is not found";
    private static final String[] REMOVAL_KEYS = {"mac", "vlan", "location", "ipAddresses"};

    /**
     * Get all end-station hosts.
     * Returns array of all known end-station hosts.
     *
     * @return 200 OK with array of all known end-station hosts.
     * @onos.rsModel Hosts
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getHosts() {
        final Iterable<Host> hosts = get(HostService.class).getHosts();
        final ObjectNode root = encodeArray(Host.class, "hosts", hosts);
        return ok(root).build();
    }

    /**
     * Get details of end-station host.
     * Returns detailed properties of the specified end-station host.
     *
     * @param id host identifier
     * @return 200 OK with detailed properties of the specified end-station host
     * @onos.rsModel Host
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id}")
    public Response getHostById(@PathParam("id") String id) {
        final Host host = nullIsNotFound(get(HostService.class).getHost(hostId(id)),
                                         HOST_NOT_FOUND);
        final ObjectNode root = codec(Host.class).encode(host, this);
        return ok(root).build();
    }

    /**
     * Get details of end-station host with MAC/VLAN.
     * Returns detailed properties of the specified end-station host.
     *
     * @param mac  host MAC address
     * @param vlan host VLAN identifier
     * @return 200 OK with detailed properties of the specified end-station host
     * @onos.rsModel Host
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{mac}/{vlan}")
    public Response getHostByMacAndVlan(@PathParam("mac") String mac,
                                        @PathParam("vlan") String vlan) {
        final Host host = nullIsNotFound(get(HostService.class).getHost(hostId(mac + "/" + vlan)),
                                         HOST_NOT_FOUND);
        final ObjectNode root = codec(Host.class).encode(host, this);
        return ok(root).build();
    }

    /**
     * Creates a new host based on JSON input and adds it to the current
     * host inventory.
     *
     * @param stream input JSON
     * @return status of the request - CREATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid
     * @onos.rsModel HostPut
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createAndAddHost(InputStream stream) {
        URI location;
        try {
            // Parse the input stream
            ObjectNode root = (ObjectNode) mapper().readTree(stream);

            HostProviderRegistry hostProviderRegistry = get(HostProviderRegistry.class);
            InternalHostProvider hostProvider = new InternalHostProvider();
            HostProviderService hostProviderService = hostProviderRegistry.register(hostProvider);
            hostProvider.setHostProviderService(hostProviderService);
            HostId hostId = hostProvider.parseHost(root);

            UriBuilder locationBuilder = uriInfo.getBaseUriBuilder()
                    .path("hosts")
                    .path(hostId.mac().toString())
                    .path(hostId.vlanId().toString());
            location = locationBuilder.build();
            hostProviderRegistry.unregister(hostProvider);

        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }
        return Response
                .created(location)
                .build();
    }

    /**
     * Internal host provider that provides host events.
     */
    private final class InternalHostProvider implements HostProvider {
        private final ProviderId providerId =
                new ProviderId("host", "org.onosproject.rest", true);
        private HostProviderService hostProviderService;

        /**
         * Prevents from instantiation.
         */
        private InternalHostProvider() {
        }

        public void triggerProbe(Host host) {
            // Not implemented since there is no need to check for hosts on network
        }

        public void setHostProviderService(HostProviderService service) {
            this.hostProviderService = service;
        }

        /*
         * Return the ProviderId of "this"
         */
        public ProviderId id() {
            return providerId;
        }

        /**
         * Creates and adds new host based on given data and returns its host ID.
         *
         * @param node JsonNode containing host information
         * @return host ID of new host created
         */
        private HostId parseHost(JsonNode node) {
            MacAddress mac = MacAddress.valueOf(node.get("mac").asText());
            VlanId vlanId = VlanId.vlanId((short) node.get("vlan").asInt(VlanId.UNTAGGED));
            JsonNode locationNode = node.get("location");
            String deviceAndPort = locationNode.get("elementId").asText() + "/" +
                    locationNode.get("port").asText();
            HostLocation hostLocation = new HostLocation(ConnectPoint.deviceConnectPoint(deviceAndPort), 0);

            Iterator<JsonNode> ipStrings = node.get("ipAddresses").elements();
            Set<IpAddress> ips = new HashSet<>();
            while (ipStrings.hasNext()) {
                ips.add(IpAddress.valueOf(ipStrings.next().asText()));
            }

            // try to remove elements from json node after reading them
            SparseAnnotations annotations = annotations(removeElements(node, REMOVAL_KEYS));
            // Update host inventory

            HostId hostId = HostId.hostId(mac, vlanId);
            DefaultHostDescription desc = new DefaultHostDescription(mac, vlanId, hostLocation, ips, true, annotations);
            hostProviderService.hostDetected(hostId, desc, false);
            return hostId;
        }

        /**
         * Remove a set of elements from JsonNode by specifying keys.
         *
         * @param node JsonNode containing host information
         * @param removalKeys key of elements that need to be removed
         * @return removal keys
         */
        private JsonNode removeElements(JsonNode node, String[] removalKeys) {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> map = mapper.convertValue(node, Map.class);
            for (String key : removalKeys) {
                map.remove(key);
            }
            return mapper.convertValue(map, JsonNode.class);
        }

        /**
         * Produces annotations from specified JsonNode. Copied from the ConfigProvider
         * class for use in the POST method.
         *
         * @param node node to be annotated
         * @return SparseAnnotations object with information about node
         */
        private SparseAnnotations annotations(JsonNode node) {
            if (node == null) {
                return DefaultAnnotations.EMPTY;
            }

            DefaultAnnotations.Builder builder = DefaultAnnotations.builder();
            Iterator<String> it = node.fieldNames();
            while (it.hasNext()) {
                String k = it.next();
                builder.set(k, node.get(k).asText());
            }
            return builder.build();
        }

    }
}

