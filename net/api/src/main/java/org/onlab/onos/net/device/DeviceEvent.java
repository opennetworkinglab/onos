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
        /**
         * Signifies that a new device has been detected.
         */
        DEVICE_ADDED,

        /**
         * Signifies that some device attributes have changed; excludes
         * availability changes.
         */
        DEVICE_UPDATED,

        /**
         * Signifies that a device has been removed.
         */
        DEVICE_REMOVED,

        /**
         * Signifies that a device has been administratively suspended.
         */
        DEVICE_SUSPENDED,

        /**
         * Signifies that a device has come online or has gone offline.
         */
        DEVICE_AVAILABILITY_CHANGED,

        /**
         * Signifies that the current controller instance relationship has
         * changed with respect to a device.
         */
        DEVICE_MASTERSHIP_CHANGED
    }

    /**
     * Creates an event of a given type and for the specified device and the
     * current time.
     *
     * @param type   device event type
     * @param device event device subject
     */
    public DeviceEvent(Type type, Device device) {
        super(type, device);
    }

    /**
     * Creates an event of a given type and for the specified device and time.
     *
     * @param type   device event type
     * @param device event device subject
     * @param time   occurrence time
     */
    public DeviceEvent(Type type, Device device, long time) {
        super(type, device, time);
    }

}
