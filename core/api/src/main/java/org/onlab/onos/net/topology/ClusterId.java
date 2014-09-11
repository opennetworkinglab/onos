package org.onlab.onos.net.topology;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Representation of the topology cluster identity.
 */
public final class ClusterId {

    private final int id;

    // Public construction is prohibit
    private ClusterId(int id) {
        this.id = id;
    }

    /**
     * Returns the cluster identifier, represented by the specified integer
     * serial number.
     *
     * @param id integer serial number
     * @return cluster identifier
     */
    public static ClusterId clusterId(int id) {
        return new ClusterId(id);
    }

    /**
     * Returns the backing integer index.
     *
     * @return backing integer index
     */
    public int index() {
        return id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ClusterId) {
            final ClusterId other = (ClusterId) obj;
            return Objects.equals(this.id, other.id);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("id", id).toString();
    }

}
