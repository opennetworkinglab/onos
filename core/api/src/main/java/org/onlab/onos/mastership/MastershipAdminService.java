package org.onlab.onos.mastership;

import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.MastershipRole;

/**
 * Service for administering the inventory of device masterships.
 */
public interface MastershipAdminService {

    /**
     * Applies the current mastership role for the specified device.
     *
     * @param instance controller instance identifier
     * @param deviceId device identifier
     * @param role     requested role
     */
    void setRole(NodeId instance, DeviceId deviceId, MastershipRole role);

}
