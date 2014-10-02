package org.onlab.onos.net;

/**
 * Abstraction of a network infrastructure link.
 */
public interface Link extends Annotated, Provided {

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
        INDIRECT,

        /**
         * Signifies that this link is an edge, i.e. host link.
         */
        EDGE
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

    /**
     * Returns the link type.
     *
     * @return link type
     */
    Type type();

    // LinkInfo info(); // Additional link information / decorations

}
