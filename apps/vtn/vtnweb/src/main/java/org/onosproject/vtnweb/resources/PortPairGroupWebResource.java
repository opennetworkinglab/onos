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
import org.onosproject.vtnrsc.PortPairGroup;
import org.onosproject.vtnrsc.PortPairGroupId;
import org.onosproject.vtnrsc.portpairgroup.PortPairGroupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Query and program port pair group.
 */

@Path("port_pair_groups")
public class PortPairGroupWebResource extends AbstractWebResource {

    private final Logger log = LoggerFactory.getLogger(PortPairGroupWebResource.class);
    public static final String PORT_PAIR_GROUP_NOT_FOUND = "Port pair group not found";
    public static final String PORT_PAIR_GROUP_ID_EXIST = "Port pair group exists";
    public static final String PORT_PAIR_GROUP_ID_NOT_EXIST = "Port pair group does not exist with identifier";

    /**
     * Get details of all port pair groups created.
     *
     * @return 200 OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getPortPairGroups() {
        Iterable<PortPairGroup> portPairGroups = get(PortPairGroupService.class).getPortPairGroups();
        ObjectNode result = mapper().createObjectNode();
        ArrayNode portPairGroupEntry = result.putArray("port_pair_groups");
        if (portPairGroups != null) {
            for (final PortPairGroup portPairGroup : portPairGroups) {
                portPairGroupEntry.add(codec(PortPairGroup.class).encode(portPairGroup, this));
            }
        }
        return ok(result.toString()).build();
    }

    /**
     * Get details of a specified port pair group id.
     *
     * @param id port pair group id
     * @return 200 OK, 404 if given identifier does not exist
     */
    @GET
    @Path("{group_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getPortPairGroup(@PathParam("group_id") String id) {
        PortPairGroup portPairGroup = nullIsNotFound(get(PortPairGroupService.class)
                                                     .getPortPairGroup(PortPairGroupId.of(id)),
                                                     PORT_PAIR_GROUP_NOT_FOUND);

        ObjectNode result = mapper().createObjectNode();
        result.set("port_pair_group", codec(PortPairGroup.class).encode(portPairGroup, this));
        return ok(result.toString()).build();
    }

    /**
     * Creates a new port pair group.
     *
     * @param stream   port pair group from JSON
     * @return status of the request - CREATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createPortPairGroup(InputStream stream) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode jsonTree = (ObjectNode) mapper.readTree(stream);
            JsonNode port = jsonTree.get("port_pair_group");

            PortPairGroup portPairGroup = codec(PortPairGroup.class).decode((ObjectNode) port, this);
            Boolean issuccess = nullIsNotFound(get(PortPairGroupService.class).createPortPairGroup(portPairGroup),
                                               PORT_PAIR_GROUP_NOT_FOUND);
            return Response.status(OK).entity(issuccess.toString()).build();
        } catch (IOException e) {
            log.error("Exception while creating port pair group {}.", e.toString());
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Update details of a specified port pair group id.
     *
     * @param id port pair group id
     * @param stream port pair group from json
     * @return 200 OK, 404 if given identifier does not exist
     */
    @PUT
    @Path("{group_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updatePortPairGroup(@PathParam("group_id") String id,
                                        final InputStream stream) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode jsonTree = (ObjectNode) mapper.readTree(stream);
            JsonNode port = jsonTree.get("port_pair_group");
            PortPairGroup portPairGroup = codec(PortPairGroup.class).decode((ObjectNode) port, this);
            Boolean isSuccess = nullIsNotFound(get(PortPairGroupService.class).updatePortPairGroup(portPairGroup),
                                               PORT_PAIR_GROUP_NOT_FOUND);
            return Response.status(OK).entity(isSuccess.toString()).build();
        } catch (IOException e) {
            log.error("Update port pair group failed because of exception {}.", e.toString());
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Delete details of a specified port pair group id.
     *
     * @param id port pair group id
     * @return 204 NO CONTENT
     */
    @Path("{group_id}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deletePortPairGroup(@PathParam("group_id") String id) {
        log.debug("Deletes port pair group by identifier {}.", id);
        PortPairGroupId portPairGroupId = PortPairGroupId.of(id);
        Boolean issuccess = nullIsNotFound(get(PortPairGroupService.class).removePortPairGroup(portPairGroupId),
                                           PORT_PAIR_GROUP_NOT_FOUND);
        if (!issuccess) {
            log.debug("Port pair group identifier {} does not exist", id);
        }

        return Response.noContent().build();
    }
}
