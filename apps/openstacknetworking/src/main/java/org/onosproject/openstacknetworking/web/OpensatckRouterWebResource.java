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
package org.onosproject.openstacknetworking.web;

import com.fasterxml.jackson.databind.JsonNode;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onosproject.openstacknetworking.api.OpenstackRouterAdminService;
import org.onosproject.rest.AbstractWebResource;
import org.openstack4j.core.transport.ObjectMapperSingleton;
import org.openstack4j.openstack.networking.domain.NeutronRouter;
import org.openstack4j.openstack.networking.domain.NeutronRouterInterface;
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

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.status;

/**
 * Handles REST API call of Neturon L3 plugin.
 */

@Path("routers")
public class OpensatckRouterWebResource extends AbstractWebResource {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String MESSAGE_ROUTER = "Received router %s request";
    private static final String MESSAGE_ROUTER_IFACE = "Received router interface %s request";
    private static final String ROUTERS = "routers";

    private final OpenstackRouterAdminService adminService =
            DefaultServiceDirectory.getService(OpenstackRouterAdminService.class);

    @Context
    private UriInfo uriInfo;

    /**
     * Creates a router from the JSON input stream.
     *
     * @param input router JSON input stream
     * @return 201 CREATED if the JSON is correct, 400 BAD_REQUEST if the JSON
     * is invalid or duplicated router already exists
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createRouter(InputStream input) {
        log.trace(String.format(MESSAGE_ROUTER, "CREATE"));

        final NeutronRouter osRouter = readRouter(input);
        adminService.createRouter(osRouter);

        UriBuilder locationBuilder = uriInfo.getBaseUriBuilder()
                .path(ROUTERS)
                .path(osRouter.getId());

        return created(locationBuilder.build()).build();
    }

    /**
     * Updates the router with the specified identifier.
     *
     * @param id router identifier
     * @param input router JSON input stream
     * @return 200 OK with the updated router, 400 BAD_REQUEST if the requested
     * router does not exist
     */
    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateRouter(@PathParam("id") String id, InputStream input) {
        log.trace(String.format(MESSAGE_ROUTER, "UPDATE " + id));

        final NeutronRouter osRouter = readRouter(input);
        osRouter.setId(id);
        adminService.updateRouter(osRouter);

        return status(Response.Status.OK).build();
    }

    /**
     * Updates the router with the specified router interface.
     *
     * @param id router identifier
     * @param input router interface JSON input stream
     * @return 200 OK with the updated router interface, 400 BAD_REQUEST if the
     * requested router does not exist
     */
    @PUT
    @Path("{id}/add_router_interface")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addRouterInterface(@PathParam("id") String id, InputStream input) {
        log.trace(String.format(MESSAGE_ROUTER_IFACE, "UPDATE " + id));

        final NeutronRouterInterface osRouterIface = readRouterInterface(input);
        adminService.addRouterInterface(osRouterIface);

        return status(Response.Status.OK).build();
    }

    /**
     * Updates the router with the specified router interface.
     *
     * @param id router identifier
     * @param input router interface JSON input stream
     * @return 200 OK with the updated router interface, 400 BAD_REQUEST if the
     * requested router does not exist
     */
    @PUT
    @Path("{id}/remove_router_interface")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeRouterInterface(@PathParam("id") String id, InputStream input) {
        log.trace(String.format(MESSAGE_ROUTER_IFACE, "DELETE " + id));

        final NeutronRouterInterface osRouterIface = readRouterInterface(input);
        adminService.removeRouterInterface(osRouterIface.getPortId());

        return status(Response.Status.OK).build();
    }

    /**
     * Removes the router.
     *
     * @param id router identifier
     * @return 204 NO_CONTENT, 400 BAD_REQUEST if the router does not exist
     */
    @DELETE
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteRouter(@PathParam("id") String id) {
        log.trace(String.format(MESSAGE_ROUTER, "DELETE " + id));

        adminService.removeRouter(id);
        return noContent().build();
    }

    private NeutronRouter readRouter(InputStream input) {
        try {
            JsonNode jsonTree = mapper().enable(INDENT_OUTPUT).readTree(input);
            log.trace(mapper().writeValueAsString(jsonTree));
            return ObjectMapperSingleton.getContext(NeutronRouter.class)
                    .readerFor(NeutronRouter.class)
                    .readValue(jsonTree);
        } catch (Exception e) {
            throw new IllegalArgumentException();
        }
    }

    private NeutronRouterInterface readRouterInterface(InputStream input) {
        try {
            JsonNode jsonTree = mapper().enable(INDENT_OUTPUT).readTree(input);
            log.trace(mapper().writeValueAsString(jsonTree));
            return ObjectMapperSingleton.getContext(NeutronRouterInterface.class)
                    .readerFor(NeutronRouterInterface.class)
                    .readValue(jsonTree);
        } catch (Exception e) {
            throw new IllegalArgumentException();
        }
    }
}
