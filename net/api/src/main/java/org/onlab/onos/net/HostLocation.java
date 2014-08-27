package org.onlab.onos.net;

/**
 * Representation of a network edge location where an end-station host is
 * connected.
 */
public interface HostLocation extends ConnectPoint {

    /**
     * Returns the timestamp when the location was established, given in
     * milliseconds since start of epoch.
     *
     * @return timestamp in milliseconds since start of epoch
     */
    long time();

}
