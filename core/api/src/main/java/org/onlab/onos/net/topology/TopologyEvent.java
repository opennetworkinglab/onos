package org.onlab.onos.net.topology;

import org.onlab.onos.event.AbstractEvent;

/**
 * Describes network topology event.
 */
public class TopologyEvent extends AbstractEvent<TopologyEvent.Type, Topology> {

    /**
     * Type of topology events.
     */
    public enum Type {
        /**
         * Signifies that topology has changed.
         */
        TOPOLOGY_CHANGED
    }

    /**
     * Creates an event of a given type and for the specified topology and the
     * current time.
     *
     * @param type     topology event type
     * @param topology event topology subject
     */
    public TopologyEvent(Type type, Topology topology) {
        super(type, topology);
    }

    /**
     * Creates an event of a given type and for the specified topology and time.
     *
     * @param type     link event type
     * @param topology event topology subject
     * @param time     occurrence time
     */
    public TopologyEvent(Type type, Topology topology, long time) {
        super(type, topology, time);
    }

}
