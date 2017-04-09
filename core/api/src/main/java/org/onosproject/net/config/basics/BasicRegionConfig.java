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

package org.onosproject.net.config.basics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import org.onosproject.net.DeviceId;
import org.onosproject.net.region.Region;
import org.onosproject.net.region.RegionId;
import org.onosproject.ui.topo.LayoutLocation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.onosproject.ui.topo.LayoutLocation.layoutLocation;

/**
 * Basic configuration for network regions.
 */
public final class BasicRegionConfig extends BasicElementConfig<RegionId> {

    private static final String TYPE = "type";
    private static final String DEVICES = "devices";
    private static final String LOC_IN_PEERS = "locInPeers";

    private static final String LOC_TYPE = "locType";
    private static final String LAT_OR_Y = "latOrY";
    private static final String LONG_OR_X = "LongOrX";


    @Override
    public boolean isValid() {
        return hasOnlyFields(ALLOWED, NAME, LATITUDE, LONGITUDE, UI_TYPE,
                             RACK_ADDRESS, OWNER, TYPE, DEVICES, LOC_IN_PEERS);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add(NAME, name())
                .add(TYPE, type())
                .add(UI_TYPE, uiType())
                .add(LATITUDE, latitude())
                .add(LONGITUDE, longitude())
                .add(DEVICES, devices())
                .toString();
    }

    /**
     * Returns the region type.
     *
     * @return the region type
     */
    public Region.Type type() {
        String t = get(TYPE, null);
        return t == null ? null : regionTypeFor(t);
    }

    private Region.Type regionTypeFor(String t) {
        try {
            return Region.Type.valueOf(t.toUpperCase());
        } catch (IllegalArgumentException ignored) {
        }
        return null;
    }

    /**
     * Sets the region type.
     *
     * @param type the region type, or null to unset
     * @return the config of the region
     */
    public BasicRegionConfig type(Region.Type type) {
        String t = type == null ? null : type.name().toLowerCase();
        return (BasicRegionConfig) setOrClear(TYPE, t);
    }

    /**
     * Returns the identities of the devices in this region.
     *
     * @return list of device identifiers
     */
    public List<DeviceId> devices() {
        return object.has(DEVICES) ? getList(DEVICES, DeviceId::deviceId) : null;
    }

    /**
     * Sets the devices of this region.
     *
     * @param devices the device identifiers, or null to unset
     * @return the config of the region
     */
    public BasicRegionConfig devices(Set<DeviceId> devices) {
        return (BasicRegionConfig) setOrClear(DEVICES, devices);
    }


    // Requires some custom json-node handling for maintaining a map
    // of peer location data...

    /**
     * Adds a peer location mapping to this region.
     *
     * @param peerId  the region ID of the peer
     * @param locType the type of location (geo/grid)
     * @param latOrY  geo latitude / grid y-coord
     * @param longOrX geo longitude / grid x-coord
     * @return self
     */
    public BasicRegionConfig addPeerLocMapping(String peerId, String locType,
                                               Double latOrY, Double longOrX) {
        ObjectNode map = getLocMap();
        map.set(peerId, makeLocation(locType, latOrY, longOrX));
        return this;
    }

    private JsonNode makeLocation(String locType, Double latOrY, Double longOrX) {
        return mapper.createObjectNode()
                .put(LOC_TYPE, locType)
                .put(LAT_OR_Y, latOrY)
                .put(LONG_OR_X, longOrX);
    }

    private ObjectNode getLocMap() {
        ObjectNode locMap = (ObjectNode) object.get(LOC_IN_PEERS);
        if (locMap == null) {
            locMap = mapper.createObjectNode();
            object.set(LOC_IN_PEERS, locMap);
        }
        return locMap;
    }

    /**
     * Returns the list of layout location mappings for where peer region nodes
     * should be placed on the layout when viewing this region.
     *
     * @return list of peer node locations
     */
    public List<LayoutLocation> getMappings() {
        List<LayoutLocation> mappings = new ArrayList<>();
        ObjectNode map = (ObjectNode) object.get(LOC_IN_PEERS);
        if (map != null) {
            for (Iterator<Map.Entry<String, JsonNode>> it = map.fields(); it.hasNext();) {
                Map.Entry<String, JsonNode> entry = it.next();
                String peerId = entry.getKey();
                ObjectNode data = (ObjectNode) entry.getValue();

                String lt = data.get(LOC_TYPE).asText();
                double latY = data.get(LAT_OR_Y).asDouble();
                double longX = data.get(LONG_OR_X).asDouble();

                mappings.add(layoutLocation(peerId, lt, latY, longX));
            }
        }
        return mappings;
    }
}
