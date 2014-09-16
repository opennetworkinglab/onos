package org.onlab.onos.net.path;

import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.HostId;
import org.onlab.onos.net.Path;
import org.onlab.onos.net.topology.LinkWeight;

import java.util.Set;

/**
 * Service for obtaining pre-computed paths or for requesting computation of
 * paths using the current topology snapshot.
 */
public interface PathService {

    /**
     * Returns the set of all shortest paths, precomputed in terms of hop-count,
     * between the specified source and destination devices.
     *
     * @param src source device
     * @param dst destination device
     * @return set of all shortest paths between the two devices
     */
    Set<Path> getPaths(DeviceId src, DeviceId dst);

    /**
     * Returns the set of all shortest paths, computed using the supplied
     * edge-weight entity, between the specified source and destination devices.
     *
     * @param src source device
     * @param dst destination device
     * @return set of all shortest paths between the two devices
     */
    Set<Path> getPaths(DeviceId src, DeviceId dst,
                       LinkWeight weight);


    /**
     * Returns the set of all shortest paths, precomputed in terms of hop-count,
     * between the specified source and destination end-stations.
     *
     * @param src source device
     * @param dst destination device
     * @return set of all shortest paths between the two end-stations hosts
     */
    Set<Path> getPaths(HostId src, HostId dst);

    /**
     * Returns the set of all shortest paths, computed using the supplied
     * edge-weight entity, between the specified source and end-stations.
     *
     * @param src source host
     * @param dst destination host
     * @return set of all shortest paths between the two end-station hosts
     */
    Set<Path> getPaths(HostId src, HostId dst, LinkWeight weight);

}
