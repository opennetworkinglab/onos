/*
 * Copyright 2020-present Open Networking Foundation
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
package org.onosproject.k8snode.codec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.IpAddress;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.k8snode.api.DefaultHostNodesInfo;
import org.onosproject.k8snode.api.HostNodesInfo;

import java.util.HashSet;
import java.util.Set;

import static org.onlab.util.Tools.nullIsIllegal;

/**
 * HostNodesInfo codec used for serializing and de-serializing JSON.
 */
public final class HostNodesInfoCodec extends JsonCodec<HostNodesInfo> {

    private static final String HOST_IP = "hostIp";
    private static final String NODES = "nodes";

    private static final String MISSING_MESSAGE = " is required in HostNodesInfo";

    @Override
    public ObjectNode encode(HostNodesInfo entity, CodecContext context) {
        ObjectNode node = context.mapper().createObjectNode()
                .put(HOST_IP, entity.hostIp().toString());

        ArrayNode nodes = context.mapper().createArrayNode();
        entity.nodes().forEach(nodes::add);
        node.set(NODES, nodes);
        return node;
    }

    @Override
    public HostNodesInfo decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        IpAddress hostIp = IpAddress.valueOf(nullIsIllegal(
                json.get(HOST_IP).asText(), HOST_IP + MISSING_MESSAGE));

        Set<String> nodes = new HashSet<>();
        ArrayNode nodesJson = (ArrayNode) json.get(NODES);

        for (JsonNode cidrJson : nodesJson) {
            nodes.add(cidrJson.asText());
        }

        return new DefaultHostNodesInfo.Builder()
                .hostIp(hostIp)
                .nodes(nodes)
                .build();
    }
}
