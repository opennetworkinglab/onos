package org.onlab.onos.net;

/**
 * Representation of a network edge location where an end-station host is
 * connected.
 */
public class HostLocation extends ConnectPoint {

    // Note that time is explicitly excluded from the notion of equality.
    private final long time;

    /**
     * Creates a new host location using the supplied device &amp; port.
     *
     * @param deviceId   device identity
     * @param portNumber device port number
     * @param time       time when detected, in millis since start of epoch
     */
    public HostLocation(DeviceId deviceId, PortNumber portNumber, long time) {
        super(deviceId, portNumber);
        this.time = time;
    }

    /**
     * Creates a new host location derived from the supplied connection point.
     *
     * @param connectPoint connection point
     * @param time         time when detected, in millis since start of epoch
     */
    public HostLocation(ConnectPoint connectPoint, long time) {
        super(connectPoint.deviceId(), connectPoint.port());
        this.time = time;
    }

    /**
     * Returns the time when the location was established, given in
     * milliseconds since start of epoch.
     *
     * @return time in milliseconds since start of epoch
     */
    public long time() {
        return time;
    }

}
