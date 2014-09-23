package org.onlab.onos.cluster;

import org.onlab.onos.event.AbstractEvent;
import org.onlab.onos.net.DeviceId;

/**
 * Describes a device mastership event.
 */
public class MastershipEvent extends AbstractEvent<MastershipEvent.Type, DeviceId> {

    NodeId master;

    /**
     * Type of mastership events.
     */
    public enum Type {
        /**
         * Signifies that the master for a device has changed.
         */
        MASTER_CHANGED
    }

    /**
     * Creates an event of a given type and for the specified device, master,
     * and the current time.
     *
     * @param type   device event type
     * @param device event device subject
     * @param master master ID subject
     */
    protected MastershipEvent(Type type, DeviceId device, NodeId master) {
        super(type, device);
        this.master = master;
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
    protected MastershipEvent(Type type, DeviceId device, NodeId master, long time) {
        super(type, device, time);
        this.master = master;
    }

    /**
     * Returns the current master's ID as a subject.
     *
     * @return master ID subject
     */
    public NodeId master() {
        return master;
    }
}
