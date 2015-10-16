/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.openstackswitching.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.openstackswitching.OpenstackPort;
import org.onosproject.openstackswitching.OpenstackSwitchingService;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;

@Path("ports")
public class OpenstackPortWebResource extends AbstractWebResource {

    protected static final Logger log = LoggerFactory
            .getLogger(OpenstackPortWebResource.class);

    private static final OpenstackPortCodec PORT_CODEC = new OpenstackPortCodec();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createPorts(InputStream input) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode portNode = (ObjectNode) mapper.readTree(input);

            OpenstackPort openstackPort = PORT_CODEC.decode(portNode, this);

            OpenstackSwitchingService switchingService = get(OpenstackSwitchingService.class);
            switchingService.createPorts(openstackPort);
            log.info("REST API ports is called with {}", portNode.toString());
            return Response.status(Response.Status.OK).build();
        } catch (Exception e) {
            log.error("Creates VirtualPort failed because of exception {}",
                    e.toString());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.toString())
                    .build();
        }
    }

    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deletesPorts(InputStream input) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode portNode = (ObjectNode) mapper.readTree(input);

            OpenstackSwitchingService switchingService = get(OpenstackSwitchingService.class);
            switchingService.deletePorts();
            log.info("REST API ports is called with {}", portNode.toString());
            return Response.status(Response.Status.OK).build();
        } catch (Exception e) {
            log.error("Delete VirtualPort failed because of exception {}",
                    e.toString());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.toString())
                    .build();
        }
    }

    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updatePorts(InputStream input) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode portNode = (ObjectNode) mapper.readTree(input);

            OpenstackSwitchingService switchingService = get(OpenstackSwitchingService.class);
            switchingService.updatePorts();
            log.info("REST API ports is called with {}", portNode.toString());
            return Response.status(Response.Status.OK).build();
        } catch (Exception e) {
            log.error("Update VirtualPort failed because of exception {}",
                    e.toString());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.toString())
                    .build();
        }
    }
}
