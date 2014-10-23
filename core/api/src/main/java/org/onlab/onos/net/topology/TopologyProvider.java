package org.onlab.onos.net.topology;

import org.onlab.onos.net.provider.Provider;

/**
 * Means for injecting topology information into the core.
 */
public interface TopologyProvider extends Provider {

    /**
     * Triggers topology recomputation.
     */
    void triggerRecompute();

}
