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
    public MastershipEvent(Type type, DeviceId device, NodeId master) {
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
    public MastershipEvent(Type type, DeviceId device, NodeId master, long time) {
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
