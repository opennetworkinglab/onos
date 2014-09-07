package org.onlab.onos.net.link;

import org.onlab.onos.net.ConnectPoint;

/**
 * Default implementation of immutable link description entity.
 */
public class DefaultLinkDescription implements LinkDescription {

    private ConnectPoint src;
    private ConnectPoint dst;

    /**
     * Creates a link description using the supplied information.
     *
     * @param src link source
     * @param dst link destination
     */
    public DefaultLinkDescription(ConnectPoint src, ConnectPoint dst) {
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

}
