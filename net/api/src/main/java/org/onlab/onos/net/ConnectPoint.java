package org.onlab.onos.net;

/**
 * Abstraction of a network connection point expressed as a pair of the
 * device identifier and the device port number.
 */
public interface ConnectPoint {

    /**
     * Returns the connection device identifier.
     *
     * @return device id
     */
    DeviceId deviceId();

    /**
     * Returns the connection port number.
     *
     * @return port number
     */
    PortNumber port();

}
