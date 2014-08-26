package org.onlab.onos.net.device;

import org.onlab.onos.net.MastershipRole;
import org.onlab.onos.provider.ProviderService;

import java.util.List;

/**
 * Service through which device providers can inject device information into
 * the core.
 */
public interface DeviceProviderService extends ProviderService {

    // TODO: define suspend and remove actions on the mezzanine administrative API

    /**
     * Signals the core that a device has connected or has been detected somehow.
     *
     * @param deviceDescription information about network device
     * @return mastership role chosen by the provider service
     */
    MastershipRole deviceConnected(DeviceDescription deviceDescription);

    /**
     * Signals the core that a device has disconnected or is no longer reachable.
     *
     * @param deviceDescription device to be removed
     */
    void deviceDisconnected(DeviceDescription deviceDescription);

    /**
     * Sends information about all ports of a device. It is up to the core to
     * determine what has changed.
     *
     * @param ports list of device ports
     */
    void updatePorts(List<PortDescription> ports);

    /**
     * Used to notify the core about port status change of a single port.
     *
     * @param port description of the port that changed
     */
    void portStatusChanged(PortDescription port);

}
