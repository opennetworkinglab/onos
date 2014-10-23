package org.onlab.onos.mastership;

import org.onlab.onos.cluster.RoleInfo;
import org.onlab.onos.event.AbstractEvent;
import org.onlab.onos.net.DeviceId;

/**
 * Describes a device mastership event.
 */
public class MastershipEvent extends AbstractEvent<MastershipEvent.Type, DeviceId> {

    //Contains master and standby information.
    RoleInfo roleInfo;

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
     * Creates an event of a given type and for the specified device,
     * role information, and the current time.
     *
     * @param type   device event type
     * @param device event device subject
     * @param info mastership role information subject
     */
    public MastershipEvent(Type type, DeviceId device, RoleInfo info) {
        super(type, device);
        this.roleInfo = info;
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
    public MastershipEvent(Type type, DeviceId device, RoleInfo info, long time) {
        super(type, device, time);
        this.roleInfo = info;
    }

    /**
     * Returns the current role state for the subject.
     *
     * @return RoleInfo associated with Device ID subject
     */
    public RoleInfo roleInfo() {
        return roleInfo;
    }
}
