package org.onlab.onos.net.device;

import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.MastershipRole;
import org.onlab.onos.net.provider.ProviderService;

import java.util.List;

/**
 * Service through which device providers can inject device information into
 * the core.
 */
public interface DeviceProviderService extends ProviderService<DeviceProvider> {

    // TODO: define suspend and remove actions on the mezzanine administrative API

    /**
     * Signals the core that a device has connected or has been detected somehow.
     *
     * @param deviceDescription information about network device
     * @return mastership role chosen by the provider service
     */
    MastershipRole deviceConnected(DeviceId deviceId, DeviceDescription deviceDescription);

    /**
     * Signals the core that a device has disconnected or is no longer reachable.
     *
     * @param deviceId identity of the device to be removed
     */
    void deviceDisconnected(DeviceId deviceId);

    /**
     * Sends information about all ports of a device. It is up to the core to
     * determine what has changed.
     * <p/>
     *
     * @param deviceId identity of the device
     * @param ports list of device ports
     */
    void updatePorts(DeviceId deviceId, List<PortDescription> ports);

    /**
     * Used to notify the core about port status change of a single port.
     *
     * @param deviceId identity of the device
     * @param port description of the port that changed
     */
    void portStatusChanged(DeviceId deviceId, PortDescription port);

}
