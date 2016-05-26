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
package org.onosproject.vtnweb.resources;

import static org.onlab.util.Tools.nullIsNotFound;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.onosproject.rest.AbstractWebResource;
import org.onosproject.vtnrsc.PortChainId;
import org.onosproject.vtnrsc.ServiceFunctionGroup;
import org.onosproject.vtnrsc.portchainsfmap.PortChainSfMapService;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Query service function and load details by port chain.
 */

@Path("portChainSfMap")
public class PortChainSfMapWebResource extends AbstractWebResource {

    public static final String PORT_CHAIN_NOT_FOUND = "Port chain not found";
    public static final String PORT_CHAIN_ID_EXIST = "Port chain exists";
    public static final String PORT_CHAIN_ID_NOT_EXIST = "Port chain does not exist with identifier";

    /**
     * Get service function details of a specified port chain id.
     *
     * @param id port chain id
     * @return 200 OK, 404 if given identifier does not exist
     */
    @GET
    @Path("{chainId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getPortChainSfMap(@PathParam("chainId") String id) {

        Iterable<ServiceFunctionGroup> serviceFunctionGroups = nullIsNotFound(get(PortChainSfMapService.class)
                .getServiceFunctions(PortChainId.of(id)),
                                                                              PORT_CHAIN_NOT_FOUND);
        ObjectNode result = mapper().createObjectNode();
        ArrayNode portChainSfMap = result.putArray("portChainSfMap");
        if (serviceFunctionGroups != null) {
            for (final ServiceFunctionGroup serviceFunctionGroup : serviceFunctionGroups) {
                portChainSfMap.add(codec(ServiceFunctionGroup.class).encode(serviceFunctionGroup, this));
            }
        }
        return ok(result.toString()).build();
    }
}
