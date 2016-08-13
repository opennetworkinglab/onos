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
package org.onosproject.tetopology.management.api.node;

import static com.google.common.base.MoreObjects.toStringHelper;

import org.onosproject.tetopology.management.api.KeyId;

import com.google.common.base.Objects;

/**
 * Representation of a termination point key or reference.
 */
public class TerminationPointKey extends NetworkNodeKey {
    private final KeyId tpId;

    /**
     * Creates an instance of TerminationPointKey.
     *
     * @param networkId network identifier
     * @param nodeId node identifier
     * @param tpId termination point identifier
     */
    public TerminationPointKey(KeyId networkId, KeyId nodeId, KeyId tpId) {
        super(networkId, nodeId);
        this.tpId = tpId;
    }

    /**
     * Returns the termination point Id.
     *
     * @return termination point identifier
     */
    public KeyId tpId() {
        return tpId;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), tpId);
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof TerminationPointKey) {
            if (!super.equals(object)) {
                return false;
            }
            TerminationPointKey that = (TerminationPointKey) object;
            return Objects.equal(this.tpId, that.tpId);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("networkId", networkId())
                .add("nodeId", nodeId())
                .add("tpId", tpId)
                .toString();
    }

}
