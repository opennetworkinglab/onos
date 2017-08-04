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
package org.onosproject.cluster;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

/**
 * An immutable container for role information for a device,
 * within the current cluster. Role attributes include current
 * master and a preference-ordered list of backup nodes.
 */
public class RoleInfo {
    private final Optional<NodeId> master;
    private final List<NodeId> backups;

    public RoleInfo(NodeId master, List<NodeId> backups) {
        this.master = Optional.ofNullable(master);
        this.backups = ImmutableList.copyOf(backups);
    }

    public RoleInfo() {
        this.master = Optional.empty();
        this.backups = ImmutableList.of();
    }

    // This will return a Optional<NodeId> in the future.
    public NodeId master() {
        return master.orElseGet(() -> null);
    }

    public List<NodeId> backups() {
        return backups;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof RoleInfo)) {
            return false;
        }
        RoleInfo that = (RoleInfo) other;

        return Objects.equals(this.master, that.master)
                && Objects.equals(this.backups, that.backups);
    }

    @Override
    public int hashCode() {
        return Objects.hash(master, backups);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this.getClass())
                .add("master", master.orElseGet(() -> null))
            .add("backups", backups)
            .toString();
    }
}
