package org.onlab.onos.net.topology;

import org.onlab.onos.event.Event;
import org.onlab.onos.net.provider.ProviderService;

import java.util.List;

/**
 * Means for injecting topology information into the core.
 */
public interface TopologyProviderService extends ProviderService<TopologyProvider> {

    // What can be conveyed in a topology that isn't by individual
    // providers?

    /**
     * Signals the core that some aspect of the topology has changed.
     *
     * @param topoDescription information about topology
     * @param reasons         events that triggered topology change
     */
    void topologyChanged(TopologyDescription topoDescription,
                         List<Event> reasons);

}
