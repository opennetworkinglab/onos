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
import org.openstack4j.openstack.networking.domain.NeutronSubnet;
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
 * Handles subnet REST API call from Neutron ML2 plugin.
 */
@Path("subnets")
public class OpenstackSubnetWebResource extends AbstractWebResource {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private static final String MESSAGE = "Received subnets %s request";
    private static final String SUBNETS = "subnets";

    private final OpenstackNetworkAdminService adminService =
                                        get(OpenstackNetworkAdminService.class);
    private final OpenstackHaService haService = get(OpenstackHaService.class);

    @Context
    private UriInfo uriInfo;

    /**
     * Creates a subnet from the JSON input stream.
     *
     * @param input subnet JSON input stream
     * @return 201 CREATED if the JSON is correct, 400 BAD_REQUEST if the JSON
     * is invalid or duplicated subnet already exists
     * @throws IOException exception
     * @onos.rsModel NeutronSubnet
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createSubnet(InputStream input) throws IOException {
        log.trace(String.format(MESSAGE, "CREATE"));

        String inputStr = IOUtils.toString(input, REST_UTF8);

        if (!haService.isActive()
                && !DEFAULT_ACTIVE_IP_ADDRESS.equals(haService.getActiveIp())) {
            return syncPost(haService, SUBNETS, inputStr);
        }

        final NeutronSubnet subnet = (NeutronSubnet)
                jsonToModelEntity(inputStr, NeutronSubnet.class);

        adminService.createSubnet(subnet);
        UriBuilder locationBuilder = uriInfo.getBaseUriBuilder()
                .path(SUBNETS)
                .path(subnet.getId());

        // TODO fix networking-onos to send Network UPDATE when subnet created
        return created(locationBuilder.build()).build();
    }

    /**
     * Updates the subnet with the specified identifier.
     *
     * @param id    subnet identifier
     * @param input subnet JSON input stream
     * @return 200 OK with the updated subnet, 400 BAD_REQUEST if the requested
     * subnet does not exist
     * @throws IOException exception
     * @onos.rsModel NeutronSubnet
     */
    @PUT
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateSubnet(@PathParam("id") String id, InputStream input) throws IOException {
        log.trace(String.format(MESSAGE, "UPDATE " + id));

        String inputStr = IOUtils.toString(input, REST_UTF8);

        if (!haService.isActive()
                && !DEFAULT_ACTIVE_IP_ADDRESS.equals(haService.getActiveIp())) {
            return syncPut(haService, SUBNETS, id, inputStr);
        }

        final NeutronSubnet subnet = (NeutronSubnet)
                            jsonToModelEntity(inputStr, NeutronSubnet.class);

        adminService.updateSubnet(subnet);

        return status(Response.Status.OK).build();
    }

    /**
     * Removes the subnet.
     *
     * @param id subnet identifier
     * @return 204 NO_CONTENT, 400 BAD_REQUEST if the subnet does not exist
     */
    @DELETE
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteSubnet(@PathParam("id") String id) {
        log.trace(String.format(MESSAGE, "DELETE " + id));

        if (!haService.isActive()
                && !DEFAULT_ACTIVE_IP_ADDRESS.equals(haService.getActiveIp())) {
            return syncDelete(haService, SUBNETS, id);
        }

        adminService.removeSubnet(id);
        return noContent().build();
    }
}
