/*
 * Copyright 2015-present Open Networking Laboratory
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

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Objects;

/**
 * Represents the Pseudonode information of device in ISIS domain.
 */
public class IsIsPseudonode implements RouteIdentifier {
    private final IsoNodeId isoNodeId;
    private final byte psnIdentifier;
    private final ProtocolType type;

    /**
     * Constructor to initialize the values.
     *
     * @param isoNodeId ISO system-ID
     * @param psnIdentifier Pseudonode identifier
     * @param type Protocol ID
     */
    public IsIsPseudonode(IsoNodeId isoNodeId, byte psnIdentifier, ProtocolType type) {
        this.isoNodeId = isoNodeId;
        this.psnIdentifier = psnIdentifier;
        this.type = type;
    }

    /**
     * Obtains iso system id of Pseudonode of device in ISIS domain.
     *
     * @return ISO system Id
     */
    public IsoNodeId isoNodeId() {
        return isoNodeId;
    }

    /**
     * Obtains Pseudonode identifier.
     *
     * @return Pseudonode identifier
     */
    public byte psnIdentifier() {
        return psnIdentifier;
    }

    @Override
    public ProtocolType type() {
        return type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(isoNodeId, psnIdentifier, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof IsIsPseudonode) {
            IsIsPseudonode other = (IsIsPseudonode) obj;
            return Objects.equals(isoNodeId, other.isoNodeId) && Objects.equals(psnIdentifier, other.psnIdentifier)
                    && Objects.equals(type, other.type);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("isoNodeId", isoNodeId)
                .add("psnIdentifier", psnIdentifier)
                .add("type", type)
                .toString();
    }
}