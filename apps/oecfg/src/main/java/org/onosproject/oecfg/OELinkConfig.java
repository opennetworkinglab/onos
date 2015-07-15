/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.oecfg;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility program to convert standard ONOS config JSON to format expected
 * by the OE Link switch.
 */
public final class OELinkConfig {

    private ObjectMapper mapper = new ObjectMapper();
    private Map<String, String> dpidToName = new HashMap<>();

    public static void main(String[] args) {
        try {
            OELinkConfig config = new OELinkConfig();
            JsonNode json = config.convert(System.in);
            System.out.println(json.toString());
        } catch (IOException e) {
            System.err.println("Unable to convert JSON due to: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private OELinkConfig() {
    }

    private JsonNode convert(InputStream input) throws IOException {
        JsonNode json = mapper.readTree(input);
        ObjectNode result = mapper.createObjectNode();
        result.set("switchConfig", opticalSwitches(json));
        result.set("linkConfig", opticalLinks(json));
        return result;
    }

    private JsonNode opticalSwitches(JsonNode json) {
        ArrayNode result = mapper.createArrayNode();
        for (JsonNode node : json.get("devices")) {
            String dpid = dpid(node.path("uri"));
            String name = node.path("name").asText("none");
            dpidToName.put(dpid, name);
            if (node.path("type").asText("none").equals("ROADM")) {
                result.add(opticalSwitch(dpid, name, (ObjectNode) node));
            }
        }
        return result;
    }

    private ObjectNode opticalSwitch(String dpid, String name, ObjectNode node) {
        ObjectNode result = mapper.createObjectNode();
        ObjectNode annot = (ObjectNode) node.path("annotations");
        result.put("allowed", true).put("type", "Roadm")
                .put("name", name).put("nodeDpid", dpid)
                .put("latitude", annot.path("latitude").asDouble(0.0))
                .put("longitude", annot.path("longitude").asDouble(0.0))
                .set("params", switchParams(annot));
        return result;
    }

    private ObjectNode switchParams(ObjectNode annot) {
        return mapper.createObjectNode()
                .put("numRegen", annot.path("optical.regens").asInt(0));
    }

    private JsonNode opticalLinks(JsonNode json) {
        ArrayNode result = mapper.createArrayNode();
        for (JsonNode node : json.get("links")) {
            if (node.path("type").asText("none").equals("OPTICAL")) {
                result.add(opticalLink((ObjectNode) node));
            }
        }
        return result;
    }

    private ObjectNode opticalLink(ObjectNode node) {
        ObjectNode result = mapper.createObjectNode();
        ObjectNode annot = (ObjectNode) node.path("annotations");
        String src = dpid(node.path("src"));
        String dst = dpid(node.path("dst"));
        result.put("allowed", true).put("type", linkType(annot))
                .put("nodeDpid1", src).put("nodeDpid2", dst)
                .set("params", linkParams(src, dst, node, annot));
        return result;
    }

    private String linkType(ObjectNode annot) {
        return annot.path("optical.type").asText("cross-connect").equals("WDM") ?
                "wdmLink" : "pktOptLink";
    }

    private ObjectNode linkParams(String src, String dst,
                                  ObjectNode node, ObjectNode annot) {
        ObjectNode result = mapper.createObjectNode()
                .put("nodeName1", dpidToName.get(src))
                .put("nodeName2", dpidToName.get(dst))
                .put("port1", port(node.path("src")))
                .put("port2", port(node.path("dst")));
        if (annot.has("bandwidth")) {
            result.put("bandwidth", annot.path("bandwidth").asInt());
        }
        if (annot.has("optical.waves")) {
            result.put("numWaves", annot.path("optical.waves").asInt());
        }
        return result;
    }

    private String dpid(JsonNode node) {
        String s = node.asText("of:0000000000000000").substring(3);
        return s.substring(0, 2) + ":" + s.substring(2, 4) + ":" +
                s.substring(4, 6) + ":" + s.substring(6, 8) + ":" +
                s.substring(8, 10) + ":" + s.substring(10, 12) + ":" +
                s.substring(12, 14) + ":" + s.substring(14, 16);
    }

    private int port(JsonNode node) {
        return Integer.parseInt(node.asText("of:0000000000000000/0").substring(20));
    }

}
