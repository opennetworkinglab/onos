package org.onlab.onos.store.flow;

import org.onlab.onos.net.DeviceId;

/**
 * Service to return where the replica should be placed.
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

    /**
     * Adds the specified replica placement info change listener.
     *
     * @param listener the replica placement info change listener
     */
    void addListener(ReplicaInfoEventListener listener);

    /**
     * Removes the specified replica placement info change listener.
     *
     * @param listener the replica placement info change listener
     */
    void removeListener(ReplicaInfoEventListener listener);

}
