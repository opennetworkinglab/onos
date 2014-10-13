package org.onlab.onos.net.host;

import org.onlab.onos.net.HostId;
import org.onlab.onos.store.Timestamp;

/**
 * Interface for a logical clock service that issues per host timestamps.
 */
public interface HostClockService {

    /**
     * Returns a new timestamp for the specified host.
     * @param hostId identifier for the host.
     * @return timestamp.
     */
    public Timestamp getTimestamp(HostId hostId);
}
