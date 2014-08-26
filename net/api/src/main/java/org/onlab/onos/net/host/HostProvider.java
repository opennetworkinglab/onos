package org.onlab.onos.net.host;

import org.onlab.onos.net.Host;
import org.onlab.onos.provider.Provider;

/**
 * Provider of information about hosts and their location on the network.
 */
public interface HostProvider extends Provider {

    // TODO: consider how dirty the triggerProbe gets; if it costs too much, let's drop it

    /**
     * Triggers an asynchronous probe of the specified host, intended to
     * determine whether the host is present or not. An indirect result of this
     * should be invocation of {@link org.onlab.onos.net.host.HostProviderService#hostDetected(HostDescription)} or
     * {@link org.onlab.onos.net.host.HostProviderService#hostNotDetected(HostDescription)}
     * at some later point in time.
     *
     * @param host host to probe
     */
    void triggerProbe(Host host);

}
