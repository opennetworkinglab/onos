package org.onlab.onos.net.trivial.topology.provider.impl;

import org.onlab.onos.net.Link;
import org.onlab.onos.net.topology.TopoEdge;
import org.onlab.onos.net.topology.TopoVertex;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Implementation of the topology edge backed by a link.
 */
class DefaultTopoEdge implements TopoEdge {

    private final Link link;
    private final TopoVertex src;
    private final TopoVertex dst;

    /**
     * Creates a new topology edge.
     *
     * @param src  source vertex
     * @param dst  destination vertex
     * @param link infrastructure link
     */
    DefaultTopoEdge(TopoVertex src, TopoVertex dst, Link link) {
        this.src = src;
        this.dst = dst;
        this.link = link;
    }

    @Override
    public Link link() {
        return link;
    }

    @Override
    public TopoVertex src() {
        return src;
    }

    @Override
    public TopoVertex dst() {
        return dst;
    }

    @Override
    public int hashCode() {
        return Objects.hash(link);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DefaultTopoEdge) {
            final DefaultTopoEdge other = (DefaultTopoEdge) obj;
            return Objects.equals(this.link, other.link);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("src", src).add("dst", dst).toString();
    }

}

