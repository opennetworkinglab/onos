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
import com.google.common.collect.Sets;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.onosproject.cluster.NodeId;
import org.onosproject.net.region.Region;

import java.util.Set;

/**
 * Hamcrest matcher for region.
 */
public final class RegionJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

    private final Region region;

    private RegionJsonMatcher(Region region) {
        this.region = region;
    }

    @Override
    protected boolean matchesSafely(JsonNode jsonRegion, Description description) {
        // check id
        String jsonRegionId = jsonRegion.get("id").asText();
        String regionId = region.id().toString();
        if (!jsonRegionId.equals(regionId)) {
            description.appendText("region id was " + jsonRegionId);
            return false;
        }

        // check type
        String jsonType = jsonRegion.get("type").asText();
        String type = region.type().toString();
        if (!jsonType.equals(type)) {
            description.appendText("type was " + jsonType);
            return false;
        }

        // check name
        String jsonName = jsonRegion.get("name").asText();
        String name = region.name();
        if (!jsonName.equals(name)) {
            description.appendText("name was " + jsonName);
            return false;
        }

        // check size of master array
        JsonNode jsonMasters = jsonRegion.get("masters");
        if (jsonMasters.size() != region.masters().size()) {
            description.appendText("masters size was " + jsonMasters.size());
            return false;
        }

        // check master
        for (Set<NodeId> set : region.masters()) {
            boolean masterFound = false;
            for (int masterIndex = 0; masterIndex < jsonMasters.size(); masterIndex++) {
                masterFound = checkEquality(jsonMasters.get(masterIndex), set);
            }

            if (!masterFound) {
                description.appendText("master not found " + set.toString());
                return false;
            }
        }

        return true;
    }

    private Set<NodeId> jsonToSet(JsonNode nodes) {
        final Set<NodeId> nodeIds = Sets.newHashSet();
        nodes.forEach(node -> nodeIds.add(NodeId.nodeId(node.asText())));
        return nodeIds;
    }

    private boolean checkEquality(JsonNode nodes, Set<NodeId> nodeIds) {
        Set<NodeId> jsonSet = jsonToSet(nodes);
        if (jsonSet.size() == nodes.size()) {
            return jsonSet.containsAll(nodeIds);
        }
        return false;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(region.toString());
    }

    /**
     * Factory to allocate a region matcher.
     *
     * @param region region object we are looking for
     * @return matcher
     */
    public static RegionJsonMatcher matchesRegion(Region region) {
        return new RegionJsonMatcher(region);
    }
}
