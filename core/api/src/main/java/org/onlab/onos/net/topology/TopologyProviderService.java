package org.onlab.onos.net.topology;

import org.onlab.onos.event.Event;
import org.onlab.onos.net.provider.ProviderService;

import java.util.List;

/**
 * Means for injecting topology information into the core.
 */
public interface TopologyProviderService extends ProviderService<TopologyProvider> {

    /**
     * Signals the core that some aspect of the topology has changed.
     *
     * @param graphDescription information about the network graph
     * @param reasons          events that triggered topology change
     */
    void topologyChanged(GraphDescription graphDescription,
                         List<Event> reasons);

}
