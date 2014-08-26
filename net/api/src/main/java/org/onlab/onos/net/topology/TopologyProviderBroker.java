package org.onlab.onos.net.topology;

import org.onlab.onos.net.provider.ProviderBroker;

/**
 * Abstraction of a network topology provider brokerage.
 */
public interface TopologyProviderBroker extends
        ProviderBroker<TopologyProvider, TopologyProviderService> {
}
