/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.common;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSetMultimap.Builder;
import org.onlab.graph.DijkstraGraphSearch;
import org.onlab.graph.DisjointPathPair;
import org.onlab.graph.GraphPathSearch;
import org.onlab.graph.GraphPathSearch.Result;
import org.onlab.graph.SrlgGraphSearch;
import org.onlab.graph.SuurballeGraphSearch;
import org.onlab.graph.TarjanGraphSearch;
import org.onlab.graph.TarjanGraphSearch.SccResult;
import org.onosproject.net.AbstractModel;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultDisjointPath;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.DeviceId;
import org.onosproject.net.DisjointPath;
import org.onosproject.net.Link;
import org.onosproject.net.Link.Type;
import org.onosproject.net.Path;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.topology.ClusterId;
import org.onosproject.net.topology.DefaultTopologyCluster;
import org.onosproject.net.topology.DefaultTopologyVertex;
import org.onosproject.net.topology.GraphDescription;
import org.onosproject.net.topology.HopCountLinkWeight;
import org.onosproject.net.topology.LinkWeight;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyCluster;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyGraph;
import org.onosproject.net.topology.TopologyVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static org.onlab.graph.GraphPathSearch.ALL_PATHS;
import static org.onlab.util.Tools.isNullOrEmpty;
import static org.onosproject.core.CoreService.CORE_PROVIDER_ID;
import static org.onosproject.net.Link.State.INACTIVE;
import static org.onosproject.net.Link.Type.INDIRECT;

/**
 * Default implementation of the topology descriptor. This carries the backing
 * topology data.
 */
public class DefaultTopology extends AbstractModel implements Topology {

    private static final Logger log = LoggerFactory.getLogger(DefaultTopology.class);

    private static final DijkstraGraphSearch<TopologyVertex, TopologyEdge> DIJKSTRA = new DijkstraGraphSearch<>();
    private static final TarjanGraphSearch<TopologyVertex, TopologyEdge> TARJAN = new TarjanGraphSearch<>();
    private static final SuurballeGraphSearch<TopologyVertex, TopologyEdge> SUURBALLE = new SuurballeGraphSearch<>();

    private static LinkWeight defaultLinkWeight = null;
    private static GraphPathSearch<TopologyVertex, TopologyEdge> defaultGraphPathSearch = null;

    private final long time;
    private final long creationTime;
    private final long computeCost;
    private final TopologyGraph graph;

    private final LinkWeight hopCountWeight;

    private final Supplier<SccResult<TopologyVertex, TopologyEdge>> clusterResults;
    private final Supplier<ImmutableMap<ClusterId, TopologyCluster>> clusters;
    private final Supplier<ImmutableSet<ConnectPoint>> infrastructurePoints;
    private final Supplier<ImmutableSetMultimap<ClusterId, ConnectPoint>> broadcastSets;
    private final Function<ConnectPoint, Boolean> broadcastFunction;
    private final Supplier<ClusterIndexes> clusterIndexes;

    /**
     * Sets the default link-weight to be used when computing paths. If null is
     * specified, the builtin default link-weight measuring hop-counts will be
     * used.
     *
     * @param linkWeight new default link-weight
     */
    public static void setDefaultLinkWeight(LinkWeight linkWeight) {
        log.info("Setting new default link-weight function to {}", linkWeight);
        defaultLinkWeight = linkWeight;
    }

    /**
     * Sets the default lpath search algorighm to be used when computing paths.
     * If null is specified, the builtin default Dijkstra will be used.
     *
     * @param graphPathSearch new default algorithm
     */
    public static void setDefaultGraphPathSearch(GraphPathSearch<TopologyVertex, TopologyEdge> graphPathSearch) {
        log.info("Setting new default graph path algorithm to {}", graphPathSearch);
        defaultGraphPathSearch = graphPathSearch;
    }


    /**
     * Creates a topology descriptor attributed to the specified provider.
     *
     * @param providerId        identity of the provider
     * @param description       data describing the new topology
     * @param broadcastFunction broadcast point function
     */
    public DefaultTopology(ProviderId providerId, GraphDescription description,
                           Function<ConnectPoint, Boolean> broadcastFunction) {
        super(providerId);
        this.broadcastFunction = broadcastFunction;
        this.time = description.timestamp();
        this.creationTime = description.creationTime();

        // Build the graph
        this.graph = new DefaultTopologyGraph(description.vertexes(),
                                              description.edges());

        this.clusterResults = Suppliers.memoize(() -> searchForClusters());
        this.clusters = Suppliers.memoize(() -> buildTopologyClusters());

        this.clusterIndexes = Suppliers.memoize(() -> buildIndexes());

        this.hopCountWeight = new HopCountLinkWeight(graph.getVertexes().size());
        this.broadcastSets = Suppliers.memoize(() -> buildBroadcastSets());
        this.infrastructurePoints = Suppliers.memoize(() -> findInfrastructurePoints());
        this.computeCost = Math.max(0, System.nanoTime() - time);
    }

    /**
     * Creates a topology descriptor attributed to the specified provider.
     *
     * @param providerId  identity of the provider
     * @param description data describing the new topology
     */
    public DefaultTopology(ProviderId providerId, GraphDescription description) {
        this(providerId, description, null);
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
    public TopologyGraph getGraph() {
        return graph;
    }

    /**
     * Returns the set of topology clusters.
     *
     * @return set of clusters
     */
    public Set<TopologyCluster> getClusters() {
        return ImmutableSet.copyOf(clusters.get().values());
    }

    /**
     * Returns the specified topology cluster.
     *
     * @param clusterId cluster identifier
     * @return topology cluster
     */
    public TopologyCluster getCluster(ClusterId clusterId) {
        return clusters.get().get(clusterId);
    }

    /**
     * Returns the topology cluster that contains the given device.
     *
     * @param deviceId device identifier
     * @return topology cluster
     */
    public TopologyCluster getCluster(DeviceId deviceId) {
        return clustersByDevice().get(deviceId);
    }

    /**
     * Returns the set of cluster devices.
     *
     * @param cluster topology cluster
     * @return cluster devices
     */
    public Set<DeviceId> getClusterDevices(TopologyCluster cluster) {
        return devicesByCluster().get(cluster);
    }

    /**
     * Returns the set of cluster links.
     *
     * @param cluster topology cluster
     * @return cluster links
     */
    public Set<Link> getClusterLinks(TopologyCluster cluster) {
        return linksByCluster().get(cluster);
    }

    /**
     * Indicates whether the given point is an infrastructure link end-point.
     *
     * @param connectPoint connection point
     * @return true if infrastructure
     */
    public boolean isInfrastructure(ConnectPoint connectPoint) {
        return infrastructurePoints.get().contains(connectPoint);
    }

    /**
     * Indicates whether the given point is part of a broadcast set.
     *
     * @param connectPoint connection point
     * @return true if in broadcast set
     */
    public boolean isBroadcastPoint(ConnectPoint connectPoint) {
        if (broadcastFunction != null) {
            return broadcastFunction.apply(connectPoint);
        }

        // Any non-infrastructure, i.e. edge points are assumed to be OK.
        if (!isInfrastructure(connectPoint)) {
            return true;
        }

        // Find the cluster to which the device belongs.
        TopologyCluster cluster = clustersByDevice().get(connectPoint.deviceId());
        checkArgument(cluster != null, "No cluster found for device %s", connectPoint.deviceId());

        // If the broadcast set is null or empty, or if the point explicitly
        // belongs to it, return true.
        Set<ConnectPoint> points = broadcastSets.get().get(cluster.id());
        return isNullOrEmpty(points) || points.contains(connectPoint);
    }

    /**
     * Returns the size of the cluster broadcast set.
     *
     * @param clusterId cluster identifier
     * @return size of the cluster broadcast set
     */
    public int broadcastSetSize(ClusterId clusterId) {
        return broadcastSets.get().get(clusterId).size();
    }

    /**
     * Returns the set of the cluster broadcast points.
     *
     * @param clusterId cluster identifier
     * @return set of cluster broadcast points
     */
    public Set<ConnectPoint> broadcastPoints(ClusterId clusterId) {
        return broadcastSets.get().get(clusterId);
    }

    /**
     * Returns the set of pre-computed shortest paths between source and
     * destination devices.
     *
     * @param src source device
     * @param dst destination device
     * @return set of shortest paths
     */
    public Set<Path> getPaths(DeviceId src, DeviceId dst) {
        return getPaths(src, dst, linkWeight());
    }

    /**
     * Computes on-demand the set of shortest paths between source and
     * destination devices.
     *
     * @param src    source device
     * @param dst    destination device
     * @param weight link weight function
     * @return set of shortest paths
     */
    public Set<Path> getPaths(DeviceId src, DeviceId dst, LinkWeight weight) {
        DefaultTopologyVertex srcV = new DefaultTopologyVertex(src);
        DefaultTopologyVertex dstV = new DefaultTopologyVertex(dst);
        Set<TopologyVertex> vertices = graph.getVertexes();
        if (!vertices.contains(srcV) || !vertices.contains(dstV)) {
            // src or dst not part of the current graph
            return ImmutableSet.of();
        }

        GraphPathSearch.Result<TopologyVertex, TopologyEdge> result =
                graphPathSearch().search(graph, srcV, dstV, weight, ALL_PATHS);
        ImmutableSet.Builder<Path> builder = ImmutableSet.builder();
        for (org.onlab.graph.Path<TopologyVertex, TopologyEdge> path : result.paths()) {
            builder.add(networkPath(path));
        }
        return builder.build();
    }

    /**
     * /**
     * Returns the set of pre-computed shortest disjoint path pairs between source and
     * destination devices.
     *
     * @param src source device
     * @param dst destination device
     * @return set of shortest disjoint path pairs
     */
    public Set<DisjointPath> getDisjointPaths(DeviceId src, DeviceId dst) {
        return getDisjointPaths(src, dst, linkWeight());
    }

    /**
     * Computes on-demand the set of shortest disjoint path pairs between source and
     * destination devices.
     *
     * @param src    source device
     * @param dst    destination device
     * @param weight link weight function
     * @return set of disjoint shortest path pairs
     */
    public Set<DisjointPath> getDisjointPaths(DeviceId src, DeviceId dst, LinkWeight weight) {
        DefaultTopologyVertex srcV = new DefaultTopologyVertex(src);
        DefaultTopologyVertex dstV = new DefaultTopologyVertex(dst);
        Set<TopologyVertex> vertices = graph.getVertexes();
        if (!vertices.contains(srcV) || !vertices.contains(dstV)) {
            // src or dst not part of the current graph
            return ImmutableSet.of();
        }

        GraphPathSearch.Result<TopologyVertex, TopologyEdge> result =
                SUURBALLE.search(graph, srcV, dstV, weight, ALL_PATHS);
        ImmutableSet.Builder<DisjointPath> builder = ImmutableSet.builder();
        for (org.onlab.graph.Path<TopologyVertex, TopologyEdge> path : result.paths()) {
            DisjointPath disjointPath =
                    networkDisjointPath((org.onlab.graph.DisjointPathPair<TopologyVertex, TopologyEdge>) path);
            if (disjointPath.backup() != null) {
                builder.add(disjointPath);
            }
        }
        return builder.build();
    }

    /**
     * Computes on-demand the set of shortest disjoint risk groups path pairs between source and
     * destination devices.
     *
     * @param src         source device
     * @param dst         destination device
     * @param weight      edge weight object
     * @param riskProfile map representing risk groups for each edge
     * @return set of shortest disjoint paths
     */
    private Set<DisjointPath> disjointPaths(DeviceId src, DeviceId dst, LinkWeight weight,
                                            Map<TopologyEdge, Object> riskProfile) {
        DefaultTopologyVertex srcV = new DefaultTopologyVertex(src);
        DefaultTopologyVertex dstV = new DefaultTopologyVertex(dst);

        Set<TopologyVertex> vertices = graph.getVertexes();
        if (!vertices.contains(srcV) || !vertices.contains(dstV)) {
            // src or dst not part of the current graph
            return ImmutableSet.of();
        }

        SrlgGraphSearch<TopologyVertex, TopologyEdge> srlg = new SrlgGraphSearch<>(riskProfile);
        GraphPathSearch.Result<TopologyVertex, TopologyEdge> result =
                srlg.search(graph, srcV, dstV, weight, ALL_PATHS);
        ImmutableSet.Builder<DisjointPath> builder = ImmutableSet.builder();
        for (org.onlab.graph.Path<TopologyVertex, TopologyEdge> path : result.paths()) {
            DisjointPath disjointPath =
                    networkDisjointPath((org.onlab.graph.DisjointPathPair<TopologyVertex, TopologyEdge>) path);
            if (disjointPath.backup() != null) {
                builder.add(disjointPath);
            }
        }
        return builder.build();
    }

    /**
     * Computes on-demand the set of shortest disjoint risk groups path pairs between source and
     * destination devices.
     *
     * @param src         source device
     * @param dst         destination device
     * @param weight      edge weight object
     * @param riskProfile map representing risk groups for each link
     * @return set of shortest disjoint paths
     */
    public Set<DisjointPath> getDisjointPaths(DeviceId src, DeviceId dst, LinkWeight weight,
                                              Map<Link, Object> riskProfile) {
        Map<TopologyEdge, Object> riskProfile2 = new HashMap<>();
        for (Link l : riskProfile.keySet()) {
            riskProfile2.put(new TopologyEdge() {
                Link cur = l;

                @Override
                public Link link() {
                    return cur;
                }

                @Override
                public TopologyVertex src() {
                    return () -> src;
                }

                @Override
                public TopologyVertex dst() {
                    return () -> dst;
                }
            }, riskProfile.get(l));
        }
        return disjointPaths(src, dst, weight, riskProfile2);
    }

    /**
     * Computes on-demand the set of shortest disjoint risk groups path pairs between source and
     * destination devices.
     *
     * @param src         source device
     * @param dst         destination device
     * @param riskProfile map representing risk groups for each link
     * @return set of shortest disjoint paths
     */
    public Set<DisjointPath> getDisjointPaths(DeviceId src, DeviceId dst, Map<Link, Object> riskProfile) {
        return getDisjointPaths(src, dst, linkWeight(), riskProfile);
    }

    // Converts graph path to a network path with the same cost.
    private Path networkPath(org.onlab.graph.Path<TopologyVertex, TopologyEdge> path) {
        List<Link> links = path.edges().stream().map(TopologyEdge::link).collect(Collectors.toList());
        return new DefaultPath(CORE_PROVIDER_ID, links, path.cost());
    }

    private DisjointPath networkDisjointPath(DisjointPathPair<TopologyVertex, TopologyEdge> path) {
        if (!path.hasBackup()) {
            // There was no secondary path available.
            return new DefaultDisjointPath(CORE_PROVIDER_ID,
                                           (DefaultPath) networkPath(path.primary()),
                                           null);
        }
        return new DefaultDisjointPath(CORE_PROVIDER_ID,
                                       (DefaultPath) networkPath(path.primary()),
                                       (DefaultPath) networkPath(path.secondary()));
    }

    // Searches for SCC clusters in the network topology graph using Tarjan
    // algorithm.
    private SccResult<TopologyVertex, TopologyEdge> searchForClusters() {
        return TARJAN.search(graph, new NoIndirectLinksWeight());
    }

    // Builds the topology clusters and returns the id-cluster bindings.
    private ImmutableMap<ClusterId, TopologyCluster> buildTopologyClusters() {
        ImmutableMap.Builder<ClusterId, TopologyCluster> clusterBuilder = ImmutableMap.builder();
        SccResult<TopologyVertex, TopologyEdge> results = clusterResults.get();

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
            if ((minVertex == null) || (vertex.deviceId()
                    .toString().compareTo(minVertex.deviceId().toString()) < 0)) {
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
        Result<TopologyVertex, TopologyEdge> result = DIJKSTRA.search(graph, cluster.root(), null, hopCountWeight, 1);
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
            if (edge.link().type() == Type.EDGE) {
                // exclude EDGE link from infrastructure link
                // - Device <-> Host
                // - Device <-> remote domain Device
                continue;
            }
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
                                  devicesBuilder.build(),
                                  linksBuilder.build());
    }

    private GraphPathSearch<TopologyVertex, TopologyEdge> graphPathSearch() {
        return defaultGraphPathSearch != null ? defaultGraphPathSearch : DIJKSTRA;
    }

    private LinkWeight linkWeight() {
        return defaultLinkWeight != null ? defaultLinkWeight : hopCountWeight;
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

        public ClusterIndexes(ImmutableMap<DeviceId, TopologyCluster> clustersByDevice,
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
                .add("creationTime", creationTime)
                .add("computeCost", computeCost)
                .add("clusters", clusterCount())
                .add("devices", deviceCount())
                .add("links", linkCount()).toString();
    }
}
