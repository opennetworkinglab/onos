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
import org.onosproject.routing.fpm.FpmPeerAcceptRoutes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import static org.onlab.util.Tools.nullIsIllegal;
import static org.onlab.util.Tools.readTreeFromStream;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.PathParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import java.io.InputStream;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Comparator;
import java.util.Map;
import java.util.List;

/**
 * FPM REST API.
 */
@Path("")
public class FpmWebResource extends AbstractWebResource {

    private static final String ACCEPT_ROUTES = "acceptRoutes";
    private static final String PEER_ADDRESS = "peerAddress";
    private static final String PEER_PORT = "peerPort";
    protected static final String PEERS = "peers";
    protected static final String PEERS_KEY_ERROR = "Peers key must be present";
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * To get all fpm connections.
     * @return 200 OK with component properties of given component and variable.
     * @onos.rsModel FpmConnectionsGet
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("connections/")
    public Response getFpmConnections() {
        ObjectNode node = getFpmConnectionsJsonOutput();
        return Response.status(200).entity(node).build();
    }

    /**
     * Performs disabling of FPM Peer.
     *
     * @param stream array of peer address and accept route flag
     * @return 200 OK disable peer.
     * @onos.rsModel FpmPeerSetAcceptRouteFlag
     */
    @POST
    @Path("acceptRoutes")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateAcceptRouteFlagForConnection(InputStream stream) {
        FpmInfoService fpmService = get(FpmInfoService.class);
        try {
            ObjectNode jsonTree = readTreeFromStream(mapper(), stream);
            ArrayNode peersArray = nullIsIllegal((ArrayNode) jsonTree.get(PEERS),
                    PEERS_KEY_ERROR);
            List<FpmPeerAcceptRoutes> fpmPeerRouteInfo = (new FpmAcceptRoutesCodec()).decode(peersArray, this);
            fpmService.updateAcceptRouteFlag(fpmPeerRouteInfo);
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }

        return Response.ok().build();

    }

    /**
     * Gets peers acceptRoute Flag details.
     * @param peerAddress peer identifier
     * @return 200 OK with a collection of peerInfo
     * @onos.rsModel FpmPeerGetAcceptRoutes
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("acceptRoutes/{peerAddress}")
    public Response getPeerAcceptRouteInfo(@PathParam("peerAddress") String peerAddress) {
        ObjectNode node = getFpmPeerAcceptFlagInfoJsonOutput(peerAddress);
        return Response.status(200).entity(node).build();
    }

    /**
     * Gets all peers acceptRoute Flag details.
     * @return 200 OK with a collection of peerInfo
     * @onos.rsModel FpmGetAcceptRoutes
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("acceptRoutes/")
    public Response getAllPeerAcceptRouteInfo() {
        ObjectNode node = getFpmPeerRouteInfoJsonOutput();
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

    private ObjectNode getFpmPeerRouteInfoJsonOutput() {

        FpmInfoService fpmService = get(FpmInfoService.class);
        ObjectNode node = mapper().createObjectNode();
        ArrayNode connectionArray = mapper().createArrayNode();
        Map<FpmPeer, FpmPeerInfo> fpmPeers = fpmService.peers();
        fpmPeers.entrySet().stream()
                .sorted(Comparator.<Map.Entry<FpmPeer, FpmPeerInfo>, IpAddress>comparing(e -> e.getKey().address())
                        .thenComparing(e -> e.getKey().port()))
                .map(Map.Entry::getValue)
                .forEach(fpmPeerInfo -> {
                    fpmPeerInfo.connections().forEach(connection -> {
                        ObjectNode fpmNode = mapper().createObjectNode();
                        fpmNode.put(PEER_ADDRESS, connection.peer().address().toString());
                        fpmNode.put(PEER_PORT, connection.peer().port());
                        fpmNode.put(ACCEPT_ROUTES, connection.isAcceptRoutes());
                        connectionArray.add(fpmNode);
                    });

                });

        node.put("fpm-peer-info", connectionArray);
        return node;


    }

    private ObjectNode getFpmPeerAcceptFlagInfoJsonOutput(String address) {

        FpmInfoService fpmService = get(FpmInfoService.class);
        ObjectNode fpmNode = mapper().createObjectNode();
        Map<FpmPeer, FpmPeerInfo> fpmPeers = fpmService.peers();
        IpAddress peerAddress = IpAddress.valueOf(address);
        fpmPeers.entrySet().stream()
                .filter(peer -> peer.getKey().address().equals(peerAddress))
                .map(Map.Entry::getValue)
                .forEach(fpmPeerInfo -> {
                    fpmPeerInfo.connections().forEach(connection -> {
                        fpmNode.put(ACCEPT_ROUTES, connection.isAcceptRoutes());
                    });
                });


        return fpmNode;


    }
}


