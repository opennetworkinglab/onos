package org.onlab.onos.cluster;

import java.util.Set;

/**
 * Service for obtaining information about the individual instances within
 * the controller cluster.
 */
public interface ClusterService {

    /**
     * Returns the set of current cluster members.
     *
     * @return set of cluster members
     */
    Set<ControllerInstance> getInstances();

    /**
     * Returns the availability state of the specified controller instance.
     *
     * @return availability state
     */
    ControllerInstance.State getState(ControllerInstance instance);

    /**
     * Adds the specified cluster event listener.
     *
     * @param listener the cluster listener
     */
    void addListener(ClusterEventListener listener);

    /**
     * Removes the specified cluster event listener.
     *
     * @param listener the cluster listener
     */
    void removeListener(ClusterEventListener listener);

}
