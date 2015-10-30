/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.net.topology;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Representation of the topology cluster identity.
 */
public final class ClusterId {

    private final int id;

    // Public construction is prohibit
    private ClusterId(int id) {
        this.id = id;
    }

    /**
     * Returns the cluster identifier, represented by the specified integer
     * serial number.
     *
     * @param id integer serial number
     * @return cluster identifier
     */
    public static ClusterId clusterId(int id) {
        return new ClusterId(id);
    }

    /**
     * Returns the backing integer index.
     *
     * @return backing integer index
     */
    public int index() {
        return id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ClusterId) {
            final ClusterId other = (ClusterId) obj;
            return Objects.equals(this.id, other.id);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("id", id).toString();
    }

}
