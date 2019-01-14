/*
 * Copyright 2018-present Open Networking Foundation
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
 *
 */

package org.onosproject.fpm.web;

import org.onlab.packet.IpAddress;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.rest.AbstractWebResource;
import org.onosproject.routing.fpm.FpmPeerInfo;
import org.onosproject.routing.fpm.FpmInfoService;
import org.onosproject.routing.fpm.FpmPeer;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.Comparator;
import java.util.Map;

/**
 * FPM REST API.
 */
@Path("")
public class FpmWebResource extends AbstractWebResource {

    /**
     * To get all fpm connections.
     * @return 200 OK with component properties of given component and variable.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("connections/")
    public Response getFpmConnections() {
        ObjectNode node = getFpmConnectionsJsonOutput();
        return Response.status(200).entity(node).build();
    }


    private ObjectNode getFpmConnectionsJsonOutput() {

        FpmInfoService fpmService = get(FpmInfoService.class);
        ObjectNode node = mapper().createObjectNode();
        ArrayNode connectionArray = mapper().createArrayNode();

        Map<FpmPeer, FpmPeerInfo> fpmPeers = fpmService.peers();

        fpmPeers.entrySet().stream()
                .sorted(Comparator.<Map.Entry<FpmPeer, FpmPeerInfo>, IpAddress>comparing(e -> e.getKey().address())
                                .thenComparing(e -> e.getKey().port()))
                .map(Map.Entry::getValue)
                .forEach(fpmPeerInfo -> connectionArray.add((new FpmCodec()).encode(fpmPeerInfo, this)));

        node.put("fpm-connections", connectionArray);
        return node;

    }
}


