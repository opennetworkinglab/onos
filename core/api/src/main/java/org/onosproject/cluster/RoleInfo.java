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
package org.onosproject.cluster;

import java.util.List;
import java.util.Objects;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

/**
 * An immutable container for role information for a device,
 * within the current cluster. Role attributes include current
 * master and a preference-ordered list of backup nodes.
 */
public class RoleInfo {
    private final NodeId master;
    private final List<NodeId> backups;

    public RoleInfo(NodeId master, List<NodeId> backups) {
        this.master = master;
        this.backups = ImmutableList.copyOf(backups);
    }

    public RoleInfo() {
        this.master = null;
        this.backups = ImmutableList.of();
    }

    public NodeId master() {
        return master;
    }

    public List<NodeId> backups() {
        return backups;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) {
            return false;
        }
        if (!(other instanceof RoleInfo)) {
            return false;
        }
        RoleInfo that = (RoleInfo) other;
        if (!Objects.equals(this.master, that.master)) {
            return false;
        }
        if (!Objects.equals(this.backups, that.backups)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(master, backups);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this.getClass())
            .add("master", master)
            .add("backups", backups)
            .toString();
    }
}
