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

import org.apache.commons.io.IOUtils;
import org.onosproject.openstacknetworking.api.OpenstackHaService;
import org.onosproject.openstacknetworking.api.OpenstackNetworkAdminService;
import org.onosproject.rest.AbstractWebResource;
import org.openstack4j.openstack.networking.domain.NeutronNetwork;
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
import java.io.IOException;
import java.io.InputStream;

import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static javax.ws.rs.core.Response.status;
import static org.onosproject.openstacknetworking.api.Constants.DEFAULT_ACTIVE_IP_ADDRESS;
import static org.onosproject.openstacknetworking.api.Constants.REST_UTF8;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.jsonToModelEntity;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.syncDelete;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.syncPost;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.syncPut;

/**
 * Handles network REST API call of Neutron ML2 plugin.
 */
@Path("networks")
public class OpenstackNetworkWebResource extends AbstractWebResource {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private static final String MESSAGE = "Received networks %s request";
    private static final String NETWORKS = "networks";

    private final OpenstackNetworkAdminService adminService =
                                        get(OpenstackNetworkAdminService.class);
    private final OpenstackHaService haService = get(OpenstackHaService.class);

    @Context
    private UriInfo uriInfo;

    /**
     * Creates a network from the JSON input stream.
     *
     * @param input network JSON input stream
     * @return 201 CREATED if the JSON is correct, 400 BAD_REQUEST if the JSON
     * is invalid or duplicated network already exists
     * @throws IOException exception
     * @onos.rsModel NeutronNetwork
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createNetwork(InputStream input) throws IOException {
        log.trace(String.format(MESSAGE, "CREATE"));

        String inputStr = IOUtils.toString(input, REST_UTF8);

        if (!haService.isActive()
                && !DEFAULT_ACTIVE_IP_ADDRESS.equals(haService.getActiveIp())) {
            return syncPost(haService, NETWORKS, inputStr);
        }

        final NeutronNetwork net = (NeutronNetwork)
                jsonToModelEntity(inputStr, NeutronNetwork.class);

        adminService.createNetwork(net);

        UriBuilder locationBuilder = uriInfo.getBaseUriBuilder()
                .path(NETWORKS)
                .path(net.getId());

        return created(locationBuilder.build()).build();
    }

    /**
     * Updates the network with the specified identifier.
     *
     * @param id network identifier
     * @param input network JSON input stream
     * @return 200 OK with the updated network, 400 BAD_REQUEST if the requested
     * network does not exist
     * @throws IOException exception
     * @onos.rsModel NeutronNetwork
     */
    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateNetwork(@PathParam("id") String id, InputStream input) throws IOException {
        log.trace(String.format(MESSAGE, "UPDATE " + id));

        String inputStr = IOUtils.toString(input, REST_UTF8);

        if (!haService.isActive()
                && !DEFAULT_ACTIVE_IP_ADDRESS.equals(haService.getActiveIp())) {
            return syncPut(haService, NETWORKS, id, inputStr);
        }

        final NeutronNetwork net = (NeutronNetwork)
                jsonToModelEntity(inputStr, NeutronNetwork.class);

        adminService.updateNetwork(net);

        return status(Response.Status.OK).build();
    }

    /**
     * Removes the service network.
     *
     * @param id network identifier
     * @return 204 NO_CONTENT, 400 BAD_REQUEST if the network does not exist
     */
    @DELETE
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteNetwork(@PathParam("id") String id) {
        log.trace(String.format(MESSAGE, "DELETE " + id));

        if (!haService.isActive()
                && !DEFAULT_ACTIVE_IP_ADDRESS.equals(haService.getActiveIp())) {
            return syncDelete(haService, NETWORKS, id);
        }

        adminService.removeNetwork(id);
        return noContent().build();
    }
}
