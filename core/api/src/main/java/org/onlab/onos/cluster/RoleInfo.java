package org.onlab.onos.cluster;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A container for detailed role information for a device,
 * within the current cluster. Role attributes include current
 * master and a preference-ordered list of backup nodes.
 */
public class RoleInfo {
    private final NodeId master;
    private final List<NodeId> backups;

    public RoleInfo(NodeId master, List<NodeId> backups) {
        this.master = master;
        this.backups = new LinkedList<>();

        this.backups.addAll(checkNotNull(backups));
    }

    public NodeId master() {
        return master;
    }

    public List<NodeId> backups() {
        return Collections.unmodifiableList(backups);
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
        final StringBuilder builder = new StringBuilder();
        builder.append("master: \n\t").append(master).append("\n");
        builder.append("backups: \n");
        for (NodeId n : backups) {
            builder.append("\t").append(n).append("\n");
        }
        return builder.toString();
    }
}
