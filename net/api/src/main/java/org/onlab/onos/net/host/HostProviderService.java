package org.onlab.onos.net.host;

import org.onlab.onos.net.provider.ProviderService;

/**
 * Means of conveying host information to the core.
 */
public interface HostProviderService extends ProviderService {

    /**
     * Notifies the core when a host has been detected on a network along with
     * information that identifies the hoot location.
     *
     * @param hostDescription description of host and its location
     */
    void hostDetected(HostDescription hostDescription);

    /**
     * Notifies the core when a host is no longer detected on a network.
     *
     * @param hostDescription description of host
     */
    void hostNotDetected(HostDescription hostDescription);

}
