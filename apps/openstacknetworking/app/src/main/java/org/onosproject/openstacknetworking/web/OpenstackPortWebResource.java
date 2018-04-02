/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.openstacknetworking.web;

import org.onosproject.openstacknetworking.api.OpenstackNetworkAdminService;
import org.onosproject.rest.AbstractWebResource;
import org.openstack4j.openstack.networking.domain.NeutronPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.InputStream;

import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.status;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.jsonToModelEntity;

/**
 * Handles Rest API call from Neutron ML2 plugin.
 */
@Path("ports")
public class OpenstackPortWebResource extends AbstractWebResource {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private static final String MESSAGE = "Received ports %s request";
    private static final String PORTS = "ports";

    private final OpenstackNetworkAdminService adminService =
                                        get(OpenstackNetworkAdminService.class);

    @Context
    private UriInfo uriInfo;

    /**
     * Creates a port from the JSON input stream.
     *
     * @param input port JSON input stream
     * @return 201 CREATED if the JSON is correct, 400 BAD_REQUEST if the JSON
     * is invalid or duplicated port already exists
     * @onos.rsModel NeutronPort
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createPorts(InputStream input) {
        log.trace(String.format(MESSAGE, "CREATE"));

        final NeutronPort port = (NeutronPort)
                                 jsonToModelEntity(input, NeutronPort.class);

        adminService.createPort(port);
        UriBuilder locationBuilder = uriInfo.getBaseUriBuilder()
                .path(PORTS)
                .path(port.getId());

        return created(locationBuilder.build()).build();
    }

    /**
     * Updates the port with the specified identifier.
     *
     * @param id    port identifier
     * @param input port JSON input stream
     * @return 200 OK with the updated port, 400 BAD_REQUEST if the requested
     * port does not exist
     * @onos.rsModel NeutronPort
     */
    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updatePort(@PathParam("id") String id, InputStream input) {
        log.trace(String.format(MESSAGE, "UPDATE " + id));

        final NeutronPort port = (NeutronPort)
                                 jsonToModelEntity(input, NeutronPort.class);

        adminService.updatePort(port);

        return status(Response.Status.OK).build();
    }

    /**
     * Removes the port with the given id.
     *
     * @param id port identifier
     * @return 204 NO_CONTENT, 400 BAD_REQUEST if the port does not exist
     */
    @DELETE
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deletePorts(@PathParam("id") String id) {
        log.trace(String.format(MESSAGE, "DELETE " + id));

        adminService.removePort(id);
        return noContent().build();
    }
}
