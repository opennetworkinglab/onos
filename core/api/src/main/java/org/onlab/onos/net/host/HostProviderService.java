package org.onlab.onos.net.host;

import org.onlab.onos.net.HostId;
import org.onlab.onos.net.provider.ProviderService;

/**
 * Means of conveying host information to the core.
 */
public interface HostProviderService extends ProviderService<HostProvider> {

    /**
     * Notifies the core when a host has been detected on a network along with
     * information that identifies the hoot location.
     *
     * @param hostId          id of the host that been detected
     * @param hostDescription description of host and its location
     */
    void hostDetected(HostId hostId, HostDescription hostDescription);

    /**
     * Notifies the core when a host is no longer detected on a network.
     *
     * @param hostId id of the host that vanished
     */
    void hostVanished(HostId hostId);

}
