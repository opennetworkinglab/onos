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

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Objects;

/**
 * Area identifier class (32 Bit Area-ID).
 */
public class AreaId {
    private final int areaId;

    /**
     * Constructor to set area identifier.
     *
     * @param areaId area id
     */
    public AreaId(int areaId) {
        this.areaId = areaId;
    }

    /**
     * obtain area identifier.
     *
     * @return area identifier
     */
    public int areaId() {
        return areaId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(areaId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof AreaId) {
            AreaId other = (AreaId) obj;
            return Objects.equals(areaId, other.areaId);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("areaId", areaId)
                .toString();
    }
}