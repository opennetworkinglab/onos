package org.onlab.onos.net.trivial.topology.provider.impl;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.onlab.graph.AdjacencyListsGraph;
import org.onlab.graph.DijkstraGraphSearch;
import org.onlab.graph.Graph;
import org.onlab.graph.GraphPathSearch;
import org.onlab.graph.TarjanGraphSearch;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.topology.ClusterId;
import org.onlab.onos.net.topology.DefaultTopologyCluster;
import org.onlab.onos.net.topology.LinkWeight;
import org.onlab.onos.net.topology.TopoEdge;
import org.onlab.onos.net.topology.TopoVertex;
import org.onlab.onos.net.topology.TopologyCluster;
import org.onlab.onos.net.topology.TopologyDescription;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.ImmutableSetMultimap.Builder;
import static com.google.common.collect.ImmutableSetMultimap.builder;
import static org.onlab.graph.GraphPathSearch.Result;
import static org.onlab.graph.TarjanGraphSearch.SCCResult;
import static org.onlab.onos.net.Link.Type.INDIRECT;

/**
 * Default implementation of an immutable topology data carrier.
 */
class DefaultTopologyDescription implements TopologyDescription {

    private static final GraphPathSearch<TopoVertex, TopoEdge> DIJKSTRA =
            new DijkstraGraphSearch<>();
    private static final TarjanGraphSearch<TopoVertex, TopoEdge> TARJAN =
            new TarjanGraphSearch<>();

    private final long nanos;
    private final Map<DeviceId, TopoVertex> vertexesById = Maps.newHashMap();
    private final Graph<TopoVertex, TopoEdge> graph;
    private final ImmutableMap<DeviceId, Result<TopoVertex, TopoEdge>> results;

    // Cluster-related structures
    private final ImmutableSet<TopologyCluster> clusters;
    private ImmutableSetMultimap<TopologyCluster, DeviceId> devicesByCluster;
    private ImmutableSetMultimap<TopologyCluster, Link> linksByCluster;

    /**
     * Creates a topology description to carry topology vitals to the core.
     *
     * @param nanos   time in nanos of when the topology description was created
     * @param devices collection of infrastructure devices
     * @param links   collection of infrastructure links
     */
    DefaultTopologyDescription(long nanos, Iterable<Device> devices, Iterable<Link> links) {
        this.nanos = nanos;
        this.graph = buildGraph(devices, links);
        this.results = computeDefaultPaths();
        this.clusters = computeClusters();
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
    public ImmutableMap<DeviceId, Result<TopoVertex, TopoEdge>> pathsBySource() {
        return results;
    }

    @Override
    public ImmutableSet<TopologyCluster> clusters() {
        return clusters;
    }

    @Override
    public ImmutableSetMultimap<TopologyCluster, DeviceId> devicesByCluster() {
        return devicesByCluster;
    }

    @Override
    public ImmutableSetMultimap<TopologyCluster, Link> linksByCluster() {
        return linksByCluster;
    }

    // Link weight for measuring link cost as hop count with indirect links
    // being as expensive as traversing the entire graph to assume the worst.
    private static class HopCountLinkWeight implements LinkWeight {
        private final int indirectLinkCost;

        HopCountLinkWeight(int indirectLinkCost) {
            this.indirectLinkCost = indirectLinkCost;
        }

        @Override
        public double weight(TopoEdge edge) {
            // To force preference to use direct paths first, make indirect
            // links as expensive as the linear vertex traversal.
            return edge.link().type() == INDIRECT ? indirectLinkCost : 1;
        }
    }

    // Link weight for preventing traversal over indirect links.
    private static class NoIndirectLinksWeight implements LinkWeight {
        @Override
        public double weight(TopoEdge edge) {
            return edge.link().type() == INDIRECT ? -1 : 1;
        }
    }

    // Constructs the topology graph using the supplied devices and links.
    private Graph<TopoVertex, TopoEdge> buildGraph(Iterable<Device> devices,
                                                   Iterable<Link> links) {
        return new AdjacencyListsGraph<>(buildVertexes(devices),
                                         buildEdges(links));
    }

    // Builds a set of topology vertexes from the specified list of devices
    private Set<TopoVertex> buildVertexes(Iterable<Device> devices) {
        Set<TopoVertex> vertexes = Sets.newHashSet();
        for (Device device : devices) {
            TopoVertex vertex = new DefaultTopoVertex(device.id());
            vertexes.add(vertex);
            vertexesById.put(vertex.deviceId(), vertex);
        }
        return vertexes;
    }

    // Builds a set of topology vertexes from the specified list of links
    private Set<TopoEdge> buildEdges(Iterable<Link> links) {
        Set<TopoEdge> edges = Sets.newHashSet();
        for (Link link : links) {
            edges.add(new DefaultTopoEdge(vertexOf(link.src()),
                                          vertexOf(link.dst()), link));
        }
        return edges;
    }

    // Computes the default shortest paths for all source/dest pairs using
    // the multi-path Dijkstra and hop-count as path cost.
    private ImmutableMap<DeviceId, Result<TopoVertex, TopoEdge>> computeDefaultPaths() {
        LinkWeight weight = new HopCountLinkWeight(graph.getVertexes().size());
        ImmutableMap.Builder<DeviceId, Result<TopoVertex, TopoEdge>> results =
                ImmutableMap.builder();

        // Search graph paths for each source to all destinations.
        for (TopoVertex src : vertexesById.values()) {
            results.put(src.deviceId(), DIJKSTRA.search(graph, src, null, weight));
        }
        return results.build();
    }

    // Computes topology SCC clusters using Tarjan algorithm.
    private ImmutableSet<TopologyCluster> computeClusters() {
        ImmutableSet.Builder<TopologyCluster> clusterBuilder = ImmutableSet.builder();
        SCCResult<TopoVertex, TopoEdge> result =
                TARJAN.search(graph, new NoIndirectLinksWeight());

        // Extract both vertexes and edges from the results; the lists form
        // pairs along the same index.
        List<Set<TopoVertex>> clusterVertexes = result.clusterVertexes();
        List<Set<TopoEdge>> clusterEdges = result.clusterEdges();

        Builder<TopologyCluster, DeviceId> devicesBuilder = ImmutableSetMultimap.builder();
        Builder<TopologyCluster, Link> linksBuilder = ImmutableSetMultimap.builder();

        // Scan over the lists and create a cluster from the results.
        for (int i = 0, n = result.clusterCount(); i < n; i++) {
            Set<TopoVertex> vertexSet = clusterVertexes.get(i);
            Set<TopoEdge> edgeSet = clusterEdges.get(i);

            DefaultTopologyCluster cluster =
                    new DefaultTopologyCluster(ClusterId.clusterId(i),
                                               vertexSet.size(), edgeSet.size(),
                                               findRoot(vertexSet).deviceId());
            clusterBuilder.add(cluster);
            findClusterDevices(vertexSet, cluster, devicesBuilder);
            findClusterLinks(edgeSet, cluster, linksBuilder);
        }
        return clusterBuilder.build();
    }

    // Scans through the set of cluster vertexes and puts their devices in a
    // multi-map associated with the cluster. It also binds the devices to
    // the cluster.
    private void findClusterDevices(Set<TopoVertex> vertexSet,
                                    DefaultTopologyCluster cluster,
                                    Builder<TopologyCluster, DeviceId> builder) {
        for (TopoVertex vertex : vertexSet) {
            DeviceId deviceId = vertex.deviceId();
            builder.put(cluster, deviceId);
        }
    }

    // Scans through the set of cluster edges and puts their links in a
    // multi-map associated with the cluster.
    private void findClusterLinks(Set<TopoEdge> edgeSet,
                                  DefaultTopologyCluster cluster,
                                  Builder<TopologyCluster, Link> builder) {
        for (TopoEdge edge : edgeSet) {
            builder.put(cluster, edge.link());
        }
    }

    // Finds the vertex whose device id is the lexicographical minimum in the
    // specified set.
    private TopoVertex findRoot(Set<TopoVertex> vertexSet) {
        TopoVertex minVertex = null;
        for (TopoVertex vertex : vertexSet) {
            if (minVertex == null ||
                    minVertex.deviceId().toString()
                            .compareTo(minVertex.deviceId().toString()) < 0) {
                minVertex = vertex;
            }
        }
        return minVertex;
    }

    // Fetches a vertex corresponding to the given connection point device.
    private TopoVertex vertexOf(ConnectPoint connectPoint) {
        DeviceId id = connectPoint.deviceId();
        TopoVertex vertex = vertexesById.get(id);
        if (vertex == null) {
            // If vertex does not exist, create one and register it.
            vertex = new DefaultTopoVertex(id);
            vertexesById.put(id, vertex);
        }
        return vertex;
    }

}
