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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.openstackinterface.OpenstackPort;
import org.onosproject.openstackinterface.web.OpenstackPortCodec;
import org.onosproject.openstacknetworking.OpenstackPortInfo;
import org.onosproject.openstacknetworking.OpenstackRoutingService;
import org.onosproject.openstacknetworking.OpenstackSwitchingService;
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
import java.io.InputStream;

/**
 * Handles Rest API call from Neutron ML2 plugin.
 */
@Path("ports")
public class OpenstackPortWebResource extends AbstractWebResource {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private static final String PORTNAME_PREFIX_VM = "tap";
    private static final OpenstackPortCodec PORT_CODEC = new OpenstackPortCodec();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createPorts(InputStream input) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode portNode = (ObjectNode) mapper.readTree(input);

            OpenstackPort openstackPort = PORT_CODEC.decode(portNode, this);
            OpenstackSwitchingService switchingService =
                    getService(OpenstackSwitchingService.class);
            switchingService.createPorts(openstackPort);

            log.debug("REST API ports is called with {}", portNode.toString());
            return Response.status(Response.Status.OK).build();

        } catch (Exception e) {
            log.error("Creates Port failed because of exception {}",
                    e.toString());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.toString())
                    .build();
        }
    }

    @Path("{portUUID}")
    @DELETE
    public Response deletePorts(@PathParam("portUUID") String id) {
        OpenstackSwitchingService switchingService =
                getService(OpenstackSwitchingService.class);
        OpenstackPortInfo portInfo = switchingService.openstackPortInfo()
                .get(PORTNAME_PREFIX_VM.concat(id.substring(0, 11)));
        OpenstackRoutingService routingService =
                getService(OpenstackRoutingService.class);
        routingService.checkDisassociatedFloatingIp(id, portInfo);

        switchingService.removePort(id);

        return Response.noContent().build();
    }

    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updatePorts(InputStream input) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode portNode = (ObjectNode) mapper.readTree(input);

            OpenstackPort openstackPort = PORT_CODEC.decode(portNode, this);
            OpenstackSwitchingService switchingService =
                    getService(OpenstackSwitchingService.class);
            switchingService.updatePort(openstackPort);

            log.debug("REST API update port is called with {}", portNode.toString());
            return Response.status(Response.Status.OK).build();

        } catch (Exception e) {
            log.error("Update Port failed because of exception {}",
                    e.toString());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.toString())
                    .build();
        }
    }
}
