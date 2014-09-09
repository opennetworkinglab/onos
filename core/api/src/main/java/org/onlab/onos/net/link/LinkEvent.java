package org.onlab.onos.net.link;

import org.onlab.onos.event.AbstractEvent;
import org.onlab.onos.net.Link;

/**
 * Describes infrastructure link event.
 */
public class LinkEvent extends AbstractEvent<LinkEvent.Type, Link> {

    /**
     * Type of link events.
     */
    public enum Type {
        /**
         * Signifies that a new link has been detected.
         */
        LINK_ADDED,

        /**
         * Signifies that a link has been updated.
         */
        LINK_UPDATED,

        /**
         * Signifies that a link has been removed.
         */
        LINK_REMOVED
    }

    /**
     * Creates an event of a given type and for the specified link and the
     * current time.
     *
     * @param type link event type
     * @param link event link subject
     */
    public LinkEvent(Type type, Link link) {
        super(type, link);
    }

    /**
     * Creates an event of a given type and for the specified link and time.
     *
     * @param type link event type
     * @param link event link subject
     * @param time occurrence time
     */
    public LinkEvent(Type type, Link link, long time) {
        super(type, link, time);
    }

}
