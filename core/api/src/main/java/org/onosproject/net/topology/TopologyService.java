/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.net.topology;

import org.onlab.util.GuavaCollectors;
import org.onosproject.event.ListenerService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.DisjointPath;
import org.onosproject.net.Link;
import org.onosproject.net.Path;

import static org.onosproject.net.topology.HopCountLinkWeigher.DEFAULT_HOP_COUNT_WEIGHER;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Service for providing network topology information.
 */
public interface TopologyService
        extends ListenerService<TopologyEvent, TopologyListener> {

    /**
     * Returns the current topology descriptor.
     *
     * @return current topology
     */
    Topology currentTopology();

    /**
     * Indicates whether the specified topology is the latest or not.
     *
     * @param topology topology descriptor
     * @return true if the topology is the most recent; false otherwise
     */
    boolean isLatest(Topology topology);

    /**
     * Returns the graph view of the specified topology.
     *
     * @param topology topology descriptor
     * @return topology graph view
     */
    TopologyGraph getGraph(Topology topology);

    /**
     * Returns the set of clusters in the specified topology.
     *
     * @param topology topology descriptor
     * @return set of topology clusters
     */
    Set<TopologyCluster> getClusters(Topology topology);

    /**
     * Returns the cluster with the specified ID.
     *
     * @param topology  topology descriptor
     * @param clusterId cluster identifier
     * @return topology cluster
     */
    TopologyCluster getCluster(Topology topology, ClusterId clusterId);

    /**
     * Returns the set of devices that belong to the specified cluster.
     *
     * @param topology topology descriptor
     * @param cluster  topology cluster
     * @return set of cluster devices
     */
    Set<DeviceId> getClusterDevices(Topology topology, TopologyCluster cluster);

    /**
     * Returns the set of links that form the specified cluster.
     *
     * @param topology topology descriptor
     * @param cluster  topology cluster
     * @return set of cluster links
     */
    Set<Link> getClusterLinks(Topology topology, TopologyCluster cluster);

    /**
     * Returns the set of all shortest paths, precomputed in terms of hop-count,
     * between the specified source and destination devices.
     *
     * @param topology topology descriptor
     * @param src      source device
     * @param dst      destination device
     * @return set of all shortest paths between the two devices
     */
    Set<Path> getPaths(Topology topology, DeviceId src, DeviceId dst);

    /**
     * Returns the set of all shortest paths, computed using the supplied
     * edge-weight entity, between the specified source and destination devices.
     *
     * @param topology topology descriptor
     * @param src      source device
     * @param dst      destination device
     * @param weight   edge-weight entity
     * @return set of all shortest paths between the two devices
     *
     * @deprecated in Junco (1.9.0), use version with LinkWeigher instead
     */
    @Deprecated
    Set<Path> getPaths(Topology topology, DeviceId src, DeviceId dst,
                       LinkWeight weight);

    /**
     * Returns the set of all shortest paths, computed using the supplied
     * edge-weight entity, between the specified source and destination devices.
     *
     * @param topology topology descriptor
     * @param src      source device
     * @param dst      destination device
     * @param weigher  edge-weight entity
     * @return set of all shortest paths between the two devices
     */
    Set<Path> getPaths(Topology topology, DeviceId src, DeviceId dst,
                       LinkWeigher weigher);

    /**
     * Returns the k-shortest paths between source and
     * destination devices.
     *
     * The first {@code maxPaths} paths will be returned
     * in ascending order according to the provided {@code weigher}
     *
     * @param topology topology descriptor
     * @param src    source device
     * @param dst    destination device
     * @param weigher edge-weight entity
     * @param maxPaths maximum number of paths (k)
     * @return set of k-shortest paths
     */
    default Set<Path> getKShortestPaths(Topology topology,
                                       DeviceId src, DeviceId dst,
                                       LinkWeigher weigher,
                                       int maxPaths) {
        return getKShortestPaths(topology, src, dst, weigher)
                .limit(maxPaths)
                .collect(GuavaCollectors.toImmutableSet());
    }

    /**
     * Returns the k-shortest paths between source and
     * destination devices.
     *
     * @param topology topology descriptor
     * @param src    source device
     * @param dst    destination device
     * @return stream of k-shortest paths
     */
    default Stream<Path> getKShortestPaths(Topology topology,
                                        DeviceId src, DeviceId dst) {
        return getKShortestPaths(topology, src, dst, DEFAULT_HOP_COUNT_WEIGHER);
     }

    /**
     * Returns the k-shortest paths between source and
     * destination devices.
     *
     * @param topology topology descriptor
     * @param src    source device
     * @param dst    destination device
     * @param weigher edge-weight entity
     * @return stream of k-shortest paths
     */
    default Stream<Path> getKShortestPaths(Topology topology,
                                        DeviceId src, DeviceId dst,
                                        LinkWeigher weigher) {
         return getPaths(topology, src, dst, weigher).stream();
     }

    /**
     * Returns the set of all disjoint shortest path pairs, precomputed in terms of hop-count,
     * between the specified source and destination devices.
     *
     * @param topology topology descriptor
     * @param src      source device
     * @param dst      destination device
     * @return set of all shortest paths between the two devices
     */
    Set<DisjointPath> getDisjointPaths(Topology topology, DeviceId src, DeviceId dst);

    /**
     * Returns the set of all disjoint shortest path pairs, computed using the supplied
     * edge-weight entity, between the specified source and destination devices.
     *
     * @param topology topology descriptor
     * @param src      source device
     * @param dst      destination device
     * @param weight   edge-weight entity
     * @return set of all shortest paths between the two devices
     *
     * @deprecated in Junco (1.9.0), use version with LinkWeigher instead
     */
    @Deprecated
    Set<DisjointPath> getDisjointPaths(Topology topology, DeviceId src, DeviceId dst,
                                       LinkWeight weight);

    /**
     * Returns the set of all disjoint shortest path pairs, computed using the supplied
     * edge-weight entity, between the specified source and destination devices.
     *
     * @param topology topology descriptor
     * @param src      source device
     * @param dst      destination device
     * @param weigher  edge-weight entity
     * @return set of all shortest paths between the two devices
     */
    Set<DisjointPath> getDisjointPaths(Topology topology, DeviceId src, DeviceId dst,
                                       LinkWeigher weigher);

    /**
     * Returns the set of all disjoint shortest path pairs, precomputed in terms of hop-count,
     * between the specified source and destination devices.
     *
     * @param topology    topology descriptor
     * @param src         source device
     * @param dst         destination device
     * @param riskProfile map of edges to risk profiles
     * @return set of all shortest paths between the two devices
     */
    Set<DisjointPath> getDisjointPaths(Topology topology, DeviceId src, DeviceId dst,
                                       Map<Link, Object> riskProfile);

    /**
     * Returns the set of all disjoint shortest path pairs, precomputed in terms of hop-count,
     * between the specified source and destination devices.
     *
     * @param topology    topology descriptor
     * @param src         source device
     * @param dst         destination device
     * @param weight      edge-weight entity
     * @param riskProfile map of edges to risk profiles
     * @return set of all shortest paths between the two devices
     *
     * @deprecated in Junco (1.9.0), use version with LinkWeigher instead
     */
    @Deprecated
    Set<DisjointPath> getDisjointPaths(Topology topology, DeviceId src, DeviceId dst,
                                       LinkWeight weight, Map<Link, Object> riskProfile);

    /**
     * Returns the set of all disjoint shortest path pairs, precomputed in terms of hop-count,
     * between the specified source and destination devices.
     *
     * @param topology    topology descriptor
     * @param src         source device
     * @param dst         destination device
     * @param weigher     edge-weight entity
     * @param riskProfile map of edges to risk profiles
     * @return set of all shortest paths between the two devices
     */
    Set<DisjointPath> getDisjointPaths(Topology topology, DeviceId src, DeviceId dst,
                                       LinkWeigher weigher, Map<Link, Object> riskProfile);

    /**
     * Indicates whether the specified connection point is part of the network
     * infrastructure or part of network edge.
     *
     * @param topology     topology descriptor
     * @param connectPoint connection point
     * @return true of connection point is in infrastructure; false if edge
     */
    boolean isInfrastructure(Topology topology, ConnectPoint connectPoint);


    /**
     * Indicates whether broadcast is allowed for traffic received on the
     * specified connection point.
     *
     * @param topology     topology descriptor
     * @param connectPoint connection point
     * @return true if broadcast is permissible
     */
    boolean isBroadcastPoint(Topology topology, ConnectPoint connectPoint);

}
