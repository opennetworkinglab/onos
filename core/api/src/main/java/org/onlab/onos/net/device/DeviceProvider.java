package org.onlab.onos.net.device;

import org.onlab.onos.net.Device;
import org.onlab.onos.net.provider.Provider;

/**
 * Abstraction of a device information provider.
 */
public interface DeviceProvider extends Provider {

    // TODO: consider how dirty the triggerProbe gets; if it costs too much, let's drop it

    /**
     * Triggers an asynchronous probe of the specified device, intended to
     * determine whether the host is present or not. An indirect result of this
     * should be invocation of
     * {@link org.onlab.onos.net.device.DeviceProviderService#deviceConnected} )} or
     * {@link org.onlab.onos.net.device.DeviceProviderService#deviceDisconnected}
     * at some later point in time.
     *
     * @param device device to be probed
     */
    void triggerProbe(Device device);

    /**
     * Notifies the provider of a mastership role change for the specified
     * device as decided by the core.
     *
     * @param device  affected device
     * @param newRole newly determined mastership role
     */
    void roleChanged(Device device, DeviceMastershipRole newRole);

}
