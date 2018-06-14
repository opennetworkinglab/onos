/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.store.flow;

import com.google.common.base.Objects;
import org.onosproject.cluster.NodeId;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Class to represent placement information about Master/Backup copy.
 */
public final class ReplicaInfo {

    private final long term;
    private final Optional<NodeId> master;
    private final List<NodeId> backups;

    /**
     * Creates a ReplicaInfo instance.
     *
     * @param term monotonically increasing unique mastership term
     * @param master NodeId of the node where the master copy should be
     * @param backups list of NodeId, where backup copies should be placed
     */
    public ReplicaInfo(long term, NodeId master, List<NodeId> backups) {
        this.term = term;
        this.master = Optional.ofNullable(master);
        this.backups = checkNotNull(backups);
    }

    /**
     * Returns the mastership term.
     *
     * @return the mastership term
     */
    public long term() {
        return term;
    }

    /**
     * Returns the NodeId, if there is a Node where the master copy should be.
     *
     * @return NodeId, where the master copy should be placed
     */
    public Optional<NodeId> master() {
        return master;
    }

    /**
     * Returns the collection of NodeId, where backup copies should be placed.
     *
     * @return collection of NodeId, where backup copies should be placed
     */
    public List<NodeId> backups() {
        return backups;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(master, backups);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ReplicaInfo))  {
            return false;
        }
        ReplicaInfo that = (ReplicaInfo) other;
        return Objects.equal(this.master, that.master) &&
                Objects.equal(this.backups, that.backups);
    }

    // for Serializer
    private ReplicaInfo() {
        this.term = 0;
        this.master = Optional.empty();
        this.backups = Collections.emptyList();
    }
}
