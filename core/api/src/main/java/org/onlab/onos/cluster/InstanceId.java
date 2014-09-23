package org.onlab.onos.cluster;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Controller cluster identity.
 */
public class InstanceId {

    private final String id;

    // Default constructor for serialization
    protected InstanceId() {
        id = null;
    }

    /**
     * Creates a new cluster instance identifier from the specified string.
     *
     * @param id string identifier
     */
    public InstanceId(String id) {
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
        if (obj instanceof InstanceId) {
            final InstanceId other = (InstanceId) obj;
            return Objects.equals(this.id, other.id);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("id", id).toString();
    }

}
