/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.iptopology.api;

import java.util.Objects;

import com.google.common.base.MoreObjects;

/**
 * Represents Multi-Topology IDs for a network link, node or prefix.
 */
public class TopologyId {
    private final short topologyId;

    /**
     * Constructor to initialize its parameter.
     *
     * @param topologyId topology id for node/link/prefix
     */
    public TopologyId(short topologyId) {
        this.topologyId = topologyId;
    }

    /**
     * Obtains the topology ID.
     *
     * @return  topology ID
     */
    public short topologyId() {
        return topologyId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(topologyId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof TopologyId) {
            TopologyId other = (TopologyId) obj;
            return Objects.equals(topologyId, other.topologyId);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("topologyId", topologyId)
                .toString();
    }
}