package org.onlab.onos.net;

import org.onlab.onos.net.provider.Provided;

/**
 * Representation of a network infrastructure device.
 */
public interface Device extends Provided {

    /**
     * Coarse classification of the type of the infrastructure device.
     */
    public enum Type {
        SWITCH, ROUTER, FIREWALL, BALANCER, IPS, IDS, CONTROLLER, OTHER
    }

    /**
     * Returns the device identifier.
     *
     * @return device id
     */
    DeviceId id();

    /**
     * Returns the type of the infrastructure device.
     *
     * @return type of the device
     */
    Type type();

    /**
     * Returns the device manufacturer name.
     *
     * @return manufacturer name
     */
    String manufacturer();

    /**
     * Returns the device hardware version.
     *
     * @return hardware version
     */
    String hwVersion();

    /**
     * Returns the device software version.
     *
     * @return software version
     */
    String swVersion();

    /**
     * Returns the device serial number.
     *
     * @return serial number
     */
    String serialNumber();

    // Device realizedBy(); ?

    // ports are not provided directly, but rather via DeviceService.getPorts(Device device);

    // Set<Behavior> behaviours(); // set of supported behaviours

}
