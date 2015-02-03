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
package org.onosproject.store.intent.impl;

import com.google.common.base.MoreObjects;

import java.util.Objects;

/**
 * Identifies a partition of the intent keyspace which will be assigned to and
 * processed by a single ONOS instance at a time.
 */
public class PartitionId {
    private final int id;

    /**
     * Creates a new partition ID.
     *
     * @param id the partition ID
     */
    PartitionId(int id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PartitionId)) {
            return false;
        }

        PartitionId that = (PartitionId) o;
        return Objects.equals(this.id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("partition ID", id)
                .toString();
    }
}
