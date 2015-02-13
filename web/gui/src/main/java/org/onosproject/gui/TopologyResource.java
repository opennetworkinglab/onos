/*
 * Copyright 2014,2015 Open Networking Laboratory
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
package org.onosproject.gui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.rest.BaseResource;
import org.slf4j.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Topology viewer resource.
 */
@Path("topology")
public class TopologyResource extends BaseResource {

    private static final Logger log = getLogger(TopologyResource.class);

    private final ObjectMapper mapper = new ObjectMapper();


    @Path("/geoloc")
    @GET
    @Produces("application/json")
    public Response getGeoLocations() {
        ObjectNode rootNode = mapper.createObjectNode();
        ArrayNode devices = mapper.createArrayNode();
        ArrayNode hosts = mapper.createArrayNode();

        Map<String, ObjectNode> metaUi = TopologyViewMessages.getMetaUi();
        for (String id : metaUi.keySet()) {
            ObjectNode memento = metaUi.get(id);
            if (id.charAt(17) == '/') {
                addGeoData(hosts, "id", id, memento);
            } else {
                addGeoData(devices, "uri", id, memento);
            }
        }

        rootNode.set("devices", devices);
        rootNode.set("hosts", hosts);
        return Response.ok(rootNode.toString()).build();
    }

    private void addGeoData(ArrayNode array, String idField, String id,
                            ObjectNode memento) {
        ObjectNode node = mapper.createObjectNode().put(idField, id);
        ObjectNode annot = mapper.createObjectNode();
        node.set("annotations", annot);
        try {
            annot.put("latitude", memento.get("lat").asDouble())
                    .put("longitude", memento.get("lng").asDouble());
            array.add(node);
        } catch (Exception e) {
            log.debug("Skipping geo entry");
        }
    }

}
