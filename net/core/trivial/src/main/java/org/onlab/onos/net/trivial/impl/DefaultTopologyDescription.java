package org.onlab.onos.net.trivial.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import org.onlab.graph.Graph;
import org.onlab.graph.GraphPathSearch;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.topology.ClusterId;
import org.onlab.onos.net.topology.TopoEdge;
import org.onlab.onos.net.topology.TopoVertex;
import org.onlab.onos.net.topology.TopologyCluster;
import org.onlab.onos.net.topology.TopologyDescription;

import java.util.Map;
import java.util.Set;

/**
 * Default implementation of an immutable topology data carrier.
 */
public class DefaultTopologyDescription implements TopologyDescription {

    private final long nanos;
    private final Graph<TopoVertex, TopoEdge> graph;
    private final Map<DeviceId, GraphPathSearch.Result<TopoVertex, TopoEdge>> results;
    private final Map<ClusterId, TopologyCluster> clusters;
    private final Multimap<ClusterId, DeviceId> clusterDevices;
    private final Multimap<ClusterId, Link> clusterLinks;
    private final Map<DeviceId, TopologyCluster> deviceClusters;

    public DefaultTopologyDescription(long nanos, Graph<TopoVertex, TopoEdge> graph,
                                      Map<DeviceId, GraphPathSearch.Result<TopoVertex, TopoEdge>> results,
                                      Map<ClusterId, TopologyCluster> clusters,
                                      Multimap<ClusterId, DeviceId> clusterDevices,
                                      Multimap<ClusterId, Link> clusterLinks,
                                      Map<DeviceId, TopologyCluster> deviceClusters) {
        this.nanos = nanos;
        this.graph = graph;
        this.results = results;
        this.clusters = clusters;
        this.clusterDevices = clusterDevices;
        this.clusterLinks = clusterLinks;
        this.deviceClusters = deviceClusters;
    }

    @Override
    public long timestamp() {
        return nanos;
    }

    @Override
    public Graph<TopoVertex, TopoEdge> graph() {
        return graph;
    }

    @Override
    public GraphPathSearch.Result<TopoVertex, TopoEdge> pathResults(DeviceId srcDeviceId) {
        return results.get(srcDeviceId);
    }

    @Override
    public Set<TopologyCluster> clusters() {
        return ImmutableSet.copyOf(clusters.values());
    }

    @Override
    public Set<DeviceId> clusterDevices(TopologyCluster cluster) {
        return null; // clusterDevices.get(cluster.id());
    }

    @Override
    public Set<Link> clusterLinks(TopologyCluster cluster) {
        return null; // clusterLinks.get(cluster.id());
    }

    @Override
    public TopologyCluster clusterFor(DeviceId deviceId) {
        return deviceClusters.get(deviceId);
    }
}
