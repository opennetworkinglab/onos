package org.onlab.onos.store.flow;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Collections;

import org.onlab.onos.cluster.NodeId;

import com.google.common.base.Optional;

/**
 * Class to represent placement information about Master/Backup copy.
 */
public final class ReplicaInfo {

    private final Optional<NodeId> master;
    private final Collection<NodeId> backups;

    /**
     * Creates a ReplicaInfo instance.
     *
     * @param master NodeId of the node where the master copy should be
     * @param backups collection of NodeId, where backup copies should be placed
     */
    public ReplicaInfo(NodeId master, Collection<NodeId> backups) {
        this.master = Optional.fromNullable(master);
        this.backups = checkNotNull(backups);
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
    public Collection<NodeId> backups() {
        return backups;
    }

    // for Serializer
    private ReplicaInfo() {
        this.master = Optional.absent();
        this.backups = Collections.emptyList();
    }
}
