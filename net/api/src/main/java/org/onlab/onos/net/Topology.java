package org.onlab.onos.net;

/**
 * Represents a network topology computation snapshot.
 */
public interface Topology extends Provided {

    /**
     * Returns the time, specified in milliseconds since start of epoch,
     * when the topology became active and made available.
     *
     * @return time in milliseconds since start of epoch
     */
    long time();

    /**
     * Returns the number of SCCs (strongly connected components) in the
     * topology.
     *
     * @return number of clusters
     */
    int clusterCount();

    /**
     * Returns the number of infrastructure devices in the topology.
     *
     * @return number of devices
     */
    int deviceCount();


    /**
     * Returns the number of infrastructure links in the topology.
     *
     * @return number of links
     */
    int linkCount();

    /**
     * Returns the number of infrastructure paths computed between devices
     * in the topology. This means the number of all the shortest paths
     * (hop-count) between all device pairs.
     *
     * @return number of paths
     */
    int pathCount();

}
