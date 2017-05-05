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
package org.onosproject.ui.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.rest.BaseResource;
import org.slf4j.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Topology viewer resource.
 */
@Path("topology")
public class TopologyResource extends BaseResource {

    private static final String ID = "id";
    private static final String URI = "uri";

    // length of a MAC defined as a string ... "xx:xx:xx:xx:xx:xx"
    private static final int MAC_LEN = 17;
    private static final char SLASH_CHAR = '/';

    private static final Logger log = getLogger(TopologyResource.class);

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Returns the location data associated with devices and hosts, that is
     * currently cached in the Meta-UI store.
     *
     * @return cached location data for devices and hosts
     */
    @Path("geoloc")
    @GET
    @Produces("application/json")
    public Response getGeoLocations() {
        ObjectNode rootNode = mapper.createObjectNode();
        ArrayNode devices = mapper.createArrayNode();
        ArrayNode hosts = mapper.createArrayNode();

        Map<String, ObjectNode> metaUi = TopologyViewMessageHandler.getMetaUi();
        for (String id : metaUi.keySet()) {
            ObjectNode memento = metaUi.get(id);
            if (isHostId(id)) {
                addGeoData(hosts, ID, id, memento);
            } else {
                addGeoData(devices, URI, id, memento);
            }
        }

        rootNode.set("devices", devices);
        rootNode.set("hosts", hosts);
        return Response.ok(rootNode.toString()).build();
    }

    private boolean isHostId(String id) {
        return id.length() > MAC_LEN && id.charAt(MAC_LEN) == SLASH_CHAR;
    }

    private void addGeoData(ArrayNode array, String idField, String id,
                            ObjectNode memento) {
        ObjectNode node = mapper.createObjectNode().put(idField, id);
        ObjectNode annot = mapper.createObjectNode();
        node.set("annotations", annot);

        // TODO: add handling of gridY/gridX if locType is "grid" (not "geo")

        try {
            annot.put("latitude", memento.get("latOrY").asDouble())
                 .put("longitude", memento.get("longOrX").asDouble());
            array.add(node);
        } catch (Exception e) {
            log.debug("Skipping geo entry");
        }
    }

    /**
     * Stores sprite data for retrieval by the UI Topology View.
     *
     * @param stream input data stream (typically from an uploaded file).
     * @return REST response
     * @throws IOException if there is an issue reading from the stream
     * @deprecated since Junco (1.9), in favor of client-side defined sprite layers
     */
    @Path("sprites")
    @POST
    @Consumes("application/json")
    @Deprecated
    public Response setSprites(InputStream stream) throws IOException {
        JsonNode root = mapper.readTree(stream);
        String name = root.path("defn_name").asText("sprites");
        get(SpriteService.class).put(name, root);
        return Response.ok().build();
    }
}
