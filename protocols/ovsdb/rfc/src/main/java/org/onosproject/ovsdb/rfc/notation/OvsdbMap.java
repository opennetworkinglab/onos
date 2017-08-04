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
package org.onosproject.ovsdb.rfc.notation;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;
import java.util.Objects;

import org.onosproject.ovsdb.rfc.notation.json.OvsdbMapSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * OvsdbMap is a 2-element JSON array that represents a database map value.
 */
@JsonSerialize(using = OvsdbMapSerializer.class)
public final class OvsdbMap {

    private final Map map;

    /**
     * OvsdbMap constructor.
     * @param map java.util.Map
     */
    private OvsdbMap(Map map) {
        checkNotNull(map, "map cannot be null");
        this.map = map;
    }

    /**
     * Returns map.
     * @return map
     */
    public Map map() {
        return map;
    }

    /**
     * convert Map into OvsdbMap.
     * @param map java.util.Map
     * @return OvsdbMap
     */
    public static OvsdbMap ovsdbMap(Map map) {
        return new OvsdbMap(map);
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof OvsdbMap) {
            final OvsdbMap other = (OvsdbMap) obj;
            return Objects.equals(this.map, other.map);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("map", map).toString();
    }
}
