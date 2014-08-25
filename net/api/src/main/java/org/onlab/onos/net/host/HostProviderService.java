package org.onlab.onos.net.host;

import org.onlab.onos.net.ProviderService;

/**
 * Means of conveying host information to the core.
 */
public interface HostProviderService extends ProviderService {

    void hostDetected(HostDescription hostDescription);

}
