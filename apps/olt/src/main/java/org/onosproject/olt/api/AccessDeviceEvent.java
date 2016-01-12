package org.onosproject.olt.api;

import org.onlab.packet.VlanId;
import org.onosproject.event.AbstractEvent;
import org.onosproject.net.DeviceId;

import java.util.Optional;

/**
 * Describes an access device event.
 */
public class AccessDeviceEvent extends AbstractEvent<AccessDeviceEvent.Type, DeviceId> {

    private final Optional<VlanId> sVlan;
    private final Optional<VlanId> cVlan;

    public enum Type {
        /**
         * A subscriber was registered and provisioned.
         */
        SUBSCRIBER_REGISTERED,

        /**
         * A subscriber was unregistered and deprovisioned.
         */
        SUBSCRIBER_UNREGISTERED,

        /**
         * An access device connected.
         */
        DEVICE_CONNECTED,

        /**
         * An access device disconnected.
         */
        DEVICE_DISCONNECTED

    }

    /**
     *
     * Creates an event of a given type and for the specified device,
     * along with the cVlanId and sVlanId. The vlan fields may not be provisioned
     * if the event is related to the access device (dis)connection.
     *
     * @param type the event type
     * @param deviceId the device id
     * @param sVlanId the service vlan
     * @param cVlanId the customer vlan
     */
    public AccessDeviceEvent(Type type, DeviceId deviceId,
                             VlanId sVlanId,
                             VlanId cVlanId) {
        super(type, deviceId);
        this.sVlan = Optional.ofNullable(sVlanId);
        this.cVlan = Optional.ofNullable(cVlanId);
    }

    /**
     *
     * Creates an event of a given type and for the specified device, and timestamp
     * along with the cVlanId and sVlanId. The vlan fields may not be provisioned
     * if the event is related to the access device (dis)connection.
     *
     * @param type the event type
     * @param deviceId the device id
     * @param time a timestamp
     * @param sVlanId the service vlan
     * @param cVlanId the customer vlan
     */
    protected AccessDeviceEvent(Type type, DeviceId deviceId, long time,
                                VlanId sVlanId,
                                VlanId cVlanId) {
        super(type, deviceId, time);
        this.sVlan = Optional.ofNullable(sVlanId);
        this.cVlan = Optional.ofNullable(cVlanId);

    }

    public Optional<VlanId> sVlanId() {
        return sVlan;
    }

    public Optional<VlanId> cVlanId() {
        return cVlan;
    }

}
