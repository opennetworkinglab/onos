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
package org.onosproject.cluster;

import org.onlab.util.Identifier;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * {@link Partition} identifier.
 */
public class PartitionId extends Identifier<Integer> implements Comparable<PartitionId> {

    /**
     * Creates a partition identifier from an integer.
     *
     * @param id input integer
     */
    public PartitionId(int id) {
        super(id);
        checkArgument(id >= 0, "partition id must be non-negative");
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
        return id();
    }

    @Override
    public int compareTo(PartitionId that) {
        return Integer.compare(this.identifier, that.identifier);
    }
}