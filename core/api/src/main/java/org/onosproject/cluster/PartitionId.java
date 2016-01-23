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
package org.onosproject.cluster;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Objects;

/**
 * {@link Partition} identifier.
 */
public class PartitionId implements Comparable<PartitionId> {

    private final int id;

    /**
     * Creates a partition identifier from an integer.
     *
     * @param id input integer
     */
    public PartitionId(int id) {
        checkArgument(id >= 0, "partition id must be non-negative");
        this.id = id;
    }

    /**
     * Creates a partition identifier from an integer.
     *
     * @param id input integer
     * @return partition identification
     */
    public static PartitionId from(int id) {
        return new PartitionId(id);
    }

    /**
     * Returns the partition identifier as an integer.
     * @return number
     */
    public int asInt() {
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
        if (obj instanceof PartitionId) {
            final PartitionId other = (PartitionId) obj;
            return Objects.equals(this.id, other.id);
        }
        return false;
    }

    @Override
    public String toString() {
        return String.valueOf(id);
    }

    @Override
    public int compareTo(PartitionId that) {
        return Integer.compare(this.id, that.id);
    }
}