/*
 * Copyright 2014-2015 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.store.trivial;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onlab.graph.GraphPathSearch.ALL_PATHS;
import static org.onosproject.core.CoreService.CORE_PROVIDER_ID;
import static org.onosproject.net.Link.State.ACTIVE;
import static org.onosproject.net.Link.State.INACTIVE;
import static org.onosproject.net.Link.Type.INDIRECT;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.onlab.graph.DijkstraGraphSearch;
import org.onlab.graph.GraphPathSearch;
import org.onlab.graph.GraphPathSearch.Result;
import org.onlab.graph.TarjanGraphSearch;
import org.onlab.graph.TarjanGraphSearch.SCCResult;
import org.onosproject.net.AbstractModel;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.topology.ClusterId;
import org.onosproject.net.topology.DefaultTopologyCluster;
import org.onosproject.net.topology.DefaultTopologyVertex;
import org.onosproject.net.topology.GraphDescription;
import org.onosproject.net.topology.LinkWeight;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyCluster;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyGraph;
import org.onosproject.net.topology.TopologyVertex;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSetMultimap.Builder;

// FIXME: Move to onos-core-common when ready
/**
 * Default implementation of the topology descriptor. This carries the backing
 * topology data.
 */
public class DefaultTopology extends AbstractModel implements Topology {

    private static final DijkstraGraphSearch<TopologyVertex, TopologyEdge> DIJKSTRA = new DijkstraGraphSearch<>();
    private static final TarjanGraphSearch<TopologyVertex, TopologyEdge> TARJAN = new TarjanGraphSearch<>();

    private final long time;
    private final long creationTime;
    private final long computeCost;
    private final TopologyGraph graph;

    private final LinkWeight weight;
    private final Supplier<SCCResult<TopologyVertex, TopologyEdge>> clusterResults;
    private final Supplier<ImmutableMap<ClusterId, TopologyCluster>> clusters;
    private final Supplier<ImmutableSet<ConnectPoint>> infrastructurePoints;
    private final Supplier<ImmutableSetMultimap<ClusterId, ConnectPoint>> broadcastSets;

    private final Supplier<ClusterIndexes> clusterIndexes;

    /**
     * Creates a topology descriptor attributed to the specified provider.
     *
     * @param providerId
     *            identity of the provider
     * @param description
     *            data describing the new topology
     */
    DefaultTopology(ProviderId providerId, GraphDescription description) {
        super(providerId);
        this.time = description.timestamp();
        this.creationTime = description.creationTime();

        // Build the graph
        this.graph = new DefaultTopologyGraph(description.vertexes(),
                description.edges());

        this.clusterResults = Suppliers.memoize(() -> searchForClusters());
        this.clusters = Suppliers.memoize(() -> buildTopologyClusters());

        this.clusterIndexes = Suppliers.memoize(() -> buildIndexes());

        this.weight = new HopCountLinkWeight(graph.getVertexes().size());
        this.broadcastSets = Suppliers.memoize(() -> buildBroadcastSets());
        this.infrastructurePoints = Suppliers
                .memoize(() -> findInfrastructurePoints());
        this.computeCost = Math.max(0, System.nanoTime() - time);
    }

    @Override
    public long time() {
        return time;
    }

    @Override
    public long creationTime() {
        return creationTime;
    }

    @Override
    public long computeCost() {
        return computeCost;
    }

    @Override
    public int clusterCount() {
        return clusters.get().size();
    }

    @Override
    public int deviceCount() {
        return graph.getVertexes().size();
    }

    @Override
    public int linkCount() {
        return graph.getEdges().size();
    }

    private ImmutableMap<DeviceId, TopologyCluster> clustersByDevice() {
        return clusterIndexes.get().clustersByDevice;
    }

    private ImmutableSetMultimap<TopologyCluster, DeviceId> devicesByCluster() {
        return clusterIndexes.get().devicesByCluster;
    }

    private ImmutableSetMultimap<TopologyCluster, Link> linksByCluster() {
        return clusterIndexes.get().linksByCluster;
    }

    /**
     * Returns the backing topology graph.
     *
     * @return topology graph
     */
    TopologyGraph getGraph() {
        return graph;
    }

    /**
     * Returns the set of topology clusters.
     *
     * @return set of clusters
     */
    Set<TopologyCluster> getClusters() {
        return ImmutableSet.copyOf(clusters.get().values());
    }

    /**
     * Returns the specified topology cluster.
     *
     * @param clusterId cluster identifier
     *
     * @return topology cluster
     */
    TopologyCluster getCluster(ClusterId clusterId) {
        return clusters.get().get(clusterId);
    }

    /**
     * Returns the topology cluster that contains the given device.
     *
     * @param deviceId device identifier
     *
     * @return topology cluster
     */
    TopologyCluster getCluster(DeviceId deviceId) {
        return clustersByDevice().get(deviceId);
    }

    /**
     * Returns the set of cluster devices.
     *
     * @param cluster topology cluster
     *
     * @return cluster devices
     */
    Set<DeviceId> getClusterDevices(TopologyCluster cluster) {
        return devicesByCluster().get(cluster);
    }

    /**
     * Returns the set of cluster links.
     *
     * @param cluster topology cluster
     *
     * @return cluster links
     */
    Set<Link> getClusterLinks(TopologyCluster cluster) {
        return linksByCluster().get(cluster);
    }

    /**
     * Indicates whether the given point is an infrastructure link end-point.
     *
     * @param connectPoint connection point
     *
     * @return true if infrastructure
     */
    boolean isInfrastructure(ConnectPoint connectPoint) {
        return infrastructurePoints.get().contains(connectPoint);
    }

    /**
     * Indicates whether the given point is part of a broadcast set.
     *
     * @param connectPoint connection point
     *
     * @return true if in broadcast set
     */
    boolean isBroadcastPoint(ConnectPoint connectPoint) {
        // Any non-infrastructure, i.e. edge points are assumed to be OK.
        if (!isInfrastructure(connectPoint)) {
            return true;
        }

        // Find the cluster to which the device belongs.
        TopologyCluster cluster = clustersByDevice().get(connectPoint.deviceId());
        if (cluster == null) {
            throw new IllegalArgumentException("No cluster found for device "
                    + connectPoint.deviceId());
        }

        // If the broadcast set is null or empty, or if the point explicitly
        // belongs to it, return true;
        Set<ConnectPoint> points = broadcastSets.get().get(cluster.id());
        return (points == null) || points.isEmpty() || points.contains(connectPoint);
    }

    /**
     * Returns the size of the cluster broadcast set.
     *
     * @param clusterId cluster identifier
     *
     * @return size of the cluster broadcast set
     */
    int broadcastSetSize(ClusterId clusterId) {
        return broadcastSets.get().get(clusterId).size();
    }

    /**
     * Returns the set of pre-computed shortest paths between source and
     * destination devices.
     *
     * @param src source device
     *
     * @param dst destination device
     *
     * @return set of shortest paths
     */
    Set<Path> getPaths(DeviceId src, DeviceId dst) {
        return getPaths(src, dst, null);
    }

    /**
     * Computes on-demand the set of shortest paths between source and
     * destination devices.
     *
     * @param src source device
     *
     * @param dst destination device
     *
     * @param weight link weight function
     *
     * @return set of shortest paths
     */
    Set<Path> getPaths(DeviceId src, DeviceId dst, LinkWeight weight) {
        final DefaultTopologyVertex srcV = new DefaultTopologyVertex(src);
        final DefaultTopologyVertex dstV = new DefaultTopologyVertex(dst);
        Set<TopologyVertex> vertices = graph.getVertexes();
        if (!vertices.contains(srcV) || !vertices.contains(dstV)) {
            // src or dst not part of the current graph
            return ImmutableSet.of();
        }

        GraphPathSearch.Result<TopologyVertex, TopologyEdge> result =
                DIJKSTRA.search(graph, srcV, dstV, weight, ALL_PATHS);
        ImmutableSet.Builder<Path> builder = ImmutableSet.builder();
        for (org.onlab.graph.Path<TopologyVertex, TopologyEdge> path : result.paths()) {
            builder.add(networkPath(path));
        }
        return builder.build();
    }

    // Converts graph path to a network path with the same cost.
    private Path networkPath(org.onlab.graph.Path<TopologyVertex, TopologyEdge> path) {
        List<Link> links = new ArrayList<>();
        for (TopologyEdge edge : path.edges()) {
            links.add(edge.link());
        }
        return new DefaultPath(CORE_PROVIDER_ID, links, path.cost());
    }

    // Searches for SCC clusters in the network topology graph using Tarjan
    // algorithm.
    private SCCResult<TopologyVertex, TopologyEdge> searchForClusters() {
        return TARJAN.search(graph, new NoIndirectLinksWeight());
    }

    // Builds the topology clusters and returns the id-cluster bindings.
    private ImmutableMap<ClusterId, TopologyCluster> buildTopologyClusters() {
        ImmutableMap.Builder<ClusterId, TopologyCluster> clusterBuilder = ImmutableMap.builder();
        SCCResult<TopologyVertex, TopologyEdge> results = clusterResults.get();
        // Extract both vertexes and edges from the results; the lists form
        // pairs along the same index.
        List<Set<TopologyVertex>> clusterVertexes = results.clusterVertexes();
        List<Set<TopologyEdge>> clusterEdges = results.clusterEdges();

        // Scan over the lists and create a cluster from the results.
        for (int i = 0, n = results.clusterCount(); i < n; i++) {
            Set<TopologyVertex> vertexSet = clusterVertexes.get(i);
            Set<TopologyEdge> edgeSet = clusterEdges.get(i);

            ClusterId cid = ClusterId.clusterId(i);
            DefaultTopologyCluster cluster = new DefaultTopologyCluster(cid,
                                                                        vertexSet.size(),
                                                                        edgeSet.size(),
                                                                        findRoot(vertexSet));
            clusterBuilder.put(cid, cluster);
        }
        return clusterBuilder.build();
    }

    // Finds the vertex whose device id is the lexicographical minimum in the
    // specified set.
    private TopologyVertex findRoot(Set<TopologyVertex> vertexSet) {
        TopologyVertex minVertex = null;
        for (TopologyVertex vertex : vertexSet) {
            if ((minVertex == null) || (minVertex.deviceId().toString()
                    .compareTo(minVertex.deviceId().toString()) < 0)) {
                minVertex = vertex;
            }
        }
        return minVertex;
    }

    // Processes a map of broadcast sets for each cluster.
    private ImmutableSetMultimap<ClusterId, ConnectPoint> buildBroadcastSets() {
        Builder<ClusterId, ConnectPoint> builder = ImmutableSetMultimap.builder();
        for (TopologyCluster cluster : clusters.get().values()) {
            addClusterBroadcastSet(cluster, builder);
        }
        return builder.build();
    }

    // Finds all broadcast points for the cluster. These are those connection
    // points which lie along the shortest paths between the cluster root and
    // all other devices within the cluster.
    private void addClusterBroadcastSet(TopologyCluster cluster, Builder<ClusterId, ConnectPoint> builder) {
        // Use the graph root search results to build the broadcast set.
        Result<TopologyVertex, TopologyEdge> result =
                DIJKSTRA.search(graph, cluster.root(), null, weight, 1);
        for (Map.Entry<TopologyVertex, Set<TopologyEdge>> entry : result.parents().entrySet()) {
            TopologyVertex vertex = entry.getKey();

            // Ignore any parents that lead outside the cluster.
            if (clustersByDevice().get(vertex.deviceId()) != cluster) {
                continue;
            }

            // Ignore any back-link sets that are empty.
            Set<TopologyEdge> parents = entry.getValue();
            if (parents.isEmpty()) {
                continue;
            }

            // Use the first back-link source and destinations to add to the
            // broadcast set.
            Link link = parents.iterator().next().link();
            builder.put(cluster.id(), link.src());
            builder.put(cluster.id(), link.dst());
        }
    }

    // Collects and returns an set of all infrastructure link end-points.
    private ImmutableSet<ConnectPoint> findInfrastructurePoints() {
        ImmutableSet.Builder<ConnectPoint> builder = ImmutableSet.builder();
        for (TopologyEdge edge : graph.getEdges()) {
            builder.add(edge.link().src());
            builder.add(edge.link().dst());
        }
        return builder.build();
    }

    // Builds cluster-devices, cluster-links and device-cluster indexes.
    private ClusterIndexes buildIndexes() {
        // Prepare the index builders
        ImmutableMap.Builder<DeviceId, TopologyCluster> clusterBuilder =
                ImmutableMap.builder();
        ImmutableSetMultimap.Builder<TopologyCluster, DeviceId> devicesBuilder =
                ImmutableSetMultimap.builder();
        ImmutableSetMultimap.Builder<TopologyCluster, Link> linksBuilder =
                ImmutableSetMultimap.builder();

        // Now scan through all the clusters
        for (TopologyCluster cluster : clusters.get().values()) {
            int i = cluster.id().index();

            // Scan through all the cluster vertexes.
            for (TopologyVertex vertex : clusterResults.get().clusterVertexes().get(i)) {
                devicesBuilder.put(cluster, vertex.deviceId());
                clusterBuilder.put(vertex.deviceId(), cluster);
            }

            // Scan through all the cluster edges.
            for (TopologyEdge edge : clusterResults.get().clusterEdges().get(i)) {
                linksBuilder.put(cluster, edge.link());
            }
        }

        // Finalize all indexes.
        return new ClusterIndexes(clusterBuilder.build(),
                devicesBuilder.build(), linksBuilder.build());
    }

    // Link weight for measuring link cost as hop count with indirect links
    // being as expensive as traversing the entire graph to assume the worst.
    private static class HopCountLinkWeight implements LinkWeight {
        private final int indirectLinkCost;

        HopCountLinkWeight(int indirectLinkCost) {
            this.indirectLinkCost = indirectLinkCost;
        }

        @Override
        public double weight(TopologyEdge edge) {
            // To force preference to use direct paths first, make indirect
            // links as expensive as the linear vertex traversal.
            return edge.link().state() ==
                    ACTIVE ? (edge.link().type() ==
                    INDIRECT ? indirectLinkCost : 1) : -1;
        }
    }

    // Link weight for preventing traversal over indirect links.
    private static class NoIndirectLinksWeight implements LinkWeight {
        @Override
        public double weight(TopologyEdge edge) {
            return (edge.link().state() == INACTIVE)
                    || (edge.link().type() == INDIRECT) ? -1 : 1;
        }
    }

    static final class ClusterIndexes {
        final ImmutableMap<DeviceId, TopologyCluster> clustersByDevice;
        final ImmutableSetMultimap<TopologyCluster, DeviceId> devicesByCluster;
        final ImmutableSetMultimap<TopologyCluster, Link> linksByCluster;

        public ClusterIndexes(
                ImmutableMap<DeviceId, TopologyCluster> clustersByDevice,
                ImmutableSetMultimap<TopologyCluster, DeviceId> devicesByCluster,
                ImmutableSetMultimap<TopologyCluster, Link> linksByCluster) {
            this.clustersByDevice = clustersByDevice;
            this.devicesByCluster = devicesByCluster;
            this.linksByCluster = linksByCluster;
        }
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("time", time)
                .add("created", creationTime)
                .add("computeCost", computeCost)
                .add("clusters", clusterCount())
                .add("devices", deviceCount())
                .add("links", linkCount()).toString();
    }
}
