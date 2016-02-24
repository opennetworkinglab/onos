/*
 * Copyright 2016 Open Networking Laboratory
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
import org.onosproject.codec.JsonCodec;
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
public class RegionCodec extends JsonCodec<Region> {

    // JSON field names
    private static final String REGION_ID = "id";
    private static final String NAME = "name";
    private static final String TYPE = "type";
    private static final String MASTERS = "masters";
    private static final String NODE_ID = "nodeId";
    private static final String REGION_NOT_NULL_MSG = "Region cannot be null";
    private static final String MISSING_MEMBER_MESSAGE = " member is required in Region";

    private static final BiMap<String, Region.Type> REGION_TYPE_MAP = HashBiMap.create();

    static {
        // key is String representation of Region.Type
        // value is Region.Type
        REGION_TYPE_MAP.put("CONTINENT", Region.Type.CONTINENT);
        REGION_TYPE_MAP.put("COUNTRY", Region.Type.COUNTRY);
        REGION_TYPE_MAP.put("METRO", Region.Type.METRO);
        REGION_TYPE_MAP.put("CAMPUS", Region.Type.CAMPUS);
        REGION_TYPE_MAP.put("BUILDING", Region.Type.BUILDING);
        REGION_TYPE_MAP.put("FLOOR", Region.Type.FLOOR);
        REGION_TYPE_MAP.put("ROOM", Region.Type.ROOM);
        REGION_TYPE_MAP.put("RACK", Region.Type.RACK);
        REGION_TYPE_MAP.put("LOGICAL_GROUP", Region.Type.LOGICAL_GROUP);
    }

    @Override
    public ObjectNode encode(Region region, CodecContext context) {
        checkNotNull(region, REGION_NOT_NULL_MSG);

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
        return result;
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

        if (mastersJson != null) {
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
        }

        // parse region id
        RegionId regionId = RegionId.regionId(nullIsIllegal(json.get(REGION_ID),
                REGION_ID + MISSING_MEMBER_MESSAGE).asText());

        // parse region name
        String name = nullIsIllegal(json.get(NAME), NAME +
                MISSING_MEMBER_MESSAGE).asText();

        // parse region type
        String typeText = nullIsIllegal(json.get(TYPE), TYPE +
                MISSING_MEMBER_MESSAGE).asText();

        Region.Type type = REGION_TYPE_MAP.get(typeText);

        return new DefaultRegion(regionId, name, type, masters);
    }

    /**
     * Decodes node id json to node id object.
     *
     * @param json json object
     * @return decoded node id object
     */
    private NodeId decodeNodeId(JsonNode json) {
        NodeId nodeId = NodeId.nodeId(nullIsIllegal(json, NODE_ID +
                MISSING_MEMBER_MESSAGE).asText());

        return nodeId;
    }
}
