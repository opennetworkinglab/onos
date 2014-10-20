package org.onlab.onos.net.resource;

import java.util.Objects;

/**
 * Representation of bandwidth resource.
 */
public final class Bandwidth extends LinkResource {

    private final double bandwidth;

    /**
     * Creates a new instance with given bandwidth.
     *
     * @param bandwidth bandwidth value to be assigned
     */
    private Bandwidth(double bandwidth) {
        this.bandwidth = bandwidth;
    }

    /**
     * Creates a new instance with given bandwidth.
     *
     * @param bandwidth bandwidth value to be assigned
     * @return {@link Bandwidth} instance with given bandwidth
     */
    public static Bandwidth valueOf(double bandwidth) {
        return new Bandwidth(bandwidth);
    }

    /**
     * Returns bandwidth as a double value.
     *
     * @return bandwidth as a double value
     */
    public double toDouble() {
        return bandwidth;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Bandwidth) {
            Bandwidth that = (Bandwidth) obj;
            return Objects.equals(this.bandwidth, that.bandwidth);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.bandwidth);
    }

    @Override
    public String toString() {
        return String.valueOf(this.bandwidth);
    }
}
