package org.onlab.onos.net.device;

import org.onlab.onos.net.Description;

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

}
