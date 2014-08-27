package org.onlab.onos.net.topology;

import org.onlab.onos.net.Topology;

/**
 * Service for providing network topology information.
 */
public interface TopologyService {

    /**
     * Returns the current topology descriptor.
     *
     * @return current topology
     */
    Topology currentTopology();

    // TODO: Figure out hot to best export graph traversal methods via Graph/Vertex/Edge
    // TODO: figure out how we want this to be presented, via Topology or via TopologyService
    // Set<TopologyCluster> getClusters(Topology topology);
    // Set<Path> getPaths(Topology topology, DeviceId src, DeviceId dst);
    // Set<Path> getPaths(Topology topology, DeviceId src, DeviceId dst, LinkWeight weight);
    // boolean isInfrastructure(Topology topology, ConnectPoint connectPoint);
    // boolean isInBroadcastTree(Topology topology, ConnectPoint connectPoint);

    /**
     * Adds the specified topology listener.
     *
     * @param listener topology listener
     */
    void addListener(TopologyListener listener);

    /**
     * Removes the specified topology listener.
     *
     * @param listener topology listener
     */
    void removeListener(TopologyListener listener);

}
