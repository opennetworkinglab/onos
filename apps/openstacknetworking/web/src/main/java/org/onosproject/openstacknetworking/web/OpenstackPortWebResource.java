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
import org.onosproject.openstacknetworking.OpenstackSecurityGroupService;
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

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Handles Rest API call from Neutron ML2 plugin.
 */
@Path("ports")
public class OpenstackPortWebResource extends AbstractWebResource {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final OpenstackPortCodec PORT_CODEC
            = new OpenstackPortCodec();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createPorts(InputStream input) {
        return Response.status(Response.Status.OK).build();
    }

    @Path("{portUUID}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deletePorts(@PathParam("portUUID") String id) {
        return Response.noContent().build();
    }

    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updatePorts(@PathParam("id") String id, InputStream input) {
        checkNotNull(input);
        checkNotNull(id);

        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode portNode = (ObjectNode) mapper.readTree(input);
            OpenstackPort osPort = PORT_CODEC.decode(portNode, this);

            OpenstackSecurityGroupService sgService
                    = getService(OpenstackSecurityGroupService.class);
            sgService.updateSecurityGroup(osPort);

            return Response.status(Response.Status.OK).build();
        } catch (IOException e) {
            log.error("UpdatePort post process failed due to {}", e.getMessage());

            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage())
                    .build();
        }
    }
}
