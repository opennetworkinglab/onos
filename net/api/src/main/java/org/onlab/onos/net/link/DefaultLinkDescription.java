package org.onlab.onos.net.link;

import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.Link;

/**
 * Default implementation of immutable link description entity.
 */
public class DefaultLinkDescription implements LinkDescription {

    private final ConnectPoint src;
    private final ConnectPoint dst;
    private final Link.Type type;

    /**
     * Creates a link description using the supplied information.
     *
     * @param src  link source
     * @param dst  link destination
     * @param type link type
     */
    public DefaultLinkDescription(ConnectPoint src, ConnectPoint dst, Link.Type type) {
        this.src = src;
        this.dst = dst;
        this.type = type;
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
    public Link.Type type() {
        return null;
    }

}
