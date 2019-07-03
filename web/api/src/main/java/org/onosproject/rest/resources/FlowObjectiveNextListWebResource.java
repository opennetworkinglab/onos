/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.rest.resources;


import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.tuple.Pair;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import static org.slf4j.LoggerFactory.getLogger;


/**
 * Get Flow objective next list.
 */
@Path("nextobjectives")
public class FlowObjectiveNextListWebResource extends AbstractWebResource {

    private final Logger log = getLogger(getClass());

    /**
     * To get all obj-next-Ids.
     * @return 200 OK with flow objective Next ids.
     * @onos.rsModel NextObjectives
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getObjNextids() {
        ObjectNode node = getGroupChainByIdJsonOutput(null, null);
        return Response.status(200).entity(node).build();
    }

    /**
     * Returns all group-chains associated with the given nextId.
     *
     * @param nextId nextid mapping
     * @return 200 OK with array of all the group chain.
     *
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("nextobjective/{nextId}")
    public Response getGroupChainByNextid(@PathParam("nextId") String nextId) {
        ObjectNode node = getGroupChainByIdJsonOutput(Integer.parseInt(nextId), null);
        return Response.status(200).entity(node).build();
    }


    /**
     * Returns all group-chains associated with the given deviceId.
     *
     * @param deviceId deviceId mapping
     * @return 200 OK with array of all the group chain.
     *
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{deviceId}")
    public Response getGroupChainByDeviceId(@PathParam("deviceId") String deviceId) {
        ObjectNode node = getGroupChainByIdJsonOutput(null, DeviceId.deviceId(deviceId));
        return Response.status(200).entity(node).build();
    }

    private ObjectNode getGroupChainByIdJsonOutput(Integer nextId, DeviceId deviceId) {
        ObjectNode root = mapper().createObjectNode();
        ArrayNode connectionArray = mapper().createArrayNode();
        FlowObjectiveService service = get(FlowObjectiveService.class);
        Map<Pair<Integer, DeviceId>, List<String>> nextObjGroupMap = service.getNextMappingsChain();

        if (nextId == null && deviceId == null) {
            nextObjGroupMap.forEach((key, value) -> {
                ObjectNode mappingNode = mapper().createObjectNode();
                String keyString = String.format("NextId %s: %s", key.getLeft(), key.getRight());
                mappingNode.put(keyString, value.toString());
                connectionArray.add(mappingNode);
            });
        } else {
            nextObjGroupMap.forEach((key, value) -> {
                ObjectNode mappingNode = mapper().createObjectNode();
                if ((key.getLeft().equals(nextId)) || (key.getRight().equals(deviceId))) {
                    List groupchain = value;
                    if (deviceId != null && groupchain != null) {
                        String keyString = String.format("NextId %s:", key.getLeft());
                        mappingNode.put(keyString, groupchain.toString());
                    } else if (groupchain != null) {
                        mappingNode.put("groupChain", groupchain.toString());
                    }
                    connectionArray.add(mappingNode);
                }
            });
        }
        root.put("obj-next-ids", connectionArray);

        return root;
    }


}
