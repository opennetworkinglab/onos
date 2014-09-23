package org.onlab.onos.cluster;

import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.MastershipRole;
import org.onlab.onos.net.provider.ProviderService;

public interface MastershipProviderService extends
        ProviderService<MastershipProvider> {

    /**
     * Signals the core that mastership has changed for a device.
     *
     * @param deviceId the device ID
     * @param role the new mastership role of this controller instance
     */
    void roleChanged(NodeId nodeId, DeviceId deviceId, MastershipRole role);

}
