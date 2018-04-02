/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.ofagent.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.ofagent.api.OFAgent;
import org.onosproject.ofagent.api.OFAgentAdminService;
import org.onosproject.ofagent.api.OFAgentService;
import org.onosproject.rest.AbstractWebResource;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.io.InputStream;

import static javax.ws.rs.core.Response.Status.*;
import static org.onlab.util.Tools.readTreeFromStream;


/**
 * Manage virtual switch and controller mapping.
 */
@Path("service")
public class OFAgentWebResource extends AbstractWebResource {

    private static final String OFAGENT_NOT_FOUND = "OFAgent not found";
    private static final String OFAGENTS_NOT_FOUND = "OFAgent set not found";
    private static final String OFAGENT_CREATED = "OFAgent created";
    private static final String OFAGENT_NOT_CREATED = "OFAgent not created";
    private static final String OFAGENT_STARTED = "OFAgent started";
    private static final String OFAGENT_NOT_STARTED = "OFAgent not started";
    private static final String OFAGENT_UPDATED = "OFAgent updated";
    private static final String OFAGENT_NOT_UPDATED = "OFAgent not updated";

     /**
     * Lists OpenFlow agents.
     * Shows OpenFlow agents for all virtual networks.
     *
     * @return 200 OK if set exists, 500 INTERNAL SERVER ERROR otherwise
     */
    @GET
    @Path("ofagents")
    public Response listOFAgents() {
        OFAgentService service = get(OFAgentService.class);
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        ArrayNode ofAgentsArray = mapper.createArrayNode();
        if (service.agents() == null) {
            return Response.status(INTERNAL_SERVER_ERROR)
                    .entity(OFAGENTS_NOT_FOUND).build();
        } else {
            service.agents().forEach(ofAgent -> ofAgentsArray.add((new OFAgentCodec()).encode(ofAgent, this)));

            root.set("ofAgents", ofAgentsArray);
            return Response.ok(root, MediaType.APPLICATION_JSON_TYPE).build();
        }

    }

    /**
     * Lists OpenFlow agent.
     * Shows OpenFlow agent for given network.
     *
     * @param networkId OFAgent networkId
     * @return 200 OK if OFAgent exists, 404 NOT FOUND otherwise
     */
    @GET
    @Path("ofagent/{networkId}")
    public Response listOFAgent(@PathParam("networkId") long networkId) {
        OFAgentService service = get(OFAgentService.class);
        OFAgent ofAgent = service.agent(NetworkId.networkId(networkId));
        if (ofAgent == null) {
            return Response.status(NOT_FOUND)
                    .entity(OFAGENT_NOT_FOUND).build();
        } else {
            return Response.ok((new OFAgentCodec()).encode(ofAgent, this), MediaType
                    .APPLICATION_JSON_TYPE)
                    .build();
        }
    }

    /**
     * Adds a new OpenFlow agent.
     * Creates a new OpenFlow agent and adds it to OpenFlow agent store.
     *
     * @param stream JSON stream
     * @return 201 CREATED , 400 BAD REQUEST if stream cannot be decoded to OFAgent
     * @throws IOException if request cannot be parsed
     */
    @POST
    @Path("ofagent-create")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createOFAgent(InputStream stream) throws IOException {
        OFAgentAdminService adminService = get(OFAgentAdminService.class);

        OFAgent ofAgent = (new OFAgentCodec()).decode(readTreeFromStream(mapper(), stream), this);
        if (ofAgent == null) {
            return Response.status(BAD_REQUEST)
                    .entity(OFAGENT_NOT_CREATED).build();
        } else {
            adminService.createAgent(ofAgent);
            return Response.status(CREATED).entity(OFAGENT_CREATED).build();
        }
    }

    /**
     * Starts OpenFlow agent.
     * Starts OpenFlow agent for the given network.
     *
     * @param stream JSON stream
     * @return 200 OK if OFAgent was started, 404 NOT FOUND when OF agent does not exist, 400 BAD REQUEST otherwise
     * @throws IOException if request cannot be parsed
     */
    @POST
    @Path("ofagent-start")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response startOFAgent(InputStream stream) throws IOException {
        OFAgentAdminService adminService = get(OFAgentAdminService.class);

        ObjectNode jsonTree = readTreeFromStream(mapper(), stream);
        JsonNode networkId = jsonTree.get("networkId");

        if (networkId == null) {
            return Response.status(BAD_REQUEST)
                    .entity(OFAGENT_NOT_STARTED).build();
        } else if (get(OFAgentService.class).agent(NetworkId.networkId(networkId.asLong())) == null) {
            return Response.status(NOT_FOUND)
                    .entity(OFAGENT_NOT_STARTED).build();
        } else {
            adminService.startAgent(NetworkId.networkId(networkId.asLong()));
            return Response.status(OK).entity(OFAGENT_STARTED).build();
        }
    }

    /**
     * Updates OpenFlow agent.
     * Updates existing OpenFlow agent for the given network.
     *
     * @param stream JSON stream
     * @return 200 OK if OFAgent was updated, 404 NOT FOUND when OF agent does not exist, 400 BAD REQUEST otherwise
     * @throws IOException if request cannot be parsed
     */
    @PUT
    @Path("ofagent-update")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateOFAgent(InputStream stream) throws IOException {
        OFAgentAdminService adminService = get(OFAgentAdminService.class);

        OFAgent ofAgent = (new OFAgentCodec()).decode(readTreeFromStream(mapper(), stream), this);

        if (ofAgent == null) {
            return Response.status(NOT_FOUND)
                    .entity(OFAGENT_NOT_UPDATED).build();
        } else if (get(OFAgentService.class).agent(ofAgent.networkId()) == null) {
            return Response.status(NOT_FOUND)
                    .entity(OFAGENT_NOT_UPDATED).build();
        }

        adminService.updateAgent(ofAgent);
        return Response.status(OK).entity(OFAGENT_UPDATED).build();
    }


    /**
     * Stops OFAgent.
     * Stops OFAgent for the given virtual network.
     *
     * @param stream JSON stream
     * @return 204 NO CONTENT if OpenFlow agent was stopped, 404 NOT FOUND otherwise
     * @throws IOException if request cannot be parsed
     */
    @POST
    @Path("ofagent-stop")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response stopOFAgent(InputStream stream) throws IOException {

        OFAgentAdminService adminService = get(OFAgentAdminService.class);
        ObjectNode jsonTree = readTreeFromStream(mapper(), stream);
        JsonNode networkId = jsonTree.get("networkId");

        if (get(OFAgentService.class).agent(NetworkId.networkId(networkId.asLong())) == null) {
            return Response.status(NOT_FOUND)
                    .entity(OFAGENT_NOT_FOUND).build();
        }

        adminService.stopAgent(NetworkId.networkId(networkId.asLong()));
        return Response.noContent().build();
    }


    /**
     * Deletes OFAgent.
     * Removes OFAgent for the given virtual network from repository.
     *
     * @param networkId OFAgent networkId
     * @return 200 OK if OFAgent was removed, 404 NOT FOUND when OF agent does not exist, 400 BAD REQUEST otherwise
     */
    @DELETE
    @Path("ofagent-remove/{networkId}")
    public Response removeOFAgent(@PathParam("networkId") long networkId) {
        if (get(OFAgentService.class).agent(NetworkId.networkId(networkId)) == null) {
            return Response.status(BAD_REQUEST)
                    .entity(OFAGENT_NOT_FOUND).build();
        }

        OFAgentAdminService adminService = get(OFAgentAdminService.class);
        OFAgent removed = adminService.removeAgent(NetworkId.networkId(networkId));
        if (removed != null) {
            return Response.ok((new OFAgentCodec()).encode(removed, this), MediaType
                    .APPLICATION_JSON_TYPE)
                    .build();
        } else {
            return Response.status(NOT_FOUND)
                    .entity(OFAGENT_NOT_FOUND).build();
        }
    }
}
