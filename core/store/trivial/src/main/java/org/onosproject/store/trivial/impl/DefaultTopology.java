/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.store.trivial.impl;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import org.onlab.graph.DijkstraGraphSearch;
import org.onlab.graph.GraphPathSearch;
import org.onlab.graph.TarjanGraphSearch;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.collect.ImmutableSetMultimap.Builder;
import static org.onlab.graph.GraphPathSearch.Result;
import static org.onlab.graph.TarjanGraphSearch.SCCResult;
import static org.onosproject.core.CoreService.CORE_PROVIDER_ID;
import static org.onosproject.net.Link.State.ACTIVE;
import static org.onosproject.net.Link.State.INACTIVE;
import static org.onosproject.net.Link.Type.INDIRECT;

/**
 * Default implementation of the topology descriptor. This carries the
 * backing topology data.
 */
public class DefaultTopology extends AbstractModel implements Topology {

    private static final DijkstraGraphSearch<TopologyVertex, TopologyEdge> DIJKSTRA =
            new DijkstraGraphSearch<>();
    private static final TarjanGraphSearch<TopologyVertex, TopologyEdge> TARJAN =
            new TarjanGraphSearch<>();

    private final long time;
    private final long computeCost;
    private final TopologyGraph graph;

    private final SCCResult<TopologyVertex, TopologyEdge> clusterResults;
    private final ImmutableMap<DeviceId, Result<TopologyVertex, TopologyEdge>> results;
    private final ImmutableSetMultimap<PathKey, Path> paths;

    private final ImmutableMap<ClusterId, TopologyCluster> clusters;
    private final ImmutableSet<ConnectPoint> infrastructurePoints;
    private final ImmutableSetMultimap<ClusterId, ConnectPoint> broadcastSets;

    private ImmutableMap<DeviceId, TopologyCluster> clustersByDevice;
    private ImmutableSetMultimap<TopologyCluster, DeviceId> devicesByCluster;
    private ImmutableSetMultimap<TopologyCluster, Link> linksByCluster;


    /**
     * Creates a topology descriptor attributed to the specified provider.
     *
     * @param providerId  identity of the provider
     * @param description data describing the new topology
     */
    DefaultTopology(ProviderId providerId, GraphDescription description) {
        super(providerId);
        this.time = description.timestamp();

        // Build the graph
        this.graph = new DefaultTopologyGraph(description.vertexes(),
                                              description.edges());

        this.results = searchForShortestPaths();
        this.paths = buildPaths();

        this.clusterResults = searchForClusters();
        this.clusters = buildTopologyClusters();

        buildIndexes();

        this.broadcastSets = buildBroadcastSets();
        this.infrastructurePoints = findInfrastructurePoints();
        this.computeCost = Math.max(0, System.nanoTime() - time);
    }

    @Override
    public long time() {
        return time;
    }

    @Override
    public long computeCost() {
        return computeCost;
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
        return paths.size();
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
        return ImmutableSet.copyOf(clusters.values());
    }

    /**
     * Returns the specified topology cluster.
     *
     * @param clusterId cluster identifier
     * @return topology cluster
     */
    TopologyCluster getCluster(ClusterId clusterId) {
        return clusters.get(clusterId);
    }

    /**
     * Returns the topology cluster that contains the given device.
     *
     * @param deviceId device identifier
     * @return topology cluster
     */
    TopologyCluster getCluster(DeviceId deviceId) {
        return clustersByDevice.get(deviceId);
    }

    /**
     * Returns the set of cluster devices.
     *
     * @param cluster topology cluster
     * @return cluster devices
     */
    Set<DeviceId> getClusterDevices(TopologyCluster cluster) {
        return devicesByCluster.get(cluster);
    }

    /**
     * Returns the set of cluster links.
     *
     * @param cluster topology cluster
     * @return cluster links
     */
    Set<Link> getClusterLinks(TopologyCluster cluster) {
        return linksByCluster.get(cluster);
    }

    /**
     * Indicates whether the given point is an infrastructure link end-point.
     *
     * @param connectPoint connection point
     * @return true if infrastructure
     */
    boolean isInfrastructure(ConnectPoint connectPoint) {
        return infrastructurePoints.contains(connectPoint);
    }

    /**
     * Indicates whether the given point is part of a broadcast set.
     *
     * @param connectPoint connection point
     * @return true if in broadcast set
     */
    boolean isBroadcastPoint(ConnectPoint connectPoint) {
        // Any non-infrastructure, i.e. edge points are assumed to be OK.
        if (!isInfrastructure(connectPoint)) {
            return true;
        }

        // Find the cluster to which the device belongs.
        TopologyCluster cluster = clustersByDevice.get(connectPoint.deviceId());
        if (cluster == null) {
            throw new IllegalArgumentException("No cluster found for device " + connectPoint.deviceId());
        }

        // If the broadcast set is null or empty, or if the point explicitly
        // belongs to it, return true;
        Set<ConnectPoint> points = broadcastSets.get(cluster.id());
        return points == null || points.isEmpty() || points.contains(connectPoint);
    }

    /**
     * Returns the size of the cluster broadcast set.
     *
     * @param clusterId cluster identifier
     * @return size of the cluster broadcast set
     */
    int broadcastSetSize(ClusterId clusterId) {
        return broadcastSets.get(clusterId).size();
    }

    /**
     * Returns the set of pre-computed shortest paths between source and
     * destination devices.
     *
     * @param src source device
     * @param dst destination device
     * @return set of shortest paths
     */
    Set<Path> getPaths(DeviceId src, DeviceId dst) {
        return paths.get(new PathKey(src, dst));
    }

    /**
     * Computes on-demand the set of shortest paths between source and
     * destination devices.
     *
     * @param src    source device
     * @param dst    destination device
     * @param weight edge weight function
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
                DIJKSTRA.search(graph, srcV, dstV, weight);
        ImmutableSet.Builder<Path> builder = ImmutableSet.builder();
        for (org.onlab.graph.Path<TopologyVertex, TopologyEdge> path : result.paths()) {
            builder.add(networkPath(path));
        }
        return builder.build();
    }


    // Searches the graph for all shortest paths and returns the search results.
    private ImmutableMap<DeviceId, Result<TopologyVertex, TopologyEdge>> searchForShortestPaths() {
        ImmutableMap.Builder<DeviceId, Result<TopologyVertex, TopologyEdge>> builder = ImmutableMap.builder();

        // Search graph paths for each source to all destinations.
        LinkWeight weight = new HopCountLinkWeight(graph.getVertexes().size());
        for (TopologyVertex src : graph.getVertexes()) {
            builder.put(src.deviceId(), DIJKSTRA.search(graph, src, null, weight));
        }
        return builder.build();
    }

    // Builds network paths from the graph path search results
    private ImmutableSetMultimap<PathKey, Path> buildPaths() {
        Builder<PathKey, Path> builder = ImmutableSetMultimap.builder();
        for (DeviceId deviceId : results.keySet()) {
            Result<TopologyVertex, TopologyEdge> result = results.get(deviceId);
            for (org.onlab.graph.Path<TopologyVertex, TopologyEdge> path : result.paths()) {
                builder.put(new PathKey(path.src().deviceId(), path.dst().deviceId()),
                            networkPath(path));
            }
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
        SCCResult<TopologyVertex, TopologyEdge> result =
                TARJAN.search(graph, new NoIndirectLinksWeight());

        // Extract both vertexes and edges from the results; the lists form
        // pairs along the same index.
        List<Set<TopologyVertex>> clusterVertexes = result.clusterVertexes();
        List<Set<TopologyEdge>> clusterEdges = result.clusterEdges();

        // Scan over the lists and create a cluster from the results.
        for (int i = 0, n = result.clusterCount(); i < n; i++) {
            Set<TopologyVertex> vertexSet = clusterVertexes.get(i);
            Set<TopologyEdge> edgeSet = clusterEdges.get(i);

            ClusterId cid = ClusterId.clusterId(i);
            DefaultTopologyCluster cluster =
                    new DefaultTopologyCluster(cid, vertexSet.size(), edgeSet.size(),
                                               findRoot(vertexSet).deviceId());
            clusterBuilder.put(cid, cluster);
        }
        return clusterBuilder.build();
    }

    // Finds the vertex whose device id is the lexicographical minimum in the
    // specified set.
    private TopologyVertex findRoot(Set<TopologyVertex> vertexSet) {
        TopologyVertex minVertex = null;
        for (TopologyVertex vertex : vertexSet) {
            if (minVertex == null ||
                    minVertex.deviceId().toString()
                            .compareTo(minVertex.deviceId().toString()) < 0) {
                minVertex = vertex;
            }
        }
        return minVertex;
    }

    // Processes a map of broadcast sets for each cluster.
    private ImmutableSetMultimap<ClusterId, ConnectPoint> buildBroadcastSets() {
        Builder<ClusterId, ConnectPoint> builder = ImmutableSetMultimap.builder();
        for (TopologyCluster cluster : clusters.values()) {
            addClusterBroadcastSet(cluster, builder);
        }
        return builder.build();
    }

    // Finds all broadcast points for the cluster. These are those connection
    // points which lie along the shortest paths between the cluster root and
    // all other devices within the cluster.
    private void addClusterBroadcastSet(TopologyCluster cluster,
                                        Builder<ClusterId, ConnectPoint> builder) {
        // Use the graph root search results to build the broadcast set.
        Result<TopologyVertex, TopologyEdge> result = results.get(cluster.root());
        for (Map.Entry<TopologyVertex, Set<TopologyEdge>> entry : result.parents().entrySet()) {
            TopologyVertex vertex = entry.getKey();

            // Ignore any parents that lead outside the cluster.
            if (clustersByDevice.get(vertex.deviceId()) != cluster) {
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
    private void buildIndexes() {
        // Prepare the index builders
        ImmutableMap.Builder<DeviceId, TopologyCluster> clusterBuilder = ImmutableMap.builder();
        ImmutableSetMultimap.Builder<TopologyCluster, DeviceId> devicesBuilder = ImmutableSetMultimap.builder();
        ImmutableSetMultimap.Builder<TopologyCluster, Link> linksBuilder = ImmutableSetMultimap.builder();

        // Now scan through all the clusters
        for (TopologyCluster cluster : clusters.values()) {
            int i = cluster.id().index();

            // Scan through all the cluster vertexes.
            for (TopologyVertex vertex : clusterResults.clusterVertexes().get(i)) {
                devicesBuilder.put(cluster, vertex.deviceId());
                clusterBuilder.put(vertex.deviceId(), cluster);
            }

            // Scan through all the cluster edges.
            for (TopologyEdge edge : clusterResults.clusterEdges().get(i)) {
                linksBuilder.put(cluster, edge.link());
            }
        }

        // Finalize all indexes.
        clustersByDevice = clusterBuilder.build();
        devicesByCluster = devicesBuilder.build();
        linksByCluster = linksBuilder.build();
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
            return edge.link().state() == ACTIVE ?
                    (edge.link().type() == INDIRECT ? indirectLinkCost : 1) : -1;
        }
    }

    // Link weight for preventing traversal over indirect links.
    private static class NoIndirectLinksWeight implements LinkWeight {
        @Override
        public double weight(TopologyEdge edge) {
            return edge.link().state() == INACTIVE || edge.link().type() == INDIRECT ? -1 : 1;
        }
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("time", time)
                .add("computeCost", computeCost)
                .add("clusters", clusterCount())
                .add("devices", deviceCount())
                .add("links", linkCount())
                .add("pathCount", pathCount())
                .toString();
    }
}
