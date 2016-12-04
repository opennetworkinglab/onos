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
package org.onosproject.tetopology.management.api;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.Map;

/**
 * Default TeTopologies implementation.
 */
public class DefaultTeTopologies implements TeTopologies {
    private final String name;
    private final Map<TeTopologyKey, TeTopology> teTopologies;

    /**
     * Creates an instance of DefaultTeTopologies.
     *
     * @param name         the name of a TeTopology set
     * @param teTopologies the list of TeTopology
     */
    public DefaultTeTopologies(String name, Map<TeTopologyKey, TeTopology> teTopologies) {
        this.name = name;
        this.teTopologies = teTopologies != null ?
                new HashMap<>(teTopologies) : null;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Map<TeTopologyKey, TeTopology> teTopologies() {
        if (teTopologies == null) {
            return null;
        }
        return ImmutableMap.copyOf(teTopologies);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, teTopologies);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof DefaultTeTopologies) {
            DefaultTeTopologies that = (DefaultTeTopologies) object;
            return Objects.equal(name, that.name) &&
                    Objects.equal(teTopologies, that.teTopologies);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("teTopologies", teTopologies)
                .toString();
    }
}
