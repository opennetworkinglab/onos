package org.onlab.onos.net.device;

import org.onlab.onos.net.Device;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Port;
import org.onlab.onos.net.PortNumber;

import java.util.List;

/**
 * Test adapter for device service.
 */
public class DeviceServiceAdapter implements DeviceService {
    @Override
    public int getDeviceCount() {
        return 0;
    }

    @Override
    public Iterable<Device> getDevices() {
        return null;
    }

    @Override
    public Device getDevice(DeviceId deviceId) {
        return null;
    }

    @Override
    public DeviceMastershipRole getRole(DeviceId deviceId) {
        return null;
    }

    @Override
    public List<Port> getPorts(DeviceId deviceId) {
        return null;
    }

    @Override
    public Port getPort(DeviceId deviceId, PortNumber portNumber) {
        return null;
    }

    @Override
    public boolean isAvailable(DeviceId deviceId) {
        return false;
    }

    @Override
    public void addListener(DeviceListener listener) {
    }

    @Override
    public void removeListener(DeviceListener listener) {
    }

}
