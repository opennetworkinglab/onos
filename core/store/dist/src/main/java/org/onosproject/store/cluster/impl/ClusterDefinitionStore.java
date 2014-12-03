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
package org.onosproject.store.cluster.impl;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.cluster.DefaultControllerNode;
import org.onosproject.cluster.NodeId;
import org.onlab.packet.IpAddress;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

//Not used right now
/**
 * Allows for reading and writing cluster definition as a JSON file.
 */
public class ClusterDefinitionStore {

    private final File file;

    /**
     * Creates a reader/writer of the cluster definition file.
     *
     * @param filePath location of the definition file
     */
    public ClusterDefinitionStore(String filePath) {
        file = new File(filePath);
    }

    /*
     * Returns set of the controller nodes, including self.
     *
     * @return set of controller nodes
     */
    public Set<DefaultControllerNode> read() throws IOException {
        Set<DefaultControllerNode> nodes = new HashSet<>();
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode clusterNodeDef = (ObjectNode) mapper.readTree(file);
        Iterator<JsonNode> it = ((ArrayNode) clusterNodeDef.get("nodes")).elements();
        while (it.hasNext()) {
            ObjectNode nodeDef = (ObjectNode) it.next();
            nodes.add(new DefaultControllerNode(new NodeId(nodeDef.get("id").asText()),
                                                IpAddress.valueOf(nodeDef.get("ip").asText()),
                                                nodeDef.get("tcpPort").asInt(9876)));
        }
        return nodes;
    }

    /*
     * Writes the given set of the controller nodes.
     *
     * @param nodes set of controller nodes
     */
    public void write(Set<DefaultControllerNode> nodes) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode clusterNodeDef = mapper.createObjectNode();
        ArrayNode nodeDefs = mapper.createArrayNode();
        clusterNodeDef.set("nodes", nodeDefs);
        for (DefaultControllerNode node : nodes) {
            ObjectNode nodeDef = mapper.createObjectNode();
            nodeDef.put("id", node.id().toString())
                    .put("ip", node.ip().toString())
                    .put("tcpPort", node.tcpPort());
            nodeDefs.add(nodeDef);
        }
        mapper.writeTree(new JsonFactory().createGenerator(file, JsonEncoding.UTF8),
                         clusterNodeDef);
    }

}
