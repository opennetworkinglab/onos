package org.onlab.onos.net.trivial.impl;

import org.onlab.onos.net.DeviceId;

import java.util.Objects;

/**
 * Key for filing pre-computed paths between source and destination devices.
 */
class PathKey {
    private final DeviceId src;
    private final DeviceId dst;

    /**
     * Creates a path key from the given source/dest pair.
     * @param src source device
     * @param dst destination device
     */
    PathKey(DeviceId src, DeviceId dst) {
        this.src = src;
        this.dst = dst;
    }

    @Override
    public int hashCode() {
        return Objects.hash(src, dst);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PathKey) {
            final PathKey other = (PathKey) obj;
            return Objects.equals(this.src, other.src) && Objects.equals(this.dst, other.dst);
        }
        return false;
    }
}
