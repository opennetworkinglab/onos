/*
 * Copyright 2020-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.kubevirtnetworking.api.KubevirtNetwork;
import org.onosproject.kubevirtnetworking.api.KubevirtNetworkAdminService;
import org.onosproject.rest.AbstractWebResource;
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
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import static org.onlab.util.Tools.nullIsNotFound;
import static org.onlab.util.Tools.readTreeFromStream;

/**
 * Handles REST API call from CNI plugin.
 */
@Path("network")
public class KubevirtNetworkWebResource extends AbstractWebResource {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private static final String MESSAGE = "Received network %s request";
    private static final String NETWORK_NOT_FOUND = "Network is not found for";
    private static final String NETWORK_INVALID = "Invalid networkId in network update request";

    private static final String RESULT = "result";

    /**
     * Creates a network from the JSON input stream.
     *
     * @param input network JSON input stream
     * @return 201 CREATED if the JSON is correct, 400 BAD_REQUEST if the JSON
     * is invalid or duplicated network already exists
     * @onos.rsModel KubevirtNetwork
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createNetwork(InputStream input) {
        log.trace(String.format(MESSAGE, "CREATE"));
        KubevirtNetworkAdminService service = get(KubevirtNetworkAdminService.class);
        URI location;

        try {
            ObjectNode jsonTree = readTreeFromStream(mapper(), input);
            final KubevirtNetwork network =
                    codec(KubevirtNetwork.class).decode(jsonTree, this);
            service.createNetwork(network);
            location = new URI(network.networkId());
        } catch (IOException | URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }

        return Response.created(location).build();
    }

    /**
     * Updates the network with the specified identifier.
     *
     * @param id    network identifier
     * @param input network JSON input stream
     * @return 200 OK with the updated network, 400 BAD_REQUEST if the requested
     * network does not exist
     * @onos.rsModel KubevirtNetwork
     */
    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateNetwork(@PathParam("id") String id, InputStream input) {
        log.trace(String.format(MESSAGE, "UPDATED"));
        KubevirtNetworkAdminService service = get(KubevirtNetworkAdminService.class);

        try {
            ObjectNode jsonTree = readTreeFromStream(mapper(), input);
            JsonNode specifiedNetworkId = jsonTree.get("networkId");

            if (specifiedNetworkId != null && !specifiedNetworkId.asText().equals(id)) {
                throw new IllegalArgumentException(NETWORK_INVALID);
            }

            final KubevirtNetwork network =
                    codec(KubevirtNetwork.class).decode(jsonTree, this);
            service.updateNetwork(network);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }

        return Response.ok().build();
    }

    /**
     * Removes the network with the given id.
     *
     * @param id network identifier
     * @return 204 NO_CONTENT, 400 BAD_REQUEST if the network does not exist
     */
    @DELETE
    @Path("{id}")
    public Response removeNetwork(@PathParam("id") String id) {
        log.trace(String.format(MESSAGE, "DELETE " + id));
        KubevirtNetworkAdminService service = get(KubevirtNetworkAdminService.class);

        service.removeNetwork(id);
        return Response.noContent().build();
    }

    /**
     * Checks whether the network exists with given network id.
     *
     * @param id network identifier
     * @return 200 OK with true/false result
     */
    @GET
    @Path("exist/{id}")
    public Response hasNetwork(@PathParam("id") String id) {
        log.trace(String.format(MESSAGE, "QUERY " + id));

        KubevirtNetworkAdminService service = get(KubevirtNetworkAdminService.class);

        ObjectNode root = mapper().createObjectNode();
        KubevirtNetwork network = service.network(id);

        if (network == null) {
            root.put(RESULT, false);
        } else {
            root.put(RESULT, true);
        }

        return Response.ok(root).build();
    }

    /**
     * Returns set of all networks.
     *
     * @return 200 OK with set of all networks
     * @onos.rsModel KubevirtNetworks
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNetworks() {
        KubevirtNetworkAdminService service = get(KubevirtNetworkAdminService.class);
        final Iterable<KubevirtNetwork> networks = service.networks();
        return ok(encodeArray(KubevirtNetwork.class, "networks", networks)).build();
    }

    /**
     * Returns the network with the specified identifier.
     *
     * @param id network identifier
     * @return 200 OK with a network, 404 not found
     * @onos.rsModel KubevirtNetwork
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id}")
    public Response getNetworkById(@PathParam("id") String id) {
        KubevirtNetworkAdminService service = get(KubevirtNetworkAdminService.class);
        final KubevirtNetwork network = nullIsNotFound(service.network(id),
                NETWORK_NOT_FOUND + id);
        return ok(codec(KubevirtNetwork.class).encode(network, this)).build();
    }
}
