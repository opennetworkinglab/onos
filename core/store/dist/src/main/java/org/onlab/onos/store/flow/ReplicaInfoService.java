package org.onlab.onos.store.flow;

import org.onlab.onos.net.DeviceId;

/**
 * Service to return where the Replica should be placed.
 */
public interface ReplicaInfoService {

    // returns where it should be.
    /**
     * Returns the placement information for given Device.
     *
     * @param deviceId identifier of the device
     * @return placement information
     */
    ReplicaInfo getReplicaInfoFor(DeviceId deviceId);
}
