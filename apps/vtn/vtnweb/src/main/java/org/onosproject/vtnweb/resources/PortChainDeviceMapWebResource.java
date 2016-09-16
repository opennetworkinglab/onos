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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsNotFound;

import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.onosproject.codec.CodecContext;
import org.onosproject.rest.AbstractWebResource;
import org.onosproject.vtnrsc.LoadBalanceId;
import org.onosproject.vtnrsc.PortChain;
import org.onosproject.vtnrsc.PortChainId;
import org.onosproject.vtnrsc.portchain.PortChainService;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Query and program port chain.
 */

@Path("portChainDeviceMap")
public class PortChainDeviceMapWebResource extends AbstractWebResource {

    public static final String PORT_CHAIN_NOT_FOUND = "Port chain not found";
    public static final String PORT_CHAIN_ID_EXIST = "Port chain exists";
    public static final String PORT_CHAIN_ID_NOT_EXIST = "Port chain does not exist with identifier";

    private static final String NAME = "name";
    private static final String ID = "id";
    private static final String CLASSIFIERS = "classifiers";
    private static final String FORWARDERS = "forwarders";
    private static final String LOADBALANCEID = "loadBalanceId";

    /**
     * Get details of a specified port chain id.
     *
     * @param id port chain id
     * @return 200 OK, 404 if given identifier does not exist
     */
    @GET
    @Path("{chain_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getPortChainDeviceMap(@PathParam("chain_id") String id) {

        PortChain portChain = nullIsNotFound(get(PortChainService.class).getPortChain(PortChainId.of(id)),
                                             PORT_CHAIN_NOT_FOUND);
        ObjectNode result = mapper().createObjectNode();
        result.set("portChainDeviceMap", encode(portChain, this));

        return ok(result.toString()).build();
    }

    private ObjectNode encode(PortChain portChain, CodecContext context) {
        checkNotNull(portChain, "portChain cannot be null");
        ObjectNode result = context.mapper().createObjectNode();
        result.put(ID, portChain.portChainId().toString())
        .put(NAME, portChain.name());

        Set<LoadBalanceId> loadBalanceIds = portChain.getLoadBalancePathMapKeys();
        for (LoadBalanceId id : loadBalanceIds) {
            result.put(LOADBALANCEID, id.toString())
            .put(CLASSIFIERS, portChain.getSfcClassifiers(id).toString())
            .put(FORWARDERS, portChain.getSfcForwarders(id).toString());
        }
        return result;
    }
}
