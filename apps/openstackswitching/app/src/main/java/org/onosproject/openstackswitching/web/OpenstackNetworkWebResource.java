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
package org.onosproject.openstackswitching.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.openstackswitching.OpenstackNetwork;
import org.onosproject.openstackswitching.OpenstackSwitchingService;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;

@Path("networks")
public class OpenstackNetworkWebResource extends AbstractWebResource {

    protected static final Logger log = LoggerFactory
            .getLogger(OpenstackNetworkWebResource.class);

    private static final OpenstackNetworkCodec NETWORK_CODEC = new OpenstackNetworkCodec();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createNetwork(InputStream input) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode networkNode = (ObjectNode) mapper.readTree(input);

            OpenstackNetwork openstackNetwork = NETWORK_CODEC.decode(networkNode, this);

            OpenstackSwitchingService switchingService = get(OpenstackSwitchingService.class);
            switchingService.createNetwork(openstackNetwork);
            return Response.status(Response.Status.OK).build();
        } catch (Exception e) {
            log.error("Creates VirtualPort failed because of exception {}",
                    e.toString());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.toString())
                    .build();
        }
    }
}
