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
package org.onosproject.virtualbng;

import static org.slf4j.LoggerFactory.getLogger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;

/**
 * REST services to interact with the virtual BNG.
 */
@Path("privateip")
public class VbngResource extends AbstractWebResource {

    private final Logger log = getLogger(getClass());

    /**
     * Create a new virtual BNG connection.
     *
     * @param privateIp IP Address for the BNG private network
     * @param mac MAC address for the host
     * @param hostName name of the host
     * @return public IP address for the new connection
     */
    @POST
    @Path("{privateip}/{mac}/{hostname}")
    public String privateIpAddNotification(@PathParam("privateip")
            String privateIp, @PathParam("mac") String mac,
            @PathParam("hostname") String hostName) {

        log.info("Received creating vBNG request, "
                + "privateIp= {}, mac={}, hostName= {}",
                 privateIp, mac, hostName);

        if (privateIp == null || mac == null || hostName == null) {
            log.info("Parameters can not be null");
            return "0";
        }

        IpAddress privateIpAddress = IpAddress.valueOf(privateIp);
        MacAddress hostMacAddress = MacAddress.valueOf(mac);

        VbngService vbngService = get(VbngService.class);

        IpAddress publicIpAddress = null;
        // Create a virtual BNG
        publicIpAddress = vbngService.createVbng(privateIpAddress,
                                                 hostMacAddress,
                                                 hostName);

        if (publicIpAddress != null) {
            return publicIpAddress.toString();
        } else {
            return "0";
        }
    }

    /**
     * Delete a virtual BNG connection.
     *
     * @param privateIp IP Address for the BNG private network
     * @return 200 OK
     */
    @DELETE
    @Path("{privateip}")
    public Response privateIpDeleteNotification(@PathParam("privateip")
            String privateIp) {
        String result;
        if (privateIp == null) {
            log.info("Private IP address to delete is null");
            result = "0";
        }
        log.info("Received a private IP address : {} to delete", privateIp);
        IpAddress privateIpAddress = IpAddress.valueOf(privateIp);

        VbngService vbngService = get(VbngService.class);

        IpAddress assignedPublicIpAddress = null;
        // Delete a virtual BNG
        assignedPublicIpAddress = vbngService.deleteVbng(privateIpAddress);

        if (assignedPublicIpAddress != null) {
            result = assignedPublicIpAddress.toString();
        } else {
            result = "0";
        }
        return Response.ok().entity(result).build();
    }

    /**
     * Query virtual BNG map.
     *
     * @return IP Address map
     */
    @GET
    @Path("map")
    @Produces(MediaType.APPLICATION_JSON)
    public Response privateIpDeleteNotification() {

        log.info("Received vBNG IP address map request");

        VbngConfigurationService vbngConfigurationService =
                get(VbngConfigurationService.class);

        Map<IpAddress, IpAddress> map =
                vbngConfigurationService.getIpAddressMappings();
        ObjectNode result = new ObjectMapper().createObjectNode();

        result.set("map", new IpAddressMapEntryCodec().encode(map.entrySet(), this));

        return ok(result.toString()).build();
    }
}
