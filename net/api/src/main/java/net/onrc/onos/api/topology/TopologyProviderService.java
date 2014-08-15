package net.onrc.onos.api.topology;

import net.onrc.onos.api.ProviderService;

/**
 * Means for injecting topology information into the core.
 */
public interface TopologyProviderService extends ProviderService {

    // What can be conveyed in a topology that isn't by individual
    // providers?

    /**
     * Signals the core that some aspect of the topology has changed.
     *
     * @param topoDescription information about topology
     */
    void topologyChanged(TopologyDescription topoDescription);

}
