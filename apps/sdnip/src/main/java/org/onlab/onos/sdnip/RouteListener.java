package org.onlab.onos.sdnip;

/**
 * An interface to receive route updates from route providers.
 */
public interface RouteListener {
    /**
     * Receives a route update from a route provider.
     *
     * @param routeUpdate the updated route information
     */
    public void update(RouteUpdate routeUpdate);
}
