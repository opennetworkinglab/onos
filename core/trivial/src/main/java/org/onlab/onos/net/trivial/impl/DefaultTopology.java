package org.onlab.onos.net.trivial.impl;

import org.onlab.onos.net.AbstractModel;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.net.topology.Topology;

/**
 * Default implementation of the topology descriptor. This carries the
 * backing topology data.
 */
public class DefaultTopology extends AbstractModel implements Topology {

    private final long time;
    private final int clusterCount;
    private final int deviceCount;
    private final int linkCount;
    private final int pathCount;

    /**
     * Creates a topology descriptor attributed to the specified provider.
     *
     * @param providerId   identity of the provider
     * @param time         creation time in system nanos
     * @param clusterCount number of clusters
     * @param deviceCount  number of devices
     * @param linkCount    number of links
     * @param pathCount    number of pre-computed paths
     */
    DefaultTopology(ProviderId providerId, long time, int clusterCount,
                    int deviceCount, int linkCount, int pathCount) {
        super(providerId);
        this.time = time;
        this.clusterCount = clusterCount;
        this.deviceCount = deviceCount;
        this.linkCount = linkCount;
        this.pathCount = pathCount;
    }

    @Override
    public long time() {
        return time;
    }

    @Override
    public int clusterCount() {
        return clusterCount;
    }

    @Override
    public int deviceCount() {
        return deviceCount;
    }

    @Override
    public int linkCount() {
        return linkCount;
    }

    @Override
    public int pathCount() {
        return pathCount;
    }

}
