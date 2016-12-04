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
package org.onosproject.castor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.IpAddress;
import org.onosproject.rest.AbstractWebResource;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * The Web Resource for REST API calls to the Castor application.
 */
@Path("castor")
public class CastorWebResource  extends AbstractWebResource {


    /**
     * Get the present ARP Mapping.
     * Use this to get the present ARP map stored by Castor
     *
     * @return 200 OK
     */
    @GET
    @Path("mac-map")
    public Response getMac() {
        String result = get(CastorStore.class).getAddressMap().toString();
        ObjectNode node = mapper().createObjectNode().put("response", result);
        return ok(node).build();
    }

    /**
     * Get list of added peers.
     * List of peers added.
     *
     * @return 200 OK
     */
    @GET
    @Path("get-peers")
    public Response getPeers() {
        String result = get(CastorStore.class).getCustomersMap().toString();
        ObjectNode node = mapper().createObjectNode().put("response", result);
        return ok(node).build();
    }

    /**
     * Add a Peer.
     * Use this to add a Customer or a BGP Peer
     *
     * @onos.rsModel PeerModel
     * @param incomingData json Data
     * @return 200 OK
     */
    @POST
    @Path("add-peer")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addPeer(String incomingData) {

        String arpResult = ", Mac was known";

        try {
            ObjectMapper mapper = new ObjectMapper();
            Peer peer = mapper.readValue(incomingData, Peer.class);
            get(ConnectivityManagerService.class).setUpConnectivity(peer);
            if ((get(CastorStore.class)).getAddressMap()
                    .get(IpAddress.valueOf(peer.getIpAddress())) != null) {
                get(ConnectivityManagerService.class).setUpL2(peer);
            } else {
                get(ArpService.class).createArp(peer);
                arpResult = ", ARP packet sent, MAC was not known";
            }
        } catch (Exception e) {
            e.printStackTrace();
            String result = "Unable to process due to some reason, Try again";
            ObjectNode node = mapper().createObjectNode().put("response", result);
            return ok(node).build();
        }

        String result = "Success: Peer Entered" + arpResult;
        ObjectNode node = mapper().createObjectNode().put("response", result);
        return ok(node).build();
    }

    /**
     * Delete a Peer.
     * Use this to delete a Peer. IpAddress should match as entered while adding.
     *
     * @onos.rsModel PeerModel
     * @param incomingData json Data
     * @return 200 OK
     */
    @POST
    @Path("delete-peer")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deletePeer(String incomingData) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            Peer peer = mapper.readValue(incomingData, Peer.class);
            get(ConnectivityManagerService.class).deletePeer(peer);
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(500).entity("Unable to delete the peer").build();
        }
        String result = "Peer Deleted";
        ObjectNode node = mapper().createObjectNode().put("response", result);
        return ok(node).build();
    }

    /**
     * Add router server.
     * Use this to add to add Route Servers for initializing
     *
     * @onos.rsModel PeerModel
     * @param incomingData json Data
     * @return 200 OK
     */
    @POST
    @Path("route-server")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addRouteServer(String incomingData) {

        try {
            ObjectMapper mapper = new ObjectMapper();
            Peer peer = mapper.readValue(incomingData, Peer.class);
            get(ConnectivityManagerService.class).start(peer);
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(500).entity("Unable to add the route server").build();
        }
        String result = "Server Entered";
        ObjectNode node = mapper().createObjectNode().put("response", result);
        return ok(node).build();
    }
}
