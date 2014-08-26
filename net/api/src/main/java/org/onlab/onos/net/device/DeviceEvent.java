package org.onlab.onos.net.device;

import org.onlab.onos.event.AbstractEvent;
import org.onlab.onos.net.Device;

/**
 * Describes infrastructure device event.
 */
public class DeviceEvent extends AbstractEvent<DeviceEvent.Type, Device> {

    /**
     * Type of device events.
     */
    public enum Type {
        /** Signifies that a new device has been detected. */
        DEVICE_ADDED,

        /** Signifies that a device has been removed. */
        DEVICE_REMOVED,

        /** Signifies that a device has been administratively suspended. */
        DEVICE_SUSPENDED,

        /** Signifies that a device has come online or has gone offline. */
        DEVICE_AVAILABILITY_CHANGED,

        /**
         * Signifies that the current controller instance relationship has
         * changed with respect to a device.
         */
        DEVICE_MASTERSHIP_CHANGED
    }

    /**
     * Creates an event of a given type and for the specified subject and the
     * current time.
     *
     * @param type    event type
     * @param subject event subject
     */
    public DeviceEvent(Type type, Device subject) {
        super(type, subject);
    }

    /**
     * Creates an event of a given type and for the specified subject and time.
     *
     * @param type    event type
     * @param subject event subject
     * @param time    occurrence time
     */
    public DeviceEvent(Type type, Device subject, long time) {
        super(type, subject, time);
    }

}
