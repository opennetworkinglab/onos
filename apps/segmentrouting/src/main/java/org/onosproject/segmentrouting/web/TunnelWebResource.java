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

@Path("tunnel")
public class TunnelWebResource extends AbstractWebResource {

    private static final TunnelCodec TUNNEL_CODEC = new TunnelCodec();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTunnel() {
        SegmentRoutingService srService = get(SegmentRoutingService.class);
        List<Tunnel> tunnels = srService.getTunnels();
        ObjectNode result = new ObjectMapper().createObjectNode();
        result.set("tunnel", new TunnelCodec().encode(tunnels, this));

        return ok(result.toString()).build();
    }

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

    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    public Response removeTunnel(InputStream input) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode tunnelJson = (ObjectNode) mapper.readTree(input);
        SegmentRoutingService srService = get(SegmentRoutingService.class);
        Tunnel tunnelInfo = TUNNEL_CODEC.decode(tunnelJson, this);
        srService.removeTunnel(tunnelInfo);

        return Response.ok().build();
    }

}
