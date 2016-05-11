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

import static javax.ws.rs.core.Response.Status.OK;
import static org.onlab.util.Tools.nullIsNotFound;

import java.io.IOException;
import java.io.InputStream;

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

import org.onosproject.rest.AbstractWebResource;
import org.onosproject.vtnrsc.PortChain;
import org.onosproject.vtnrsc.PortChainId;
import org.onosproject.vtnrsc.portchain.PortChainService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Query and program port chain.
 */

@Path("port_chains")
public class PortChainWebResource extends AbstractWebResource {

    private final Logger log = LoggerFactory.getLogger(PortChainWebResource.class);
    public static final String PORT_CHAIN_NOT_FOUND = "Port chain not found";
    public static final String PORT_CHAIN_ID_EXIST = "Port chain exists";
    public static final String PORT_CHAIN_ID_NOT_EXIST = "Port chain does not exist with identifier";

    /**
     * Get details of all port chains created.
     *
     * @return 200 OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getPortChains() {
        Iterable<PortChain> portChains = get(PortChainService.class).getPortChains();
        ObjectNode result = mapper().createObjectNode();
        ArrayNode portChainEntry = result.putArray("port_chains");
        if (portChains != null) {
            for (final PortChain portChain : portChains) {
                portChainEntry.add(codec(PortChain.class).encode(portChain, this));
            }
        }
        return ok(result.toString()).build();
    }

    /**
     * Get details of a specified port chain id.
     *
     * @param id port chain id
     * @return 200 OK, 404 if given identifier does not exist
     */
    @GET
    @Path("{chain_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getPortPain(@PathParam("chain_id") String id) {

        PortChain portChain = nullIsNotFound(get(PortChainService.class).getPortChain(PortChainId.of(id)),
                                             PORT_CHAIN_NOT_FOUND);
        ObjectNode result = mapper().createObjectNode();
        result.set("port_chain", codec(PortChain.class).encode(portChain, this));
        return ok(result.toString()).build();
    }

    /**
     * Creates a new port chain.
     *
     * @param stream port chain from JSON
     * @return status of the request - CREATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createPortChain(InputStream stream) {
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            JsonNode port = jsonTree.get("port_chain");
            PortChain portChain = codec(PortChain.class).decode((ObjectNode) port, this);
            Boolean issuccess = nullIsNotFound(get(PortChainService.class).createPortChain(portChain),
                                               PORT_CHAIN_NOT_FOUND);
            return Response.status(OK).entity(issuccess.toString()).build();
        } catch (IOException e) {
            log.error("Exception while creating port chain {}.", e.toString());
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Update details of a specified port chain id.
     *
     * @param id port chain id
     * @param stream port chain json
     * @return 200 OK, 404 if given identifier does not exist
     */
    @PUT
    @Path("{chain_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updatePortPain(@PathParam("chain_id") String id,
                                   final InputStream stream) {
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            JsonNode port = jsonTree.get("port_chain");
            PortChain portChain = codec(PortChain.class).decode((ObjectNode) port, this);
            Boolean result = nullIsNotFound(get(PortChainService.class).updatePortChain(portChain),
                                            PORT_CHAIN_NOT_FOUND);
            return Response.status(OK).entity(result.toString()).build();
        } catch (IOException e) {
            log.error("Update port chain failed because of exception {}.", e.toString());
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Delete details of a specified port chain id.
     *
     * @param id port chain id
     * @return 204 NO CONTENT
     */
    @Path("{chain_id}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deletePortPain(@PathParam("chain_id") String id) {
        log.debug("Deletes port chain by identifier {}.", id);
        PortChainId portChainId = PortChainId.of(id);

        Boolean issuccess = nullIsNotFound(get(PortChainService.class).removePortChain(portChainId),
                                           PORT_CHAIN_NOT_FOUND);
        if (!issuccess) {
            log.debug("Port Chain identifier {} does not exist", id);
        }
        return Response.noContent().build();
    }
}
