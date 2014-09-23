package org.onlab.onos.cluster;

import java.util.Objects;

/**
 * Controller cluster identity.
 */
public class NodeId {

    private final String id;

    // Default constructor for serialization
    protected NodeId() {
        id = null;
    }

    /**
     * Creates a new cluster node identifier from the specified string.
     *
     * @param id string identifier
     */
    public NodeId(String id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NodeId) {
            final NodeId other = (NodeId) obj;
            return Objects.equals(this.id, other.id);
        }
        return false;
    }

    @Override
    public String toString() {
        return id;
    }

}
