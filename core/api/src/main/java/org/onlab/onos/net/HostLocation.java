package org.onlab.onos.net;

/**
 * Representation of a network edge location where an end-station host is
 * connected.
 */
public class HostLocation extends ConnectPoint {

    // Note that time is explicitly excluded from the notion of equality.
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

}
