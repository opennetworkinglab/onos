/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.segmentrouting.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.rest.AbstractWebResource;
import org.onosproject.segmentrouting.SegmentRoutingService;
import org.onosproject.segmentrouting.pwaas.DefaultL2TunnelDescription;
import org.onosproject.segmentrouting.pwaas.L2TunnelPolicy;
import org.onosproject.segmentrouting.pwaas.L2Tunnel;
import org.onosproject.segmentrouting.pwaas.L2TunnelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Query, create and remove pseudowires.
 */
@Path("pseudowire")
public class PseudowireWebResource extends AbstractWebResource {

    private static final PseudowireCodec PSEUDOWIRE_CODEC = new PseudowireCodec();

    private static Logger log = LoggerFactory
            .getLogger(PseudowireWebResource.class);

    /**
     * Get all pseudowires.
     * Returns an array of pseudowires.
     *
     * @return status of OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPseudowire() {
        SegmentRoutingService srService = get(SegmentRoutingService.class);

        log.debug("Fetching pseudowires form rest api!");

        List<L2TunnelPolicy> policies = srService.getL2Policies();
        List<L2Tunnel> tunnels = srService.getL2Tunnels();
        List<DefaultL2TunnelDescription> pseudowires = tunnels.stream()
                .map(l2Tunnel -> {
                    L2TunnelPolicy policy = null;
                    for (L2TunnelPolicy l2Policy : policies) {
                        if (l2Policy.tunnelId() == l2Tunnel.tunnelId()) {
                            policy = l2Policy;
                            break;
                        }
                    }

                    // return a copy
                    return new DefaultL2TunnelDescription(l2Tunnel, policy);
                })
                .collect(Collectors.toList());

        ObjectNode result = new ObjectMapper().createObjectNode();
        result.set("pseudowires", new PseudowireCodec().encode(pseudowires, this));

        return ok(result.toString()).build();
    }

    /**
     * Create a new pseudowire.
     *
     * @param input JSON stream for pseudowire to create
     * @return Response with appropriate status
     * @throws IOException Throws IO exception.
     * @onos.rsModel PseudowireCreate
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createPseudowire(InputStream input) throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode pseudowireJson = (ObjectNode) mapper.readTree(input);
        SegmentRoutingService srService = get(SegmentRoutingService.class);

        DefaultL2TunnelDescription pseudowire = PSEUDOWIRE_CODEC.decode(pseudowireJson, this);
        if (pseudowire == null) {
            return Response.serverError().status(Response.Status.BAD_REQUEST).build();
        }

        log.info("Creating pseudowire {} from rest api!", pseudowire.l2Tunnel().tunnelId());

        L2TunnelHandler.Result res = srService.addPseudowire(pseudowire);
        switch (res) {
            case ADDITION_ERROR:
                log.error("Pseudowire {} could not be added, error in configuration," +
                                  " please check logs for more details!",
                          pseudowire.l2Tunnel().tunnelId());
                return Response.serverError().status(Response.Status.INTERNAL_SERVER_ERROR).build();

            case SUCCESS:
                log.info("Pseudowire {} succesfully deployed!", pseudowire.l2Tunnel().tunnelId());
                return Response.ok().build();
            default:
                return Response.ok().build();
        }
    }

    /**
     * Delete a pseudowire.
     *
     * @param input JSON stream for pseudowire to delete
     * @return Response with appropriate status
     * @throws IOException Throws IO exception.
     * @onos.rsModel PseudowireDelete
     */
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    public Response removePseudowire(InputStream input) throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode pseudowireJson = (ObjectNode) mapper.readTree(input);
        SegmentRoutingService srService = get(SegmentRoutingService.class);

        Integer pseudowireId = PSEUDOWIRE_CODEC.decodeId(pseudowireJson);
        if (pseudowireId == null) {
            return Response.serverError().status(Response.Status.BAD_REQUEST).build();
        }

        log.info("Deleting pseudowire {} from rest api!", pseudowireId);

        L2TunnelHandler.Result res = srService.removePseudowire(pseudowireId);
        switch (res) {
            case REMOVAL_ERROR:
                log.error("Pseudowire {} could not be removed, error in configuration," +
                                  " please check logs for more details!",
                          pseudowireId);

                return Response.noContent().build();
            case SUCCESS:
                log.info("Pseudowire {} was removed succesfully!", pseudowireId);
                return Response.noContent().build();
            default:
                return Response.noContent().build();
        }
    }
}
