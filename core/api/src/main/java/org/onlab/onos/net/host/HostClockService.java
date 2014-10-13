package org.onlab.onos.net.host;

import org.onlab.onos.store.Timestamp;
import org.onlab.packet.MacAddress;

/**
 * Interface for a logical clock service that issues per host timestamps.
 */
public interface HostClockService {

    /**
     * Returns a new timestamp for the specified host mac address.
     * @param hostMac host MAC address.
     * @return timestamp.
     */
    public Timestamp getTimestamp(MacAddress hostMac);
}
