package org.onlab.onos.net;

import org.onlab.onos.net.link.LinkDescription;

import java.util.Objects;

import static com.google.common.base.Objects.toStringHelper;

/**
 * Default infrastructure link model implementation.
 */
public class DefaultLink implements LinkDescription {

    private ConnectPoint src;
    private ConnectPoint dst;

    /**
     * Creates a link description using the supplied information.
     *
     * @param src link source
     * @param dst link destination
     */
    public DefaultLink(ConnectPoint src, ConnectPoint dst) {
        this.src = src;
        this.dst = dst;
    }

    @Override
    public ConnectPoint src() {
        return src;
    }

    @Override
    public ConnectPoint dst() {
        return dst;
    }


    @Override
    public int hashCode() {
        return Objects.hash(src, dst);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DefaultDevice) {
            final DefaultLink other = (DefaultLink) obj;
            return Objects.equals(this.src, other.src) &&
                    Objects.equals(this.dst, other.dst);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("src", src)
                .add("dst", dst)
                .toString();
    }

}
