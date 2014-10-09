package org.onlab.onos.net.device;

import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.net.DeviceId;

/**
 * Service for administering the inventory of device masterships.
 */
public interface DeviceMastershipAdminService {

    /**
     * Applies the current mastership role for the specified device.
     *
     * @param instance controller instance identifier
     * @param deviceId device identifier
     * @param role     requested role
     */
    void setRole(NodeId instance, DeviceId deviceId, DeviceMastershipRole role);

}
