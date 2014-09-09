package org.onlab.onos.net.host;

import org.onlab.onos.net.Host;
import org.onlab.onos.net.provider.Provider;

/**
 * Provider of information about hosts and their location on the network.
 */
public interface HostProvider extends Provider {

    /**
     * Triggers an asynchronous probe of the specified host, intended to
     * determine whether the host is present or not. An indirect result of this
     * should be invocation of {@link org.onlab.onos.net.host.HostProviderService#hostDetected}
     * or {@link org.onlab.onos.net.host.HostProviderService#hostVanished}
     * at some later point in time.
     *
     * @param host host to probe
     */
    void triggerProbe(Host host);

}
