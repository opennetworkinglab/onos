package org.onlab.onos.net.device;

/**
 * Opaque version structure.
 * <p>
 * Classes implementing this interface must also implement
 * {@link #hashCode()} and {@link #equals(Object)}.
 */
public interface Timestamp extends Comparable<Timestamp> {

    @Override
    public abstract int hashCode();

    @Override
    public abstract boolean equals(Object obj);
}
