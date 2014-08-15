package net.onrc.onos.api.topology;

import net.onrc.onos.api.ProviderBroker;

/**
 * Abstraction of a network topology provider brokerage.
 */
public interface TopologyProviderBroker extends
        ProviderBroker<TopologyProvider, TopologyProviderService> {
}
