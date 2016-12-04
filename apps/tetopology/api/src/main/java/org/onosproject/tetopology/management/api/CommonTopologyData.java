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
import org.onosproject.net.DeviceId;

import java.util.BitSet;

/**
 * Representation of topology common attributes.
 */
public class CommonTopologyData {
    private final OptimizationType optimization;
    private final BitSet flags;
    private final KeyId networkId;
    private final DeviceId ownerId;

    /**
     * Create an instance of CommonTopologyData.
     *
     * @param networkId    the network identifier
     * @param optimization the TE topology optimization criteria
     * @param flags        the topology characteristics flags
     * @param ownerId      the controller identifier owning this topology
     */
    public CommonTopologyData(KeyId networkId, OptimizationType optimization,
                              BitSet flags, DeviceId ownerId) {
        this.optimization = optimization;
        this.flags = flags;
        this.networkId = networkId;
        this.ownerId = ownerId;
    }

    /**
     * Creates an instance of CommonTopologyData from a given TE topology.
     *
     * @param teTopology the given TE Topology
     */
    public CommonTopologyData(TeTopology teTopology) {
        optimization = teTopology.optimization();
        flags = teTopology.flags();
        networkId = teTopology.networkId();
        ownerId = teTopology.ownerId();
    }


    /**
     * Returns the topology optimization type.
     *
     * @return the optimization type
     */
    public OptimizationType optimization() {
        return optimization;
    }

    /**
     * Returns the network identifier.
     *
     * @return the network id
     */
    public KeyId networkId() {
        return networkId;
    }

    /**
     * Returns the topology characteristics flags.
     *
     * @return the flags
     */
    public BitSet flags() {
        return flags;
    }

    /**
     * Returns the SDN controller identifier owning this topology.
     *
     * @return the SDN controller id
     */
    public DeviceId ownerId() {
        return ownerId;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(optimization, flags, ownerId, networkId);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof CommonTopologyData) {
            CommonTopologyData that = (CommonTopologyData) object;
            return Objects.equal(optimization, that.optimization) &&
                    Objects.equal(flags, that.flags) &&
                    Objects.equal(networkId, that.networkId) &&
                    Objects.equal(ownerId, that.ownerId);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("optimization", optimization)
                .add("flags", flags)
                .add("ownerId", ownerId)
                .add("networkId", networkId)
                .toString();
    }
}
