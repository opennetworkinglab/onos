package org.onlab.onos.net.trivial.topology.impl;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import org.onlab.graph.Graph;
import org.onlab.graph.GraphPathSearch;
import org.onlab.onos.net.AbstractModel;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.Path;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.net.topology.TopoEdge;
import org.onlab.onos.net.topology.TopoVertex;
import org.onlab.onos.net.topology.Topology;
import org.onlab.onos.net.topology.TopologyCluster;
import org.onlab.onos.net.topology.TopologyDescription;

import java.util.Set;

/**
 * Default implementation of the topology descriptor. This carries the
 * backing topology data.
 */
public class DefaultTopology extends AbstractModel implements Topology {

    private final long time;
    private final int pathCount;

    private final Graph<TopoVertex, TopoEdge> graph;
    private final ImmutableMap<DeviceId, GraphPathSearch.Result<TopoVertex, TopoEdge>> results;

    private final ImmutableSet<TopologyCluster> clusters;
    private final ImmutableSetMultimap<TopologyCluster, DeviceId> devicesByCluster;
    private final ImmutableSetMultimap<TopologyCluster, Link> linksByCluster;
    private final ImmutableSet connectPoints;

    /**
     * Creates a topology descriptor attributed to the specified provider.
     *
     * @param providerId  identity of the provider
     * @param description data describing the new topology
     */
    DefaultTopology(ProviderId providerId, TopologyDescription description) {
        super(providerId);
        this.time = description.timestamp();
        this.graph = description.graph();
        this.results = description.pathsBySource();
        this.clusters = description.clusters();
        this.devicesByCluster = description.devicesByCluster();
        this.linksByCluster = description.linksByCluster();

        this.connectPoints = ImmutableSet.of();
        this.pathCount = 0;
    }

    @Override
    public long time() {
        return time;
    }

    @Override
    public int clusterCount() {
        return clusters.size();
    }

    @Override
    public int deviceCount() {
        return graph.getVertexes().size();
    }

    @Override
    public int linkCount() {
        return graph.getEdges().size();
    }

    @Override
    public int pathCount() {
        return pathCount;
    }

    Set<TopologyCluster> getClusters() {
        return clusters;
    }

    Graph<TopoVertex, TopoEdge> getGraph() {
        return graph;
    }

    boolean isInfrastructure(ConnectPoint connectPoint) {
        return connectPoints.contains(connectPoint);
    }

    boolean isInBroadcastTree(ConnectPoint connectPoint) {
        return false;
    }

    Set<Path> getPaths(DeviceId src, DeviceId dst) {
        return null; // pointToPointPaths.get(key(src, dst));
    }
}
