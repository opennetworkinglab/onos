package org.onlab.onos.net.host;

import org.onlab.onos.net.HostId;

/**
 * Service for administering the inventory of end-station hosts.
 */
public interface HostAdminService {

    /**
     * Removes the end-station host with the specified identifier.
     *
     * @param hostId host identifier
     */
    void removeHost(HostId hostId);

}
