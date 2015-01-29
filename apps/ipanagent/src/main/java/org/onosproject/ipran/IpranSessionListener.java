package org.onosproject.ipran;


import org.onosproject.net.flowext.FlowRuleBatchExtRequest;


public interface IpranSessionListener {
    /**
     * Receives a route update from a route provider.
     *
     * @param routeUpdates the collection with updated route information
     */
    public void update(FlowRuleBatchExtRequest routeUpdates);
}
