package org.onlab.onos.net.host;

import org.onlab.onos.event.AbstractEvent;
import org.onlab.onos.net.Host;

/**
 * Describes end-station host event.
 */
public class HostEvent extends AbstractEvent<HostEvent.Type, Host> {

    /**
     * Type of host events.
     */
    public enum Type {
        /**
         * Signifies that a new host has been detected.
         */
        HOST_ADDED,

        /**
         * Signifies that a host has been removed.
         */
        HOST_REMOVED,

        /**
         * Signifies that a host location has changed.
         */
        HOST_MOVED
    }

    /**
     * Creates an event of a given type and for the specified host and the
     * current time.
     *
     * @param type host event type
     * @param host event host subject
     */
    public HostEvent(Type type, Host host) {
        super(type, host);
    }

    /**
     * Creates an event of a given type and for the specified host and time.
     *
     * @param type host event type
     * @param host event host subject
     * @param time occurrence time
     */
    public HostEvent(Type type, Host host, long time) {
        super(type, host, time);
    }

}
