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
package org.onosproject.codec.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Sets;
import org.onosproject.cluster.NodeId;
import org.onosproject.codec.CodecContext;
import org.onosproject.net.Annotations;
import org.onosproject.net.region.DefaultRegion;
import org.onosproject.net.region.Region;
import org.onosproject.net.region.RegionId;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * Codec for the Region class.
 */
public class RegionCodec extends AnnotatedCodec<Region> {

    // JSON field names
    private static final String REGION_ID = "id";
    private static final String NAME = "name";
    private static final String TYPE = "type";
    private static final String MASTERS = "masters";
    private static final String NODE_ID = "nodeId";

    private static final String NULL_REGION_MSG = "Region cannot be null";
    private static final String MISSING_MEMBER_MSG = " member is required in Region";

    private static final BiMap<String, Region.Type> REGION_TYPE_MAP = HashBiMap.create();

    static {
        // key is String representation of Region.Type; value is Region.Type
        for (Region.Type t : Region.Type.values()) {
            REGION_TYPE_MAP.put(t.name(), t);
        }
    }

    @Override
    public ObjectNode encode(Region region, CodecContext context) {
        checkNotNull(region, NULL_REGION_MSG);

        ObjectNode result = context.mapper().createObjectNode()
                .put(REGION_ID, region.id().toString())
                .put(NAME, region.name())
                .put(TYPE, region.type().toString());

        ArrayNode masters = context.mapper().createArrayNode();

        region.masters().forEach(sets -> {
            ArrayNode setsJson = context.mapper().createArrayNode();
            sets.forEach(nodeId -> setsJson.add(nodeId.toString()));
            masters.add(setsJson);
        });
        result.set(MASTERS, masters);
        return annotate(result, region, context);
    }

    @Override
    public Region decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        // parse masters
        List<Set<NodeId>> masters = new ArrayList<>();
        JsonNode mastersJson = json.get(MASTERS);
        checkNotNull(mastersJson);

        IntStream.range(0, mastersJson.size()).forEach(i -> {
            JsonNode setsJson = mastersJson.get(i);
            final Set<NodeId> nodeIds = Sets.newHashSet();
            if (setsJson != null && setsJson.isArray()) {
                Set<NodeId> localNodeIds = Sets.newHashSet();
                IntStream.range(0, setsJson.size()).forEach(j -> {
                    JsonNode nodeIdJson = setsJson.get(j);
                    localNodeIds.add(decodeNodeId(nodeIdJson));
                });
                nodeIds.addAll(localNodeIds);
            }
            masters.add(nodeIds);
        });

        RegionId regionId = RegionId.regionId(extractMember(REGION_ID, json));
        String name = extractMember(NAME, json);
        Region.Type type = REGION_TYPE_MAP.get(extractMember(TYPE, json));
        Annotations annots = extractAnnotations(json, context);

        return new DefaultRegion(regionId, name, type, annots, masters);
    }

    private String extractMember(String key, ObjectNode json) {
        return nullIsIllegal(json.get(key), key + MISSING_MEMBER_MSG).asText();
    }

    /**
     * Decodes node id json to node id object.
     *
     * @param json json object
     * @return decoded node id object
     */
    private NodeId decodeNodeId(JsonNode json) {
        return NodeId.nodeId(nullIsIllegal(json, NODE_ID +
                MISSING_MEMBER_MSG).asText());
    }
}
