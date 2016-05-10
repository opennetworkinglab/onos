/*
 * Copyright 2015-present Open Networking Laboratory
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
import org.onosproject.segmentrouting.Tunnel;

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

/**
 * Query, create and remove segment routing tunnels.
 */
@Path("tunnel")
public class TunnelWebResource extends AbstractWebResource {

    private static final TunnelCodec TUNNEL_CODEC = new TunnelCodec();

    /**
     * Get all segment routing tunnels.
     * Returns an array of segment routing tunnels.
     *
     * @return status of OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTunnel() {
        SegmentRoutingService srService = get(SegmentRoutingService.class);
        List<Tunnel> tunnels = srService.getTunnels();
        ObjectNode result = new ObjectMapper().createObjectNode();
        result.set("tunnel", new TunnelCodec().encode(tunnels, this));

        return ok(result.toString()).build();
    }

    /**
     * Create a new segment routing tunnel.
     *
     * @param input JSON stream for tunnel to create
     * @return status of the request - OK if the tunnel is created,
     * @throws IOException if the JSON is invalid
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createTunnel(InputStream input) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode tunnelJson = (ObjectNode) mapper.readTree(input);
        SegmentRoutingService srService = get(SegmentRoutingService.class);
        Tunnel tunnelInfo = TUNNEL_CODEC.decode(tunnelJson, this);
        srService.createTunnel(tunnelInfo);

        return Response.ok().build();
    }

    /**
     * Delete a segment routing tunnel.
     *
     * @param input JSON stream for tunnel to delete
     * @return 204 NO CONTENT, if the tunnel is removed
     * @throws IOException if JSON is invalid
     */
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    public Response removeTunnel(InputStream input) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode tunnelJson = (ObjectNode) mapper.readTree(input);
        SegmentRoutingService srService = get(SegmentRoutingService.class);
        Tunnel tunnelInfo = TUNNEL_CODEC.decode(tunnelJson, this);
        srService.removeTunnel(tunnelInfo);

        return Response.noContent().build();
    }

}
