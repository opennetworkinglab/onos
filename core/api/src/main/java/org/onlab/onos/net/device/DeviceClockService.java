package org.onlab.onos.net.device;

import org.onlab.onos.net.DeviceId;
import org.onlab.onos.store.Timestamp;

// TODO: Consider renaming to DeviceClockService?
/**
 * Interface for a logical clock service that vends per device timestamps.
 */
public interface DeviceClockService {

    /**
     * Returns a new timestamp for the specified deviceId.
     * @param deviceId device identifier.
     * @return timestamp.
     */
    public Timestamp getTimestamp(DeviceId deviceId);
}
