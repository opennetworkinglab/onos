package org.onlab.onos.net;

import org.onlab.onos.net.provider.Provided;

/**
 * Abstraction of a network infrastructure link.
 */
public interface Link extends Provided {
// TODO: Consider extending graph Edge<Element> once the graph module is available

    /**
     * Coarse representation of the link type.
     */
    public enum Type {
        /**
         * Signifies that this is a direct single-segment link.
         */
        DIRECT,

        /**
         * Signifies that this link is potentially comprised from multiple
         * underlying segments or hops, and as such should be used to tag
         * links traversing optical paths, tunnels or intervening 'dark'
         * switches.
         */
        INDIRECT
    }

    /**
     * Returns the link source connection point.
     *
     * @return link source connection point
     */
    ConnectPoint src();

    /**
     * Returns the link destination connection point.
     *
     * @return link destination connection point
     */
    ConnectPoint dst();

    // LinkInfo info(); // Additional link information / decorations

}
