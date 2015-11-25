package org.onosproject.segmentrouting.grouphandler;

import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.TrafficTreatment;

import java.util.Objects;

/**
 * Class definition of Key for Device/Port to NextObjective store. Since there
 * can be multiple next objectives to the same physical port, we differentiate
 * between them by including the treatment in the key.
 */
public class PortNextObjectiveStoreKey {
    private final DeviceId deviceId;
    private final PortNumber portNum;
    private final TrafficTreatment treatment;

    public PortNextObjectiveStoreKey(DeviceId deviceId, PortNumber portNum,
                                     TrafficTreatment treatment) {
        this.deviceId = deviceId;
        this.portNum = portNum;
        this.treatment = treatment;
    }

    /**
     * Gets device id in this PortNextObjectiveStoreKey.
     *
     * @return device id
     */
    public DeviceId deviceId() {
        return deviceId;
    }

    /**
     * Gets port information in this PortNextObjectiveStoreKey.
     *
     * @return port information
     */
    public PortNumber portNumber() {
        return portNum;
    }

    /**
     * Gets treatment information in this PortNextObjectiveStoreKey.
     *
     * @return treatment information
     */
    public TrafficTreatment treatment() {
        return treatment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PortNextObjectiveStoreKey)) {
            return false;
        }
        PortNextObjectiveStoreKey that =
                (PortNextObjectiveStoreKey) o;
        return (Objects.equals(this.deviceId, that.deviceId) &&
                Objects.equals(this.portNum, that.portNum) &&
                Objects.equals(this.treatment, that.treatment));
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceId, portNum, treatment);
    }

    @Override
    public String toString() {
        return "Device: " + deviceId + " Port: " + portNum + " Treatment: " + treatment;
    }
}
