package org.onlab.onos.net;

import java.util.Objects;

/**
 * Representation of a network edge location where an end-station host is
 * connected.
 */
public class HostLocation extends ConnectPoint {

    private final long time;

    public HostLocation(DeviceId deviceId, PortNumber portNumber, long time) {
        super(deviceId, portNumber);
        this.time = time;
    }

    /**
     * Returns the timestamp when the location was established, given in
     * milliseconds since start of epoch.
     *
     * @return timestamp in milliseconds since start of epoch
     */
    public long time() {
        return time;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(time);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof HostLocation) {
            final HostLocation other = (HostLocation) obj;
            return super.equals(obj) && Objects.equals(this.time, other.time);
        }
        return false;
    }

}
