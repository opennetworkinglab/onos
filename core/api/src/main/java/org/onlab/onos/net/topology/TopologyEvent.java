package org.onlab.onos.net.topology;

import org.onlab.onos.event.AbstractEvent;
import org.onlab.onos.event.Event;

import java.util.List;

/**
 * Describes network topology event.
 */
public class TopologyEvent extends AbstractEvent<TopologyEvent.Type, Topology> {

    private final List<Event> reasons;

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
     * @param reasons  list of events that triggered topology change
     */
    public TopologyEvent(Type type, Topology topology, List<Event> reasons) {
        super(type, topology);
        this.reasons = reasons;
    }

    /**
     * Creates an event of a given type and for the specified topology and time.
     *
     * @param type     link event type
     * @param topology event topology subject
     * @param reasons  list of events that triggered topology change
     * @param time     occurrence time
     */
    public TopologyEvent(Type type, Topology topology, List<Event> reasons,
                         long time) {
        super(type, topology, time);
        this.reasons = reasons;
    }


    /**
     * Returns the list of events that triggered the topology change.
     *
     * @return list of events responsible for change in topology; null if
     * initial topology computation
     */
    public List<Event> reasons() {
        return reasons;
    }

}
