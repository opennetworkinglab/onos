package org.onlab.onos.cluster;

import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.MastershipRole;
import org.onlab.onos.net.device.DeviceMastershipListener;
import org.onlab.onos.net.device.DeviceMastershipService;
import org.onlab.onos.net.device.DeviceMastershipTermService;

import java.util.Set;

/**
 * Test adapter for mastership service.
 */
public class MastershipServiceAdapter implements DeviceMastershipService {
    @Override
    public MastershipRole getLocalRole(DeviceId deviceId) {
        return null;
    }

    @Override
    public MastershipRole requestRoleFor(DeviceId deviceId) {
        return null;
    }

    @Override
    public void relinquishMastership(DeviceId deviceId) {
    }

    @Override
    public NodeId getMasterFor(DeviceId deviceId) {
        return null;
    }

    @Override
    public Set<DeviceId> getDevicesOf(NodeId nodeId) {
        return null;
    }

    @Override
    public void addListener(DeviceMastershipListener listener) {
    }

    @Override
    public void removeListener(DeviceMastershipListener listener) {
    }

    @Override
    public DeviceMastershipTermService requestTermService() {
        return null;
    }
}
