package org.onlab.onos.net;

import java.util.Objects;

import com.google.common.base.MoreObjects;

// TODO Consider renaming.
// it's an identifier for a Link, but it's not ElementId, so not using LinkId.
/**
 * Immutable representation of a link identity.
 */
public class LinkKey {

    private final ConnectPoint src;
    private final ConnectPoint dst;

    /**
     * Returns source connection point.
     *
     * @return source connection point
     */
    public ConnectPoint src() {
        return src;
    }

    /**
     * Returns destination connection point.
     *
     * @return destination connection point
     */
    public ConnectPoint dst() {
        return dst;
    }

    /**
     * Creates a link identifier with source and destination connection point.
     *
     * @param src source connection point
     * @param dst destination connection point
     */
    public LinkKey(ConnectPoint src, ConnectPoint dst) {
        this.src = src;
        this.dst = dst;
    }

    @Override
    public int hashCode() {
        return Objects.hash(src(), dst);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof LinkKey) {
            final LinkKey other = (LinkKey) obj;
            return Objects.equals(this.src(), other.src()) &&
                    Objects.equals(this.dst, other.dst);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("src", src())
                .add("dst", dst)
                .toString();
    }
}
