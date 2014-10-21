package org.onlab.onos.mastership;

import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.event.AbstractEvent;
import org.onlab.onos.net.DeviceId;

/**
 * Describes a device mastership event.
 */
public class MastershipEvent extends AbstractEvent<MastershipEvent.Type, DeviceId> {

    //do we worry about explicitly setting slaves/equals? probably not,
    //to keep it simple
    NodeId node;

    /**
     * Type of mastership events.
     */
    public enum Type {
        /**
         * Signifies that the master for a device has changed.
         */
        MASTER_CHANGED,

        /**
         * Signifies that the list of backup nodes has changed.
         */
        BACKUPS_CHANGED
    }

    /**
     * Creates an event of a given type and for the specified device, master,
     * and the current time.
     *
     * @param type   device event type
     * @param device event device subject
     * @param node master ID subject
     */
    public MastershipEvent(Type type, DeviceId device, NodeId node) {
        super(type, device);
        this.node = node;
    }

    /**
     * Creates an event of a given type and for the specified device, master,
     * and time.
     *
     * @param type   mastership event type
     * @param device event device subject
     * @param master master ID subject
     * @param time   occurrence time
     */
    public MastershipEvent(Type type, DeviceId device, NodeId master, long time) {
        super(type, device, time);
        this.node = master;
    }

    /**
     * Returns the NodeID of the node associated with the event.
     * For MASTER_CHANGED this is the newly elected master, and for
     * BACKUPS_CHANGED, this is the node that was newly added, removed, or
     * whose position was changed in the list.
     *
     * @return node ID as a subject
     */
    public NodeId node() {
        return node;
    }
}
