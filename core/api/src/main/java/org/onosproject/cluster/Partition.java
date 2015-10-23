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
package org.onosproject.cluster;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A data partition.
 * <p>
 * Partition represents a slice of the data space and is made up of a collection
 * of {@link org.onosproject.cluster.ControllerNode nodes}
 * that all maintain copies of this data.
 */
public class Partition {

    private final String name;
    private final Set<NodeId> members;

    private Partition() {
        name = null;
        members = null;
    }

    public Partition(String name, Collection<NodeId> members) {
        this.name = checkNotNull(name);
        this.members = ImmutableSet.copyOf(checkNotNull(members));
    }

    /**
     * Returns the partition name.
     * <p>
     * Each partition is identified by a unique name.
     * @return partition name
     */
    public String getName() {
        return this.name;
    }

    /**
     * Returns the collection of controller node identifiers that make up this partition.
     * @return collection of controller node identifiers
     */
    public Collection<NodeId> getMembers() {
        return this.members;
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(new Object[] {name, members});
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || !Partition.class.isInstance(other)) {
            return false;
        }

        Partition that = (Partition) other;

        if (!this.name.equals(that.name) || (this.members == null && that.members != null)
                || (this.members != null && that.members == null) || this.members.size() != that.members.size()) {
            return false;
        }

        return Sets.symmetricDifference(this.members, that.members).isEmpty();
    }
}