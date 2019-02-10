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
 */
package org.onosproject.k8snetworking.web;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.IpAddress;
import org.onosproject.k8snetworking.api.DefaultK8sIpam;
import org.onosproject.k8snetworking.api.K8sIpam;
import org.onosproject.k8snetworking.api.K8sIpamAdminService;
import org.onosproject.k8snetworking.api.K8sNetwork;
import org.onosproject.k8snetworking.api.K8sNetworkService;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.onlab.util.Tools.nullIsNotFound;

/**
 * Handles IPAM related REST API call from CNI plugin.
 */
@Path("ipam")
public class K8sIpamWebResource extends AbstractWebResource {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private static final String NETWORK_ID_NOT_FOUND = "Network Id is not found";
    private static final String IP_NOT_ALLOCATED = "IP address cannot be allocated";

    private static final String IPAM = "ipam";

    private final K8sNetworkService networkService = get(K8sNetworkService.class);
    private final K8sIpamAdminService ipamService = get(K8sIpamAdminService.class);

    /**
     * Requests for allocating a unique IP address of the given network ID.
     *
     * @param netId     network identifier
     * @return 200 OK with the serialized IPAM JSON string
     * @onos.rsModel K8sIpam
     */
    @GET
    @Path("{netId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response allocateIp(@PathParam("netId") String netId) {
        log.trace("Received IP allocation request of network " + netId);

        K8sNetwork network =
                nullIsNotFound(networkService.network(netId), NETWORK_ID_NOT_FOUND);

        IpAddress ip =
                nullIsNotFound(ipamService.allocateIp(network.networkId()), IP_NOT_ALLOCATED);

        ObjectNode root = mapper().createObjectNode();
        String ipamId = network.networkId() + "-" + ip.toString();
        K8sIpam ipam = new DefaultK8sIpam(ipamId, ip, network.networkId());
        root.set(IPAM, codec(K8sIpam.class).encode(ipam, this));

        return ok(root).build();
    }

    /**
     * Requests for releasing the given IP address.
     *
     * @param netId     network identifier
     * @param ip        IP address
     * @return 204 NO CONTENT
     */
    @DELETE
    @Path("{netId}/{ip}")
    public Response releaseIp(@PathParam("netId") String netId,
                              @PathParam("ip") String ip) {
        log.trace("Received IP release request of network " + netId);

        K8sNetwork network =
                nullIsNotFound(networkService.network(netId), NETWORK_ID_NOT_FOUND);

        ipamService.releaseIp(network.networkId(), IpAddress.valueOf(ip));

        return Response.noContent().build();
    }
}
