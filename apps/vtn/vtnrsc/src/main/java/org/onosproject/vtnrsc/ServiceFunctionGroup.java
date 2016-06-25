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
package org.onosproject.vtnrsc;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Map;
import java.util.Objects;

import com.google.common.collect.ImmutableMap;

/**
 * Implementation of ServiceFunctionGroup class.
 */
public final class ServiceFunctionGroup {

    private final String name;
    private final String description;
    private final Map<PortPairId, Integer> portPairLoadMap;

    /**
     * Creates an instance of service function group.
     *
     * @param name name of port pair
     * @param description description of port pair
     * @param portPairLoadMap map of port pair id and its load
     */
    public ServiceFunctionGroup(String name, String description,
                                Map<PortPairId, Integer> portPairLoadMap) {
        this.name = name;
        this.description = description;
        this.portPairLoadMap = portPairLoadMap;
    }

    /**
     * Returns name of service function group.
     *
     * @return name of service function group
     */
    public String name() {
        return name;
    }

    /**
     * Returns description of service function group.
     *
     * @return description of service function group.
     */
    public String description() {
        return description;
    }

    /**
     * Returns port pair load map.
     *
     * @return port pair load map
     */
    public Map<PortPairId, Integer> portPairLoadMap() {
        return ImmutableMap.copyOf(portPairLoadMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, portPairLoadMap);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ServiceFunctionGroup) {
            ServiceFunctionGroup that = (ServiceFunctionGroup) obj;
            return Objects.equals(name, that.name()) &&
                    Objects.equals(description, that.description()) &&
                    Objects.equals(portPairLoadMap, that.portPairLoadMap());
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("name", name)
                .add("description", description)
                .add("portPairLoadMap", portPairLoadMap)
                .toString();
    }
}
