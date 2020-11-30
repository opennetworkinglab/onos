/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.k8snetworking.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.k8snetworking.api.K8sNetworkAdminService;
import org.onosproject.k8snetworking.api.K8sPort;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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

import static org.onlab.util.Tools.readTreeFromStream;

/**
 * Handles port related REST API call from CNI plugin.
 */
@Path("port")
public class K8sPortWebResource extends AbstractWebResource {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private static final String MESSAGE = "Received port %s request";
    private static final String PORT_INVALID = "Invalid portId in port update request";

    private final K8sNetworkAdminService adminService = get(K8sNetworkAdminService.class);

    /**
     * Creates a port from the JSON input stream.
     *
     * @param input port JSON input stream
     * @return 201 CREATED if the JSON is correct, 400 BAD_REQUEST if the JSON
     * is invalid or duplicated port already exists
     * @onos.rsModel K8sPort
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createPort(InputStream input) {
        log.trace(String.format(MESSAGE, "CREATE"));
        URI location;

        try {
            ObjectNode jsonTree = readTreeFromStream(mapper(), input);
            final K8sPort port = codec(K8sPort.class).decode(jsonTree, this);
            adminService.createPort(port);
            location = new URI(port.portId());
        } catch (IOException | URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }

        return Response.created(location).build();
    }

    /**
     * Updates the port with the specified identifier.
     *
     * @param id    port identifier
     * @param input port JSON input stream
     * @return 200 OK with the updated port, 400 BAD_REQUEST if the requested
     * port does not exist
     * @onos.rsModel K8sPort
     */
    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updatePort(@PathParam("id") String id, InputStream input) {
        log.trace(String.format(MESSAGE, "UPDATED"));

        try {
            ObjectNode jsonTree = readTreeFromStream(mapper(), input);
            JsonNode specifiedPortId = jsonTree.get("portId");

            if (specifiedPortId != null && !specifiedPortId.asText().equals(id)) {
                throw new IllegalArgumentException(PORT_INVALID);
            }

            final K8sPort port = codec(K8sPort.class).decode(jsonTree, this);
            adminService.updatePort(port);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }

        return Response.ok().build();
    }

    /**
     * Removes the port with the given id.
     *
     * @param id port identifier
     * @return 204 NO_CONTENT, 400 BAD_REQUEST if the port does not exist
     */
    @DELETE
    @Path("{id}")
    public Response removePort(@PathParam("id") String id) {
        log.trace(String.format(MESSAGE, "DELETE " + id));

        adminService.removePort(id);
        return Response.noContent().build();
    }
}
