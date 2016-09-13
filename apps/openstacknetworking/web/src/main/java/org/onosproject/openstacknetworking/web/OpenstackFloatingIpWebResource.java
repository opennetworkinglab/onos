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
import org.onosproject.openstackinterface.OpenstackFloatingIP;
import org.onosproject.openstackinterface.web.OpenstackFloatingIpCodec;
import org.onosproject.openstacknetworking.OpenstackFloatingIpService;
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
 * Handles REST API call of Neutron L3 plugin.
 */
@Path("floatingips")
public class OpenstackFloatingIpWebResource extends AbstractWebResource {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final OpenstackFloatingIpCodec FLOATING_IP_CODEC = new OpenstackFloatingIpCodec();

    /**
     * Create FloatingIP.
     *
     * @param input JSON data describing FloatingIP
     * @return 200 OK
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createFloatingIp(InputStream input) {
        checkNotNull(input);

        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode floatingIpNode = (ObjectNode) mapper.readTree(input);

            OpenstackFloatingIP osFloatingIp = FLOATING_IP_CODEC.decode(floatingIpNode, this);
            OpenstackFloatingIpService floatingIpService =
                    getService(OpenstackFloatingIpService.class);
            floatingIpService.createFloatingIp(osFloatingIp);

            log.debug("REST API CREATE floatingip called");
            return Response.status(Response.Status.OK).build();
        } catch (Exception e) {
            log.error("createFloatingIp failed with {}", e.toString());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.toString())
                    .build();
        }
    }

    /**
     * Update FloatingIP.
     *
     * @param id    FloatingIP identifier
     * @param input JSON data describing FloatingIP
     * @return 200 OK
     */
    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateFloatingIp(@PathParam("id") String id, InputStream input) {
        checkNotNull(id);
        checkNotNull(input);

        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode floatingIpNode = (ObjectNode) mapper.readTree(input);

            OpenstackFloatingIP osFloatingIp = FLOATING_IP_CODEC.decode(floatingIpNode, this);
            OpenstackFloatingIpService floatingIpService =
                    getService(OpenstackFloatingIpService.class);
            floatingIpService.updateFloatingIp(osFloatingIp);

            log.debug("REST API UPDATE floatingip called {}", id);
            return Response.status(Response.Status.OK).build();
        } catch (Exception e) {
            log.error("updateFloatingIp failed with {}", e.toString());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.toString())
                    .build();
        }
    }

    /**
     * Delete FloatingIP.
     *
     * @param id FloatingIP identifier
     * @return 204 OK
     */
    @DELETE
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteFloatingIp(@PathParam("id") String id) {
        checkNotNull(id);

        OpenstackFloatingIpService floatingIpService =
                getService(OpenstackFloatingIpService.class);
        floatingIpService.deleteFloatingIp(id);

        log.debug("REST API DELETE floatingip is called {}", id);
        return Response.noContent().build();
    }

}
