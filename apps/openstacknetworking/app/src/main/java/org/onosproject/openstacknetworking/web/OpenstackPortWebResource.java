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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.IOUtils;
import org.onosproject.openstacknetworking.api.OpenstackHaService;
import org.onosproject.openstacknetworking.api.OpenstackNetworkAdminService;
import org.onosproject.openstacknode.api.DpdkConfig;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeService;
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
 * Handles port REST API call from Neutron ML2 plugin.
 */
@Path("ports")
public class OpenstackPortWebResource extends AbstractWebResource {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private static final String MESSAGE = "Received ports %s request";
    private static final String PORTS = "ports";
    private static final String VIF_TYPE = "vif_type";
    private static final String VHOSTUSER = "vhostuser";
    private static final String SOCKET_DIR = "socket_dir";

    private final OpenstackNetworkAdminService
                        adminService = get(OpenstackNetworkAdminService.class);
    private final OpenstackNodeService nodeService = get(OpenstackNodeService.class);
    private final OpenstackHaService haService = get(OpenstackHaService.class);

    @Context
    private UriInfo uriInfo;

    /**
     * Creates a port from the JSON input stream.
     *
     * @param input port JSON input stream
     * @return 201 CREATED if the JSON is correct, 400 BAD_REQUEST if the JSON
     * is invalid or duplicated port already exists
     * @throws IOException exception
     * @onos.rsModel NeutronPort
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createPorts(InputStream input) throws IOException {
        log.trace(String.format(MESSAGE, "CREATE"));

        String inputStr = IOUtils.toString(input, REST_UTF8);

        if (!haService.isActive()
                && !DEFAULT_ACTIVE_IP_ADDRESS.equals(haService.getActiveIp())) {
            return syncPost(haService, PORTS, inputStr);
        }

        final NeutronPort port = (NeutronPort)
                jsonToModelEntity(inputStr, NeutronPort.class);

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
     * @throws IOException exception
     * @onos.rsModel NeutronPort
     */
    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updatePort(@PathParam("id") String id, InputStream input) throws IOException {
        log.trace(String.format(MESSAGE, "UPDATE " + id));

        String inputStr = IOUtils.toString(input, REST_UTF8);

        if (!haService.isActive()
                && !DEFAULT_ACTIVE_IP_ADDRESS.equals(haService.getActiveIp())) {
            return syncPut(haService, PORTS, id, inputStr);
        }

        final NeutronPort port = (NeutronPort)
                jsonToModelEntity(inputStr, NeutronPort.class);

        adminService.updatePort(port);
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jsonNode = mapper.createObjectNode();

        OpenstackNode node = nodeService.node(port.getHostId());
        if (node == null) {
            return status(Response.Status.OK).build();
        } else if (node.datapathType().equals(DpdkConfig.DatapathType.NETDEV)) {
            log.debug("UpdatePort for port {} called in netdev device {} " +
                            "so sends vif type as a payload of the response",
                    port.getId(), node.hostname());
            jsonNode.put(VIF_TYPE, VHOSTUSER);

            if (node.socketDir() != null) {
                jsonNode.put(SOCKET_DIR, node.socketDir());
            }
            return status(Response.Status.OK).entity(jsonNode.toString()).build();
        } else {
            return status(Response.Status.OK).build();
        }
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

        if (!haService.isActive()
                && !DEFAULT_ACTIVE_IP_ADDRESS.equals(haService.getActiveIp())) {
            return syncDelete(haService, PORTS, id);
        }

        adminService.removePort(id);
        return noContent().build();
    }
}
