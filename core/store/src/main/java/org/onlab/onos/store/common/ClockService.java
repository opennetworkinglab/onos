package org.onlab.onos.store.common;

import org.onlab.onos.cluster.MastershipTerm;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.store.Timestamp;

/**
 * Interface for a logical clock service that vends per device timestamps.
 */
public interface ClockService {
    
    /**
     * Returns a new timestamp for the specified deviceId.
     * @param deviceId device identifier.
     * @return timestamp.
     */
    public Timestamp getTimestamp(DeviceId deviceId);
    
    /**
     * Updates the mastership term for the specified deviceId.
     * @param deviceId device identifier.
     * @param term mastership term.
     */
    public void setMastershipTerm(DeviceId deviceId, MastershipTerm term);
}
