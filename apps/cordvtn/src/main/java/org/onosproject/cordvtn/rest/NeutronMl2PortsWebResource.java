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
package org.onosproject.cordvtn.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.cordvtn.CordVtnService;
import org.onosproject.net.HostId;
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
import java.util.Map;


/**
 * Dummy Neutron ML2 mechanism driver.
 * It just returns OK for ports resource requests except for the port update.
 */
@Path("ports")
public class NeutronMl2PortsWebResource extends AbstractWebResource {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    private static final String PORTS_MESSAGE = "Received ports %s";

    private static final String PORT = "port";
    private static final String DEVICE_ID = "device_id";
    private static final String NAME = "name";
    private static final String MAC_ADDRESS = "mac_address";
    private static final String ADDRESS_PAIRS = "allowed_address_pairs";
    private static final String IP_ADDERSS = "ip_address";
    private static final String STAG_PREFIX = "stag-";
    private static final int STAG_BEGIN_INDEX = 5;

    private final CordVtnService service = get(CordVtnService.class);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createPorts(InputStream input) {
        log.trace(String.format(PORTS_MESSAGE, "create"));
        return Response.status(Response.Status.OK).build();
    }

    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updatePorts(@PathParam("id") String id, InputStream input) {
        log.debug(String.format(PORTS_MESSAGE, "update"));

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(input).get(PORT);
            log.trace("{}", jsonNode.toString());

            String deviceId = jsonNode.path(DEVICE_ID).asText();
            String name = jsonNode.path(NAME).asText();
            if (deviceId.isEmpty() || name.isEmpty() || !name.startsWith(STAG_PREFIX)) {
                // ignore all updates other than allowed address pairs
                return Response.status(Response.Status.OK).build();
            }

            // this is allowed address pairs updates
            MacAddress mac = MacAddress.valueOf(jsonNode.path(MAC_ADDRESS).asText());
            Map<IpAddress, MacAddress> vSgs = Maps.newHashMap();
            jsonNode.path(ADDRESS_PAIRS).forEach(addrPair -> {
                IpAddress pairIp = IpAddress.valueOf(addrPair.path(IP_ADDERSS).asText());
                MacAddress pairMac = MacAddress.valueOf(addrPair.path(MAC_ADDRESS).asText());
                vSgs.put(pairIp, pairMac);
            });

            service.updateVirtualSubscriberGateways(
                    HostId.hostId(mac),
                    name.substring(STAG_BEGIN_INDEX),
                    vSgs);
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        return Response.status(Response.Status.OK).build();
    }

    @Path("{id}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deletePorts(@PathParam("id") String id) {
        log.trace(String.format(PORTS_MESSAGE, "delete"));
        return Response.status(Response.Status.OK).build();
    }
}
