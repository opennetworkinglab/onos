package org.onlab.onos.cluster;

import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.MastershipRole;

import java.util.Set;

/**
 * Test adapter for mastership service.
 */
public class MastershipServiceAdapter implements MastershipService {
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
    public void addListener(MastershipListener listener) {
    }

    @Override
    public void removeListener(MastershipListener listener) {
    }

    @Override
    public MastershipTermService requestTermService() {
        return null;
    }
}
