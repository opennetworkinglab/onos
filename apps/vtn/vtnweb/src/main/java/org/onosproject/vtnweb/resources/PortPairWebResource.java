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
import org.onosproject.vtnrsc.PortPair;
import org.onosproject.vtnrsc.PortPairId;
import org.onosproject.vtnrsc.portpair.PortPairService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Query and program port pair.
 */
@Path("port_pairs")
public class PortPairWebResource extends AbstractWebResource {

    private final Logger log = LoggerFactory.getLogger(PortPairWebResource.class);
    public static final String PORT_PAIR_NOT_FOUND = "Port pair not found";
    public static final String PORT_PAIR_ID_EXIST = "Port pair exists";
    public static final String PORT_PAIR_ID_NOT_EXIST = "Port pair does not exist with identifier";

    /**
     * Get details of all port pairs created.
     *
     * @return 200 OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getPortPairs() {
        Iterable<PortPair> portPairs = get(PortPairService.class).getPortPairs();
        ObjectNode result = mapper().createObjectNode();
        ArrayNode portPairEntry = result.putArray("port_pairs");
        if (portPairs != null) {
            for (final PortPair portPair : portPairs) {
                portPairEntry.add(codec(PortPair.class).encode(portPair, this));
            }
        }
        return ok(result.toString()).build();
    }

    /**
     * Get details of a specified port pair id.
     *
     * @param id port pair id
     * @return 200 OK, 404 if given identifier does not exist
     */
    @GET
    @Path("{pair_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getPortPair(@PathParam("pair_id") String id) {
        PortPair portPair = nullIsNotFound(get(PortPairService.class).getPortPair(PortPairId.of(id)),
                                           PORT_PAIR_NOT_FOUND);
        ObjectNode result = mapper().createObjectNode();
        result.set("port_pair", codec(PortPair.class).encode(portPair, this));
        return ok(result.toString()).build();
    }

    /**
     * Creates a new port pair.
     *
     * @param stream port pair from JSON
     * @return status of the request - CREATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createPortPair(InputStream stream) {
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            JsonNode port = jsonTree.get("port_pair");
            PortPair portPair = codec(PortPair.class).decode((ObjectNode) port, this);
            Boolean isSuccess = nullIsNotFound(get(PortPairService.class).createPortPair(portPair),
                                               PORT_PAIR_NOT_FOUND);
            return Response.status(OK).entity(isSuccess.toString()).build();
        } catch (IOException e) {
            log.error("Exception while creating port pair {}.", e.toString());
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Update details of a specified port pair id.
     *
     * @param id port pair id
     * @param stream port pair from json
     * @return 200 OK, 404 if the given identifier does not exist
     */
    @PUT
    @Path("{pair_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updatePortPair(@PathParam("pair_id") String id,
                                   final InputStream stream) {
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            JsonNode port = jsonTree.get("port_pair");
            PortPair portPair = codec(PortPair.class).decode((ObjectNode) port, this);
            Boolean isSuccess = nullIsNotFound(get(PortPairService.class).updatePortPair(portPair),
                                               PORT_PAIR_NOT_FOUND);
            return Response.status(OK).entity(isSuccess.toString()).build();
        } catch (IOException e) {
            log.error("Update port pair failed because of exception {}.", e.toString());
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Delete details of a specified port pair id.
     *
     * @param id port pair id
     * @return 204 NO CONTENT
     */
    @Path("{pair_id}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deletePortPair(@PathParam("pair_id") String id) {

        PortPairId portPairId = PortPairId.of(id);
        Boolean isSuccess = nullIsNotFound(get(PortPairService.class).removePortPair(portPairId), PORT_PAIR_NOT_FOUND);
        if (!isSuccess) {
            log.debug("Port pair identifier {} does not exist", id);
        }
        return Response.noContent().build();
    }
}
