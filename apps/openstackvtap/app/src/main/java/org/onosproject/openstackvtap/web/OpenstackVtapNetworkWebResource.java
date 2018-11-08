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
package org.onosproject.openstackvtap.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.openstackvtap.api.OpenstackVtapNetwork;
import org.onosproject.openstackvtap.api.OpenstackVtapService;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static org.onlab.util.Tools.readTreeFromStream;

/**
 * Handles REST API call of openstack vtap network.
 */
@Path("vtap-network")
public class OpenstackVtapNetworkWebResource extends AbstractWebResource {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String MESSAGE_NETWORK = "Received openstackVtapNetwork {} request";
    private static final String ERROR_DUPLICATE = "Already has data {}";
    private static final String ERROR_NOTFOUND = "No data to update {}";
    private static final String CREATE = "CREATE";
    private static final String READ = "READ";
    private static final String UPDATE = "UPDATE";
    private static final String DELETE = "DELETE";

    private static final String NETWORK = "network";

    private final OpenstackVtapService vtapService = get(OpenstackVtapService.class);

    /**
     * Creates a openstack vtap network from the JSON input stream.
     *
     * @param input openstack vtap network JSON input stream
     * @return 200 OK on creating success
     *         400 BAD_REQUEST if the JSON is malformed
     *         409 CONFLICT if already the openstack vtap network exists
     * @onos.rsModel OpenstackVtapNetwork
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createNetwork(InputStream input) {
        log.info(MESSAGE_NETWORK, CREATE);

        OpenstackVtapNetwork vtapNetwork = readNetworkConfiguration(input);
        if (vtapNetwork == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if (vtapService.createVtapNetwork(vtapNetwork.mode(),
                vtapNetwork.networkId(), vtapNetwork.serverIp()) == null) {
            log.warn(ERROR_DUPLICATE, vtapNetwork);
            return Response.status(Response.Status.CONFLICT).build();
        }
        return Response.ok().build();
    }

    /**
     * Updates openstack vtap network from the JSON input stream.
     *
     * @param input openstack vtap network JSON input stream
     * @return 200 OK on updating success
     *         400 BAD_REQUEST if the JSON is malformed
     *         404 NOT_FOUND if openstack vtap network is not exists
     * @onos.rsModel OpenstackVtapNetwork
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateNetwork(InputStream input) {
        log.info(MESSAGE_NETWORK, UPDATE);

        OpenstackVtapNetwork vtapNetwork = readNetworkConfiguration(input);
        if (vtapNetwork == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        if (vtapService.updateVtapNetwork(vtapNetwork) == null) {
            log.warn(ERROR_NOTFOUND, vtapNetwork);
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok().build();
    }

    /**
     * Removes openstack network.
     *
     * @return 200 OK on removing success
     *         404 NOT_FOUND if openstack vtap network is not exists
     * @onos.rsModel OpenstackVtapNetwork
     */
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteNetwork() {
        log.info(MESSAGE_NETWORK, DELETE);

        if (vtapService.removeVtapNetwork() == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok().build();
    }

    /**
     * Get openstack vtap network.
     *
     * @return 200 OK with openstack vtap network
     *         404 NOT_FOUND if openstack vtap network is not exists
     * @onos.rsModel OpenstackVtapNetwork
     */
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNetwork() {
        log.info(MESSAGE_NETWORK, READ);

        OpenstackVtapNetwork vtapNetwork = vtapService.getVtapNetwork();
        if (vtapNetwork == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        JsonNode jsonNode = codec(OpenstackVtapNetwork.class).encode(vtapNetwork, this);
        return Response.ok(jsonNode, MediaType.APPLICATION_JSON_TYPE).build();
    }

    private OpenstackVtapNetwork readNetworkConfiguration(InputStream input) {
        try {
            JsonNode jsonTree = readTreeFromStream(mapper().enable(INDENT_OUTPUT), input);
            ObjectNode vtap = (ObjectNode) jsonTree.get(NETWORK);
            return codec(OpenstackVtapNetwork.class).decode(vtap, this);
        } catch (Exception e) {
            log.error(e.toString());
            return null;
        }
    }

}
