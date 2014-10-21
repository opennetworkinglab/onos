package org.onlab.onos.cluster;

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
        return Objects.hash(master, backups.hashCode());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this.getClass())
            .add("master", master)
            .add("backups", backups)
            .toString();
    }
}
