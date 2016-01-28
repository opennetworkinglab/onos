/*
 * Copyright 2016 Open Networking Laboratory
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
import org.onosproject.openstackrouting.OpenstackRouter;
import org.onosproject.openstackrouting.OpenstackRouterInterface;
import org.onosproject.openstackswitching.OpenstackSwitchingService;
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
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Handles REST API call of Neturon L3 plugin.
 */

@Path("routers")
public class OpensatckRouterWebResource extends AbstractWebResource {
    protected static final Logger log = LoggerFactory
            .getLogger(OpenstackNetworkWebResource.class);

    private static final OpenstackRouterInterfaceCodec ROUTER_INTERFACE_CODEC
            = new OpenstackRouterInterfaceCodec();
    private static final OpenstackRouterCodec ROUTER_CODEC
            = new OpenstackRouterCodec();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createRouter(InputStream input) {
        checkNotNull(input);
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode routerNode = (ObjectNode) mapper.readTree(input);

            OpenstackRouter openstackRouter
                    = ROUTER_CODEC.decode(routerNode, this);

            OpenstackSwitchingService switchingService
                    = getService(OpenstackSwitchingService.class);
            switchingService.createRouter(openstackRouter);

            log.debug("REST API CREATE router is called {}", input.toString());
            return Response.status(Response.Status.OK).build();
        } catch (Exception e) {
            log.error("Create Router failed because of exception {}",
                    e.toString());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.toString())
                    .build();
        }
    }

    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateRouter(@PathParam("id") String id) {
        checkNotNull(id);
        try {
            OpenstackSwitchingService switchingService
                    = getService(OpenstackSwitchingService.class);
            switchingService.updateRouter(id);

            log.debug("REST API UPDATE router is called from router {}", id);
            return Response.status(Response.Status.OK).build();
        } catch (Exception e) {
            log.error("Updates Router failed because of exception {}",
                    e.toString());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.toString())
                    .build();
        }
    }

    @PUT
    @Path("{id}/add_router_interface")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addRouterInterface(InputStream input) {
        checkNotNull(input);
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode routerIfNode = (ObjectNode) mapper.readTree(input);

            OpenstackRouterInterface openstackRouterInterface
                    = ROUTER_INTERFACE_CODEC.decode(routerIfNode, this);

            OpenstackSwitchingService switchingService
                    = getService(OpenstackSwitchingService.class);
            switchingService.updateRouterInterface(openstackRouterInterface);

            log.debug("REST API AddRouterInterface is called from router {} portId: {}, subnetId: {}, tenantId: {}",
                    openstackRouterInterface.id(), openstackRouterInterface.portId(),
                    openstackRouterInterface.subnetId(), openstackRouterInterface.tenantId());

            return Response.status(Response.Status.OK).build();
        } catch (Exception e) {
            log.error("AddRouterInterface failed because of exception {}",
                    e.toString());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.toString())
                    .build();
        }
    }

    @DELETE
    @Path("{id}")
    public Response deleteRouter(@PathParam("id") String id) {
        checkNotNull(id);
        OpenstackSwitchingService switchingService =
                getService(OpenstackSwitchingService.class);
        switchingService.deleteRouter(id);

        log.debug("REST API DELETE routers is called {}", id);
        return Response.status(Response.Status.OK).build();
    }

    @PUT
    @Path("{id}/remove_router_interface")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeRouterInterface(@PathParam("id") String id, InputStream input) {
        checkNotNull(id);
        checkNotNull(input);
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode routerIfNode = (ObjectNode) mapper.readTree(input);

            OpenstackRouterInterface openstackRouterInterface
                    = ROUTER_INTERFACE_CODEC.decode(routerIfNode, this);

            OpenstackSwitchingService switchingService
                    = getService(OpenstackSwitchingService.class);
            switchingService.removeRouterInterface(openstackRouterInterface);

            log.debug("REST API RemoveRouterInterface is called from router {} portId: {}, subnetId: {}," +
                    "tenantId: {}", openstackRouterInterface.id(), openstackRouterInterface.portId(),
                    openstackRouterInterface.subnetId(), openstackRouterInterface.tenantId());

            return Response.status(Response.Status.OK).build();
        } catch (Exception e) {
            log.error("RemoveRouterInterface failed because of exception {}",
                    e.toString());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.toString())
                    .build();
        }
    }
}
