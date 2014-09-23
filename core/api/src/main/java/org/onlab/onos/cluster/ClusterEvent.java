package org.onlab.onos.cluster;

import org.onlab.onos.event.AbstractEvent;

/**
 * Describes cluster-related event.
 */
public class ClusterEvent extends AbstractEvent<ClusterEvent.Type, ControllerInstance> {

    /**
     * Type of cluster-related events.
     */
    public enum Type {
        /**
         * Signifies that a new cluster instance has been administratively added.
         */
        INSTANCE_ADDED,

        /**
         * Signifies that a cluster instance has been administratively removed.
         */
        INSTANCE_REMOVED,

        /**
         * Signifies that a cluster instance became active.
         */
        INSTANCE_ACTIVATED,

        /**
         * Signifies that a cluster instance became inactive.
         */
        INSTANCE_DEACTIVATED
    }

    /**
     * Creates an event of a given type and for the specified instance and the
     * current time.
     *
     * @param type     cluster event type
     * @param instance cluster device subject
     */
    public ClusterEvent(Type type, ControllerInstance instance) {
        super(type, instance);
    }

    /**
     * Creates an event of a given type and for the specified device and time.
     *
     * @param type     device event type
     * @param instance event device subject
     * @param time     occurrence time
     */
    public ClusterEvent(Type type, ControllerInstance instance, long time) {
        super(type, instance, time);
    }

}
