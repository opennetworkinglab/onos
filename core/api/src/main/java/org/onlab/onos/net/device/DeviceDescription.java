package org.onlab.onos.net.device;

import org.onlab.onos.net.Description;
import org.onlab.onos.net.Device;
import org.onlab.packet.ChassisId;

import java.net.URI;

/**
 * Carrier of immutable information about a device.
 */
public interface DeviceDescription extends Description {

    /**
     * Protocol/provider specific URI that can be used to encode the identity
     * information required to communicate with the device externally, e.g.
     * datapath ID.
     *
     * @return provider specific URI for the device
     */
    URI deviceURI();

    /**
     * Returns the type of the infrastructure device.
     *
     * @return type of the device
     */
    Device.Type type();

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

    /**
     * Returns a device chassis id.
     *
     * @return chassis id
     */
    ChassisId chassisId();

}
