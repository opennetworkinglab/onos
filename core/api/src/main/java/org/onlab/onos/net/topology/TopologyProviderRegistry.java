package org.onlab.onos.net.topology;

import org.onlab.onos.net.provider.ProviderRegistry;

/**
 * Abstraction of a network topology provider registry.
 */
public interface TopologyProviderRegistry extends
        ProviderRegistry<TopologyProvider, TopologyProviderService> {
}
