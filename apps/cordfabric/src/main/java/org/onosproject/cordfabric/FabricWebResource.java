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

package org.onosproject.cordfabric;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.VlanId;
import org.onosproject.rest.AbstractWebResource;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Web resource for interacting with the fabric.
 */
@Path("vlans")
public class FabricWebResource extends AbstractWebResource {

    private static final FabricVlanCodec VLAN_CODEC = new FabricVlanCodec();

    /**
     * Get all CORD fabric VLANs.
     *
     * @return array of cord VLANs in the system.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVlans() {
        FabricService fabricService = get(FabricService.class);
        List<FabricVlan> vlans = fabricService.getVlans();
        ObjectNode result = new ObjectMapper().createObjectNode();
        result.set("vlans", new FabricVlanCodec().encode(vlans, this));

        return ok(result.toString()).build();
    }

    /**
     * Create a CORD fabric VLAN.
     *
     * @param input JSON stream describing new VLAN
     * @return status of the request - CREATED if the JSON is correct,
     * INTERNAL_SERVER_ERROR if the JSON is invalid
     * @throws IOException if the JSON is invalid
     */
    @POST
    @Path("add")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addVlan(InputStream input) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode vlanJson = (ObjectNode) mapper.readTree(input);
        FabricService fabricService = get(FabricService.class);

        fabricService.addVlan(VLAN_CODEC.decode(vlanJson, this));

        return Response.ok().build();
    }

    /**
     * Delete a CORD fabric VLAN.
     *
     * @param vlan identifier of the VLAN to remove
     * @return status of the request - OK
     */
    @DELETE
    @Path("{vlan}")
    public Response deleteVlan(@PathParam("vlan") String vlan) {
        VlanId vlanId = VlanId.vlanId(Short.parseShort(vlan));

        FabricService fabricService = get(FabricService.class);

        fabricService.removeVlan(vlanId);

        return Response.ok().build();
    }
}
