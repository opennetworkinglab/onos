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
import org.openstack4j.openstack.networking.domain.NeutronFloatingIP;
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
 * Handles REST API call of Neutron L3 plugin.
 */
@Path("floatingips")
public class OpenstackFloatingIpWebResource extends AbstractWebResource {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String MESSAGE = "Received floating IP %s request";
    private static final String FLOATING_IPS = "floatingips";

    private final OpenstackRouterAdminService adminService =
            DefaultServiceDirectory.getService(OpenstackRouterAdminService.class);

    @Context
    private UriInfo uriInfo;

    /**
     * Creates a floating IP from the JSON input stream.
     *
     * @param input floating ip JSON input stream
     * @return 201 CREATED if the JSON is correct, 400 BAD_REQUEST if the JSON
     * is invalid or duplicated floating ip already exists
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createFloatingIp(InputStream input) {
        log.trace(String.format(MESSAGE, "CREATE"));

        final NeutronFloatingIP floatingIp = readFloatingIp(input);
        adminService.createFloatingIp(floatingIp);

        UriBuilder locationBuilder = uriInfo.getBaseUriBuilder()
                .path(FLOATING_IPS)
                .path(floatingIp.getId());

        return created(locationBuilder.build()).build();
    }

    /**
     * Updates the floating IP with the specified identifier.
     *
     * @param id    floating ip identifier
     * @param input floating ip JSON input stream
     * @return 200 OK with the updated floating ip, 400 BAD_REQUEST if the requested
     * floating ip does not exist
     */
    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateFloatingIp(@PathParam("id") String id, InputStream input) {
        log.trace(String.format(MESSAGE, "UPDATE " + id));

        final NeutronFloatingIP floatingIp = readFloatingIp(input);
        adminService.updateFloatingIp(floatingIp);

        return status(Response.Status.OK).build();
    }

    /**
     * Removes the floating IP.
     *
     * @param id floating ip identifier
     * @return 204 NO_CONTENT, 400 BAD_REQUEST if the floating ip does not exist
     */
    @DELETE
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteFloatingIp(@PathParam("id") String id) {
        log.trace(String.format(MESSAGE, "DELETE " + id));

        adminService.removeFloatingIp(id);
        return noContent().build();
    }

    private NeutronFloatingIP readFloatingIp(InputStream input) {
        try {
            JsonNode jsonTree = mapper().enable(INDENT_OUTPUT).readTree(input);
            log.trace(mapper().writeValueAsString(jsonTree));
            return ObjectMapperSingleton.getContext(NeutronFloatingIP.class)
                    .readerFor(NeutronFloatingIP.class)
                    .readValue(jsonTree);
        } catch (Exception e) {
            throw new IllegalArgumentException();
        }
    }
}
